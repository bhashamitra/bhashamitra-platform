package com.bhashamitra.platform.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.util.UUID;

/**
 * Service for uploading files to AWS S3.
 */
@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucketName;

    public S3Service(@Value("${aws.s3.bucket.name}") String bucketName,
                     @Value("${aws.s3.region}") String region) {
        this.bucketName = bucketName;
        this.s3Client = S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.of(region))
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build();
    }

    /**
     * Uploads an audio file to S3 and returns the S3 URI.
     * 
     * @param inputStream The file input stream
     * @param fileName Original file name (for extension)
     * @param contentType Content type (MIME type)
     * @param ownerType Owner type (LEMMA, SURFACE_FORM, SENTENCE) - used for path organization
     * @param ownerId Owner ID - used for path organization
     * @param fileSizeBytes File size in bytes (must be validated before calling)
     * @return S3 URI (s3://bucket-name/key or https://bucket-name.s3.region.amazonaws.com/key)
     * @throws IllegalArgumentException if upload fails
     */
    public String uploadAudioFile(InputStream inputStream,
                                  String fileName,
                                  String contentType,
                                  String ownerType,
                                  String ownerId,
                                  long fileSizeBytes) {
        try {
            // Generate a unique key for the file
            // Format: pronunciations/{ownerType}/{ownerId}/{uuid}.{ext}
            String extension = getFileExtension(fileName);
            String uuid = UUID.randomUUID().toString();
            String key = String.format("pronunciations/%s/%s/%s%s",
                    ownerType.toLowerCase(), ownerId, uuid, extension);

            // Upload to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(fileSizeBytes)
                    .build();

            RequestBody requestBody = RequestBody.fromInputStream(inputStream, fileSizeBytes);

            s3Client.putObject(putObjectRequest, requestBody);

            // Return S3 URI
            // Using s3:// URI format (alternative: https://{bucket}.s3.{region}.amazonaws.com/{key})
            return String.format("s3://%s/%s", bucketName, key);

        } catch (S3Exception e) {
            throw new IllegalArgumentException("Failed to upload file to S3: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unexpected error uploading file to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts file extension from file name.
     * 
     * @param fileName File name
     * @return Extension with dot (e.g., ".mp3") or empty string if no extension
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return ".mp3"; // Default extension
        }

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return ".mp3"; // Default extension if no extension found
        }

        return fileName.substring(lastDot);
    }

    /**
     * Gets the S3 client (for testing or advanced operations).
     * 
     * @return S3 client instance
     */
    public S3Client getS3Client() {
        return s3Client;
    }

    /**
     * Gets the bucket name.
     * 
     * @return S3 bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Downloads an audio file from S3 and returns an input stream.
     * 
     * @param s3Uri S3 URI (format: s3://bucket-name/key)
     * @return ResponseInputStream with the file content and metadata
     * @throws IllegalArgumentException if the URI is invalid or file doesn't exist
     */
    public ResponseInputStream<GetObjectResponse> downloadAudioFile(String s3Uri) {
        try {
            // Parse S3 URI: s3://bucket-name/key
            if (!s3Uri.startsWith("s3://")) {
                throw new IllegalArgumentException("Invalid S3 URI format. Expected s3://bucket/key, got: " + s3Uri);
            }
            
            String withoutPrefix = s3Uri.substring(5); // Remove "s3://"
            int firstSlash = withoutPrefix.indexOf('/');
            if (firstSlash == -1) {
                throw new IllegalArgumentException("Invalid S3 URI format. Expected s3://bucket/key, got: " + s3Uri);
            }
            
            String bucketFromUri = withoutPrefix.substring(0, firstSlash);
            String key = withoutPrefix.substring(firstSlash + 1);
            
            // Validate bucket matches configured bucket
            if (!bucketFromUri.equals(bucketName)) {
                throw new IllegalArgumentException("S3 URI bucket (" + bucketFromUri + ") does not match configured bucket (" + bucketName + ")");
            }
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            return s3Client.getObject(getObjectRequest);
            
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new IllegalArgumentException("Audio file not found in S3: " + s3Uri);
            }
            throw new IllegalArgumentException("Failed to download audio file from S3: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to download audio file from S3: " + e.getMessage(), e);
        }
    }
}

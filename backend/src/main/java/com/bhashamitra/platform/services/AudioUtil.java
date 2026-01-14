package com.bhashamitra.platform.services;

import java.io.InputStream;

/**
 * Utility for audio file operations, particularly duration detection.
 */
public class AudioUtil {

    /**
     * Maximum file size: 1 MB
     */
    public static final long MAX_FILE_SIZE_BYTES = 1 * 1024 * 1024; // 1 MB

    /**
     * Maximum duration: 10 seconds (global hard limit - applies to sentences)
     */
    public static final int MAX_DURATION_SECONDS = 10;

    /**
     * Hard max duration for Lemma: 5 seconds
     */
    public static final int MAX_LEMMA_DURATION_SECONDS = 5;
    public static final int MAX_LEMMA_DURATION_MS = MAX_LEMMA_DURATION_SECONDS * 1000;

    /**
     * Hard max duration for Surface Form: 5 seconds
     */
    public static final int MAX_SURFACE_FORM_DURATION_SECONDS = 5;
    public static final int MAX_SURFACE_FORM_DURATION_MS = MAX_SURFACE_FORM_DURATION_SECONDS * 1000;

    /**
     * Hard max duration for Sentence: 10 seconds
     */
    public static final int MAX_SENTENCE_DURATION_SECONDS = 10;
    public static final int MAX_SENTENCE_DURATION_MS = MAX_SENTENCE_DURATION_SECONDS * 1000;

    /**
     * Recommended duration for Lemma: ≤3 seconds
     */
    public static final int RECOMMENDED_LEMMA_DURATION_SECONDS = 3;

    /**
     * Recommended duration for Surface Form: ≤3 seconds
     */
    public static final int RECOMMENDED_SURFACE_FORM_DURATION_SECONDS = 3;

    /**
     * Recommended duration for Usage Sentence: ≤8 seconds
     */
    public static final int RECOMMENDED_SENTENCE_DURATION_SECONDS = 8;

    /**
     * Detects audio duration from an audio file input stream.
     * 
     * @param audioInputStream The audio file input stream
     * @param contentType The content type (MIME type) of the audio file
     * @return Duration in milliseconds, or null if detection fails
     * @throws IllegalArgumentException if audio file is invalid or duration cannot be detected
     */
    public static Integer detectDurationMs(InputStream audioInputStream, String contentType) {
        // TODO: Implement audio duration detection
        // This will require a library like:
        // - Tika (Apache Tika) - has audio metadata support
        // - JAudioTagger - for MP3 metadata
        // - Or a native audio processing library
        //
        // For now, return null to indicate duration needs to be provided by client
        // In production, this should detect duration from audio metadata
        
        throw new UnsupportedOperationException("Audio duration detection not yet implemented. Client must provide duration.");
    }

    /**
     * Validates that the file size is within limits.
     * 
     * @param fileSizeBytes File size in bytes
     * @throws IllegalArgumentException if file size exceeds limit
     */
    public static void validateFileSize(long fileSizeBytes) {
        if (fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes / 1 MB)",
                    fileSizeBytes, MAX_FILE_SIZE_BYTES)
            );
        }
    }

    /**
     * Validates that the audio duration is within limits for the given owner type.
     * 
     * @param durationMs Duration in milliseconds
     * @param ownerType Owner type (LEMMA, SURFACE_FORM, SENTENCE)
     * @throws IllegalArgumentException if duration exceeds limit
     */
    public static void validateDuration(int durationMs, String ownerType) {
        int maxDurationMs;
        int maxDurationSeconds;
        
        switch (ownerType.toUpperCase()) {
            case "LEMMA":
                maxDurationMs = MAX_LEMMA_DURATION_MS;
                maxDurationSeconds = MAX_LEMMA_DURATION_SECONDS;
                break;
            case "SURFACE_FORM":
                maxDurationMs = MAX_SURFACE_FORM_DURATION_MS;
                maxDurationSeconds = MAX_SURFACE_FORM_DURATION_SECONDS;
                break;
            case "SENTENCE", "USAGE_SENTENCE":
                maxDurationMs = MAX_SENTENCE_DURATION_MS;
                maxDurationSeconds = MAX_SENTENCE_DURATION_SECONDS;
                break;
            default:
                maxDurationMs = MAX_SENTENCE_DURATION_MS; // Default to sentence limit
                maxDurationSeconds = MAX_SENTENCE_DURATION_SECONDS;
        }
        
        if (durationMs > maxDurationMs) {
            throw new IllegalArgumentException(
                String.format("Audio duration (%.1f seconds) exceeds maximum allowed duration (%d seconds) for %s",
                    durationMs / 1000.0, maxDurationSeconds, ownerType)
            );
        }
    }

    /**
     * Gets the hard max duration for an owner type (in seconds).
     * 
     * @param ownerType Owner type (LEMMA, SURFACE_FORM, SENTENCE)
     * @return Hard max duration in seconds
     */
    public static int getMaxDurationSeconds(String ownerType) {
        return switch (ownerType.toUpperCase()) {
            case "LEMMA" -> MAX_LEMMA_DURATION_SECONDS;
            case "SURFACE_FORM" -> MAX_SURFACE_FORM_DURATION_SECONDS;
            case "SENTENCE", "USAGE_SENTENCE" -> MAX_SENTENCE_DURATION_SECONDS;
            default -> MAX_SENTENCE_DURATION_SECONDS; // Default to sentence limit
        };
    }

    /**
     * Gets the recommended duration for an owner type (in seconds).
     * 
     * @param ownerType Owner type (LEMMA, SURFACE_FORM, SENTENCE)
     * @return Recommended duration in seconds
     */
    public static int getRecommendedDurationSeconds(String ownerType) {
        return switch (ownerType.toUpperCase()) {
            case "LEMMA" -> RECOMMENDED_LEMMA_DURATION_SECONDS;
            case "SURFACE_FORM" -> RECOMMENDED_SURFACE_FORM_DURATION_SECONDS;
            case "SENTENCE", "USAGE_SENTENCE" -> RECOMMENDED_SENTENCE_DURATION_SECONDS;
            default -> RECOMMENDED_SENTENCE_DURATION_SECONDS; // Default to sentence recommendation
        };
    }

    /**
     * Validates content type is a supported audio format.
     * 
     * @param contentType MIME content type
     * @throws IllegalArgumentException if content type is not supported
     */
    public static void validateContentType(String contentType) {
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new IllegalArgumentException("Content type must be provided");
        }

        String ct = contentType.toLowerCase();
        if (!ct.startsWith("audio/")) {
            throw new IllegalArgumentException("File must be an audio file. Content type: " + contentType);
        }

        // Allowed audio formats (per requirements)
        // Preferred: audio/webm (Opus)
        // Allowed: audio/mp3, audio/wav (discouraged but allowed)
        boolean isSupported = ct.equals("audio/webm") ||  // Preferred: Opus/WebM
                             ct.equals("audio/mpeg") ||    // MP3
                             ct.equals("audio/mp3") ||
                             ct.equals("audio/wav") ||     // Allowed but discouraged
                             ct.equals("audio/wave") ||
                             ct.equals("audio/x-wav");

        if (!isSupported) {
            throw new IllegalArgumentException("Unsupported audio format: " + contentType + 
                ". Supported formats: WebM (preferred), MP3, WAV (allowed but discouraged)");
        }
    }
}

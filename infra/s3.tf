# S3 bucket for audio files storage
resource "aws_s3_bucket" "bhashamitra_audio" {
  bucket = "bhashamitra-audio-prod"

  tags = {
    Name        = "Bhashamitra Audio Files"
    Environment = "production"
    Purpose     = "audio-storage"
  }
}

# Block all public access
resource "aws_s3_bucket_public_access_block" "bhashamitra_audio" {
  bucket = aws_s3_bucket.bhashamitra_audio.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Bucket ownership controls (recommended for modern S3 buckets)
resource "aws_s3_bucket_ownership_controls" "bhashamitra_audio" {
  bucket = aws_s3_bucket.bhashamitra_audio.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

# Enable default encryption (SSE-S3)
resource "aws_s3_bucket_server_side_encryption_configuration" "bhashamitra_audio" {
  bucket = aws_s3_bucket.bhashamitra_audio.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
    bucket_key_enabled = true
  }
}

# Note: Versioning is disabled by default, no need for explicit configuration

# Lifecycle configuration to manage costs
resource "aws_s3_bucket_lifecycle_configuration" "bhashamitra_audio" {
  bucket = aws_s3_bucket.bhashamitra_audio.id

  rule {
    id     = "audio_lifecycle"
    status = "Enabled"

    # Transition to Infrequent Access after 30 days
    # Audio files need to remain instantly accessible, so no Glacier transition
    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    # Delete incomplete multipart uploads after 7 days
    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

# CORS configuration for web access and uploads
resource "aws_s3_bucket_cors_configuration" "bhashamitra_audio" {
  bucket = aws_s3_bucket.bhashamitra_audio.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "HEAD", "PUT", "POST"]
    allowed_origins = [
      "https://bhashamitra.com",
      "https://www.bhashamitra.com",
      "http://localhost:8080",
      "http://localhost:3000"
    ]
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}
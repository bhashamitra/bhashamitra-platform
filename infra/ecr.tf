# ECR Repository for Bhashamitra container images (existing, created via console)

data "aws_ecr_repository" "bhashamitra_platform" {
  name = "bhashamitra-platform"
}
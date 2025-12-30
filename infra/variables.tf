variable "aws_region" {
  description = "AWS region for resources"
  type        = string
  default     = "us-west-1"
}

variable "github_org" {
  description = "GitHub organization/username"
  type        = string
  default     = "bhashamitra"
}

variable "github_repo" {
  description = "GitHub repository name"
  type        = string
  default     = "bhashamitra-platform"
}
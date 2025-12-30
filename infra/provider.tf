terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 6.0"
    }
  }

  backend "s3" {
    bucket         = "bhashamitra-platform-terraform-state"
    key            = "bhashamitra-platform/terraform.tfstate"
    region         = "us-west-1"
    dynamodb_table = "bhashamitra-platform-terraform-state-lock"
    encrypt        = true
  }
}

provider "aws" {
  region = var.aws_region
}

# Additional provider for us-east-1 (required for Cognito custom domain certificates)
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"
}
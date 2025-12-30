# Data sources to reference existing MVL infrastructure

# Reference the existing VPC from MVL project
data "aws_vpc" "mvl_vpc" {
  filter {
    name   = "tag:Name"
    values = ["mvl-dev-vpc"]
  }
}

# Reference the existing public subnet from MVL project
data "aws_subnet" "mvl_public_subnet" {
  filter {
    name   = "tag:Name"
    values = ["mvl-dev-public-subnet"]
  }
}

# Reference the existing internet gateway
data "aws_internet_gateway" "mvl_igw" {
  filter {
    name   = "attachment.vpc-id"
    values = [data.aws_vpc.mvl_vpc.id]
  }
}

# Get availability zones for the region
data "aws_availability_zones" "available" {
  state = "available"
}

# Reference existing Route 53 hosted zone for bhashamitra.com
data "aws_route53_zone" "bhashamitra" {
  name         = "bhashamitra.com"
  private_zone = false
}
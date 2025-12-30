# Additional networking infrastructure for Bhashamitra

# Create additional public subnet in us-west-1c for ALB (ALB requires 2+ AZs)
resource "aws_subnet" "bhashamitra_public_1c" {
  vpc_id                  = data.aws_vpc.mvl_vpc.id
  cidr_block              = "10.0.3.0/24"
  availability_zone       = "us-west-1c"
  map_public_ip_on_launch = true

  tags = {
    Name        = "bhashamitra-public-subnet-1c"
    Project     = "Bhashamitra"
    Environment = "production"
    Type        = "Public"
  }
}

# Associate new public subnet with existing public route table
data "aws_route_table" "mvl_public" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.mvl_vpc.id]
  }
  
  filter {
    name   = "route.destination-cidr-block"
    values = ["0.0.0.0/0"]
  }
}

resource "aws_route_table_association" "bhashamitra_public_1c" {
  subnet_id      = aws_subnet.bhashamitra_public_1c.id
  route_table_id = data.aws_route_table.mvl_public.id
}

# Create private subnet in us-west-1a (same AZ as existing public subnet)
resource "aws_subnet" "bhashamitra_private_1a" {
  vpc_id            = data.aws_vpc.mvl_vpc.id
  cidr_block        = "10.0.1.0/24"
  availability_zone = "us-west-1a"

  tags = {
    Name        = "bhashamitra-private-subnet-1a"
    Project     = "Bhashamitra"
    Environment = "production"
    Type        = "Private"
  }
}

# Create private subnet in us-west-1c (second AZ for Aurora)
resource "aws_subnet" "bhashamitra_private_1c" {
  vpc_id            = data.aws_vpc.mvl_vpc.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = "us-west-1c"

  tags = {
    Name        = "bhashamitra-private-subnet-1c"
    Project     = "Bhashamitra"
    Environment = "production"
    Type        = "Private"
  }
}

# Create route table for private subnets (no internet access)
resource "aws_route_table" "bhashamitra_private" {
  vpc_id = data.aws_vpc.mvl_vpc.id

  # No routes - private subnets have no internet access

  tags = {
    Name        = "bhashamitra-private-route-table"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# Associate private subnet 1a with private route table
resource "aws_route_table_association" "bhashamitra_private_1a" {
  subnet_id      = aws_subnet.bhashamitra_private_1a.id
  route_table_id = aws_route_table.bhashamitra_private.id
}

# Associate private subnet 1c with private route table
resource "aws_route_table_association" "bhashamitra_private_1c" {
  subnet_id      = aws_subnet.bhashamitra_private_1c.id
  route_table_id = aws_route_table.bhashamitra_private.id
}
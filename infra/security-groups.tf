# Security Groups for Bhashamitra infrastructure

# Security group for Application Load Balancer
resource "aws_security_group" "bhashamitra_alb" {
  name        = "bhashamitra-alb-sg"
  description = "Security group for Bhashamitra Application Load Balancer"
  vpc_id      = data.aws_vpc.mvl_vpc.id

  tags = {
    Name        = "bhashamitra-alb-sg"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# Security group for ECS tasks
resource "aws_security_group" "bhashamitra_ecs" {
  name        = "bhashamitra-ecs-sg"
  description = "Security group for Bhashamitra ECS tasks"
  vpc_id      = data.aws_vpc.mvl_vpc.id

  tags = {
    Name        = "bhashamitra-ecs-sg"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# Security group for Aurora MySQL
resource "aws_security_group" "bhashamitra_aurora" {
  name        = "bhashamitra-aurora-sg"
  description = "Security group for Bhashamitra Aurora MySQL database"
  vpc_id      = data.aws_vpc.mvl_vpc.id

  tags = {
    Name        = "bhashamitra-aurora-sg"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# Security group rules (defined separately to avoid circular dependencies)

# ALB ingress rules
resource "aws_security_group_rule" "alb_https_ingress" {
  type              = "ingress"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "HTTPS from internet"
  security_group_id = aws_security_group.bhashamitra_alb.id
}

resource "aws_security_group_rule" "alb_http_ingress" {
  type              = "ingress"
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "HTTP from internet"
  security_group_id = aws_security_group.bhashamitra_alb.id
}

# ALB egress to ECS
resource "aws_security_group_rule" "alb_to_ecs_egress" {
  type                     = "egress"
  from_port                = 8080
  to_port                  = 8080
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.bhashamitra_ecs.id
  description              = "HTTP to ECS tasks on port 8080"
  security_group_id        = aws_security_group.bhashamitra_alb.id
}

# ECS ingress from ALB
resource "aws_security_group_rule" "ecs_from_alb_ingress" {
  type                     = "ingress"
  from_port                = 8080
  to_port                  = 8080
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.bhashamitra_alb.id
  description              = "HTTP from ALB on port 8080"
  security_group_id        = aws_security_group.bhashamitra_ecs.id
}

# ECS egress (for ECR pulls, API calls, etc.)
resource "aws_security_group_rule" "ecs_all_egress" {
  type              = "egress"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
  description       = "All outbound traffic"
  security_group_id = aws_security_group.bhashamitra_ecs.id
}

# Aurora ingress from ECS
resource "aws_security_group_rule" "aurora_from_ecs_ingress" {
  type                     = "ingress"
  from_port                = 3306
  to_port                  = 3306
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.bhashamitra_ecs.id
  description              = "MySQL from ECS tasks"
  security_group_id        = aws_security_group.bhashamitra_aurora.id
}
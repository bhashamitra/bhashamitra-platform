# Outputs to verify data sources are working

output "mvl_vpc_id" {
  description = "ID of the existing MVL VPC"
  value       = data.aws_vpc.mvl_vpc.id
}

output "mvl_vpc_cidr" {
  description = "CIDR block of the existing MVL VPC"
  value       = data.aws_vpc.mvl_vpc.cidr_block
}

output "mvl_public_subnet_id" {
  description = "ID of the existing MVL public subnet"
  value       = data.aws_subnet.mvl_public_subnet.id
}

output "mvl_public_subnet_az" {
  description = "Availability zone of the existing MVL public subnet"
  value       = data.aws_subnet.mvl_public_subnet.availability_zone
}

output "available_azs" {
  description = "Available availability zones in the region"
  value       = data.aws_availability_zones.available.names
}

output "bhashamitra_hosted_zone_id" {
  description = "Route 53 hosted zone ID for bhashamitra.com"
  value       = data.aws_route53_zone.bhashamitra.zone_id
}

# New networking outputs
output "bhashamitra_private_subnet_1a_id" {
  description = "ID of the Bhashamitra private subnet in us-west-1a"
  value       = aws_subnet.bhashamitra_private_1a.id
}

output "bhashamitra_private_subnet_1c_id" {
  description = "ID of the Bhashamitra private subnet in us-west-1c"
  value       = aws_subnet.bhashamitra_private_1c.id
}

output "bhashamitra_public_subnet_1c_id" {
  description = "ID of the Bhashamitra public subnet in us-west-1c"
  value       = aws_subnet.bhashamitra_public_1c.id
}

# Security group outputs
output "bhashamitra_alb_sg_id" {
  description = "ID of the ALB security group"
  value       = aws_security_group.bhashamitra_alb.id
}

output "bhashamitra_ecs_sg_id" {
  description = "ID of the ECS security group"
  value       = aws_security_group.bhashamitra_ecs.id
}

output "bhashamitra_aurora_sg_id" {
  description = "ID of the Aurora security group"
  value       = aws_security_group.bhashamitra_aurora.id
}

# Aurora database outputs
output "aurora_cluster_endpoint" {
  description = "Aurora cluster endpoint"
  value       = aws_rds_cluster.bhashamitra_aurora.endpoint
}

output "aurora_cluster_reader_endpoint" {
  description = "Aurora cluster reader endpoint"
  value       = aws_rds_cluster.bhashamitra_aurora.reader_endpoint
}

output "aurora_database_name" {
  description = "Aurora database name"
  value       = aws_rds_cluster.bhashamitra_aurora.database_name
}

output "aurora_master_user_secret_arn" {
  description = "ARN of the Aurora master user secret managed by AWS"
  value       = aws_rds_cluster.bhashamitra_aurora.master_user_secret[0].secret_arn
}

output "bhashamitra_app_secret_arn" {
  description = "ARN of the Bhashamitra application user secret for Spring Boot"
  value       = aws_secretsmanager_secret.bhashamitra_app_credentials.arn
}

# ECS outputs
output "ecs_cluster_name" {
  description = "Name of the ECS cluster"
  value       = aws_ecs_cluster.bhashamitra.name
}

output "ecs_service_name" {
  description = "Name of the ECS service"
  value       = aws_ecs_service.bhashamitra.name
}

output "ecs_task_definition_arn" {
  description = "ARN of the ECS task definition"
  value       = aws_ecs_task_definition.bhashamitra.arn
}

# ALB outputs
output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = aws_lb.bhashamitra.dns_name
}

output "alb_zone_id" {
  description = "Zone ID of the Application Load Balancer"
  value       = aws_lb.bhashamitra.zone_id
}

output "ssl_certificate_arn" {
  description = "ARN of the SSL certificate"
  value       = aws_acm_certificate_validation.bhashamitra.certificate_arn
}

# DNS outputs
output "domain_root_record" {
  description = "Root domain A record pointing to ALB"
  value       = "bhashamitra.com → ${aws_lb.bhashamitra.dns_name}"
}

output "domain_www_record" {
  description = "WWW subdomain A record pointing to ALB"
  value       = "www.bhashamitra.com → ${aws_lb.bhashamitra.dns_name}"
}
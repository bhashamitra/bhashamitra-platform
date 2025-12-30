# Aurora Serverless MySQL database for Bhashamitra

# Generate password for application user
resource "random_password" "bhashamitra_app_password" {
  length  = 16
  special = true
}

# Store application user credentials in AWS Secrets Manager
resource "aws_secretsmanager_secret" "bhashamitra_app_credentials" {
  name        = "bhashamitra/app/credentials"
  description = "Application user credentials for Bhashamitra Spring Boot app"

  tags = {
    Name        = "bhashamitra-app-credentials"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

resource "aws_secretsmanager_secret_version" "bhashamitra_app_credentials" {
  secret_id = aws_secretsmanager_secret.bhashamitra_app_credentials.id
  secret_string = jsonencode({
    username = "bhashamitra"
    password = random_password.bhashamitra_app_password.result
    engine   = "mysql"
    host     = aws_rds_cluster.bhashamitra_aurora.endpoint
    port     = 3306
    dbname   = aws_rds_cluster.bhashamitra_aurora.database_name
  })
}

# DB subnet group for Aurora (requires subnets in multiple AZs)
resource "aws_db_subnet_group" "bhashamitra_aurora" {
  name       = "bhashamitra-aurora-subnet-group"
  subnet_ids = [
    aws_subnet.bhashamitra_private_1a.id,
    aws_subnet.bhashamitra_private_1c.id
  ]

  tags = {
    Name        = "bhashamitra-aurora-subnet-group"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# Aurora Serverless v2 MySQL cluster
resource "aws_rds_cluster" "bhashamitra_aurora" {
  cluster_identifier     = "bhashamitra-aurora-cluster"
  engine                 = "aurora-mysql"
  engine_version         = null  # Use latest available version
  database_name          = "bhashamitra"
  master_username        = "bmadmin"
  
  # Use AWS Secrets Manager for password management
  manage_master_user_password = true
  master_user_secret_kms_key_id = null  # Use default KMS key
  
  # Serverless v2 configuration
  engine_mode    = "provisioned"
  serverlessv2_scaling_configuration {
    max_capacity = 2.0   # 2 ACUs max (cost control)
    min_capacity = 0.5   # 0.5 ACUs min (lowest cost)
  }

  # Network and security
  db_subnet_group_name   = aws_db_subnet_group.bhashamitra_aurora.name
  vpc_security_group_ids = [aws_security_group.bhashamitra_aurora.id]
  
  # Backup and maintenance
  backup_retention_period = 7
  preferred_backup_window = "03:00-04:00"
  preferred_maintenance_window = "sun:04:00-sun:05:00"
  
  # Security settings
  storage_encrypted = true
  skip_final_snapshot = false
  final_snapshot_identifier = "bhashamitra-aurora-final-snapshot"
  
  # Performance and monitoring
  enabled_cloudwatch_logs_exports = ["error", "general", "slowquery"]
  
  # Enable Data API for RDS Query Editor
  enable_http_endpoint = true
  
  tags = {
    Name        = "bhashamitra-aurora-cluster"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# Aurora Serverless v2 instance
resource "aws_rds_cluster_instance" "bhashamitra_aurora_instance" {
  identifier         = "bhashamitra-aurora-instance-1"
  cluster_identifier = aws_rds_cluster.bhashamitra_aurora.id
  instance_class     = "db.serverless"
  engine             = aws_rds_cluster.bhashamitra_aurora.engine
  engine_version     = aws_rds_cluster.bhashamitra_aurora.engine_version_actual

  tags = {
    Name        = "bhashamitra-aurora-instance-1"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}
# ECS Infrastructure for Bhashamitra

# ECS Cluster
resource "aws_ecs_cluster" "bhashamitra" {
  name = "bhashamitra-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name        = "bhashamitra-cluster"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# ECS Task Execution Role (for ECS to pull images, write logs, access secrets)
resource "aws_iam_role" "ecs_task_execution_role" {
  name = "bhashamitra-ecs-task-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name        = "bhashamitra-ecs-task-execution-role"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# ECS Task Role (for application to access AWS services)
resource "aws_iam_role" "ecs_task_role" {
  name = "bhashamitra-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name        = "bhashamitra-ecs-task-role"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# Attach AWS managed policy for ECS task execution
resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Custom policy for accessing Secrets Manager
resource "aws_iam_policy" "ecs_secrets_policy" {
  name        = "bhashamitra-ecs-secrets-policy"
  description = "Policy for ECS tasks to access Secrets Manager"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = [
          "${aws_secretsmanager_secret.bhashamitra_app_credentials.arn}*"
        ]
      }
    ]
  })
}

# Custom policy for accessing S3 audio bucket
resource "aws_iam_policy" "ecs_s3_audio_policy" {
  name        = "bhashamitra-ecs-s3-audio-policy"
  description = "Policy for ECS tasks to access S3 audio bucket"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
          "s3:DeleteObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.bhashamitra_audio.arn,
          "${aws_s3_bucket.bhashamitra_audio.arn}/*"
        ]
      }
    ]
  })
}

# Attach secrets policy to task execution role
resource "aws_iam_role_policy_attachment" "ecs_secrets_policy_attachment" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = aws_iam_policy.ecs_secrets_policy.arn
}

# Attach S3 policy to task role (for application access)
resource "aws_iam_role_policy_attachment" "ecs_s3_audio_policy_attachment" {
  role       = aws_iam_role.ecs_task_role.name
  policy_arn = aws_iam_policy.ecs_s3_audio_policy.arn
}

# CloudWatch Log Group for ECS tasks
resource "aws_cloudwatch_log_group" "bhashamitra_ecs" {
  name              = "/ecs/bhashamitra"
  retention_in_days = 7

  tags = {
    Name        = "bhashamitra-ecs-logs"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# ECS Task Definition
resource "aws_ecs_task_definition" "bhashamitra" {
  family                   = "bhashamitra"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"  # 0.25 vCPU
  memory                   = "512"  # 512 MB
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn           = aws_iam_role.ecs_task_role.arn

  # Use nginx as placeholder - GitHub Actions will update with real image
  container_definitions = jsonencode([
    {
      name  = "bhashamitra-app"
      image = "nginx:latest"  # Placeholder - will be updated by GitHub Actions
      
      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]

      healthCheck = {
        command = [
          "CMD-SHELL",
          "curl -f http://localhost:8080/actuator/health/liveness || exit 1"
        ]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }

      secrets = [
        {
          name      = "DB_HOST"
          valueFrom = "${aws_secretsmanager_secret.bhashamitra_app_credentials.arn}:host::"
        },
        {
          name      = "DB_PORT"
          valueFrom = "${aws_secretsmanager_secret.bhashamitra_app_credentials.arn}:port::"
        },
        {
          name      = "DB_NAME"
          valueFrom = "${aws_secretsmanager_secret.bhashamitra_app_credentials.arn}:dbname::"
        },
        {
          name      = "DB_USERNAME"
          valueFrom = "${aws_secretsmanager_secret.bhashamitra_app_credentials.arn}:username::"
        },
        {
          name      = "DB_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.bhashamitra_app_credentials.arn}:password::"
        }
      ]

      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = "production"
        },
        {
          name  = "COGNITO_CLIENT_ID"
          value = aws_cognito_user_pool_client.bhashamitra_app.id
        },
        {
          name  = "COGNITO_USER_POOL_ID"
          value = aws_cognito_user_pool.bhashamitra.id
        },
        {
          name  = "S3_AUDIO_BUCKET_NAME"
          value = aws_s3_bucket.bhashamitra_audio.bucket
        },
        {
          name  = "AWS_REGION"
          value = var.aws_region
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.bhashamitra_ecs.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }

      essential = true
    }
  ])

  tags = {
    Name        = "bhashamitra-task-definition"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# ECS Service
resource "aws_ecs_service" "bhashamitra" {
  name            = "bhashamitra-service"
  cluster         = aws_ecs_cluster.bhashamitra.id
  task_definition = aws_ecs_task_definition.bhashamitra.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [data.aws_subnet.mvl_public_subnet.id]  # Using public subnet for now
    security_groups  = [aws_security_group.bhashamitra_ecs.id]
    assign_public_ip = true  # Required for Fargate in public subnet
  }

  # Load balancer configuration
  load_balancer {
    target_group_arn = aws_lb_target_group.bhashamitra_v3.arn
    container_name   = "bhashamitra-app"
    container_port   = 8080
  }

  # Health check grace period
  health_check_grace_period_seconds = 300

  # Deployment configuration
  deployment_maximum_percent         = 200
  deployment_minimum_healthy_percent = 50

  tags = {
    Name        = "bhashamitra-service"
    Project     = "Bhashamitra"
    Environment = "production"
  }

  # Note: load_balancer block will be added in Step 6 when we create the ALB
}
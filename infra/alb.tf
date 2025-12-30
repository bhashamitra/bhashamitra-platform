# Application Load Balancer for Bhashamitra

# SSL Certificate for bhashamitra.com
resource "aws_acm_certificate" "bhashamitra" {
  domain_name               = "bhashamitra.com"
  subject_alternative_names = ["www.bhashamitra.com"]
  validation_method         = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name        = "bhashamitra-ssl-certificate"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# Certificate validation records
resource "aws_route53_record" "bhashamitra_cert_validation" {
  for_each = {
    for dvo in aws_acm_certificate.bhashamitra.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  }

  allow_overwrite = true
  name            = each.value.name
  records         = [each.value.record]
  ttl             = 60
  type            = each.value.type
  zone_id         = data.aws_route53_zone.bhashamitra.zone_id
}

# Certificate validation
resource "aws_acm_certificate_validation" "bhashamitra" {
  certificate_arn         = aws_acm_certificate.bhashamitra.arn
  validation_record_fqdns = [for record in aws_route53_record.bhashamitra_cert_validation : record.fqdn]

  timeouts {
    create = "5m"
  }
}

# Application Load Balancer
resource "aws_lb" "bhashamitra" {
  name               = "bhashamitra-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.bhashamitra_alb.id]
  subnets            = [
    data.aws_subnet.mvl_public_subnet.id,
    aws_subnet.bhashamitra_public_1c.id
  ]

  enable_deletion_protection = false

  tags = {
    Name        = "bhashamitra-alb"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# Target Group for ECS Service
resource "aws_lb_target_group" "bhashamitra_v2" {
  name        = "bhashamitra-tg-v2"
  port        = 80
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.mvl_vpc.id
  target_type = "ip"

  health_check {
    enabled             = true
    healthy_threshold   = 2
    interval            = 30
    matcher             = "200"
    path                = "/"
    port                = "traffic-port"
    protocol            = "HTTP"
    timeout             = 5
    unhealthy_threshold = 2
  }

  tags = {
    Name        = "bhashamitra-target-group-v2"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# HTTPS Listener (main traffic for bhashamitra.com)
resource "aws_lb_listener" "bhashamitra_https" {
  load_balancer_arn = aws_lb.bhashamitra.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS-1-2-2017-01"
  certificate_arn   = aws_acm_certificate_validation.bhashamitra.certificate_arn

  # Default action for bhashamitra.com - serve the app
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.bhashamitra_v2.arn
  }

  tags = {
    Name        = "bhashamitra-https-listener"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# HTTPS Listener Rule: Redirect www.bhashamitra.com to bhashamitra.com
resource "aws_lb_listener_rule" "redirect_www_to_non_www" {
  listener_arn = aws_lb_listener.bhashamitra_https.arn
  priority     = 100

  action {
    type = "redirect"
    redirect {
      host        = "bhashamitra.com"
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }

  condition {
    host_header {
      values = ["www.bhashamitra.com"]
    }
  }

  tags = {
    Name        = "redirect-www-to-non-www"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# HTTP Listener (redirect to HTTPS)
resource "aws_lb_listener" "bhashamitra_http" {
  load_balancer_arn = aws_lb.bhashamitra.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }

  tags = {
    Name        = "bhashamitra-http-listener"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}
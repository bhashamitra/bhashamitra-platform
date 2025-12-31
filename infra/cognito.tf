# Cognito User Pool for BhashaMitra Authentication

resource "aws_cognito_user_pool" "bhashamitra" {
  name = "bhashamitra-user-pool"

  # User attributes
  username_attributes = ["email"]
  
  # Email configuration
  auto_verified_attributes = ["email"]
  
  # Password policy
  password_policy {
    minimum_length    = 12
    require_lowercase = true
    require_numbers   = true
    require_symbols   = false
    require_uppercase = true
  }

  # Account recovery
  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
  }

  # Email verification
  verification_message_template {
    default_email_option = "CONFIRM_WITH_CODE"
    email_subject        = "BhashaMitra - Verify your email"
    email_message        = "Welcome to BhashaMitra! Your verification code is {####}"
  }

  # User pool add-ons
  user_pool_add_ons {
    advanced_security_mode = "ENFORCED"
  }

  # Deletion protection
  deletion_protection = "ACTIVE"

  tags = {
    Name        = "bhashamitra-user-pool"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# User Pool App Client
resource "aws_cognito_user_pool_client" "bhashamitra_app" {
  name         = "bhashamitra-app-client"
  user_pool_id = aws_cognito_user_pool.bhashamitra.id

  # Client settings
  generate_secret = false  # For web apps, no client secret needed
  
  # Token validity
  access_token_validity  = 1   # 1 hour
  id_token_validity     = 1   # 1 hour  
  refresh_token_validity = 30  # 30 days
  
  token_validity_units {
    access_token  = "hours"
    id_token      = "hours"
    refresh_token = "days"
  }

  # Prevent user existence errors
  prevent_user_existence_errors = "ENABLED"

  # Explicit auth flows
  explicit_auth_flows = ["ALLOW_REFRESH_TOKEN_AUTH"]

  # OAuth configuration for Hosted UI
  allowed_oauth_flows_user_pool_client = true
  allowed_oauth_flows = ["code"]
  allowed_oauth_scopes = ["openid", "email", "profile"]
  
  # Callback and logout URLs (Spring Security OAuth2 defaults)
  callback_urls = [
    "https://bhashamitra.com/login/oauth2/code/cognito",
    "https://www.bhashamitra.com/login/oauth2/code/cognito",
    "http://localhost:8080/login/oauth2/code/cognito"
  ]

  logout_urls = [
    "https://bhashamitra.com/",
    "https://www.bhashamitra.com/",
    "http://localhost:8080/"
  ]
  
  # Identity providers
  supported_identity_providers = ["COGNITO"]

  # Security hardening
  enable_token_revocation = true

  # Read/write attributes
  read_attributes = [
    "email",
    "email_verified"
  ]

  write_attributes = [
    "email"
  ]
}

# User Pool Groups for role-based access
resource "aws_cognito_user_group" "learner" {
  name         = "learner"
  user_pool_id = aws_cognito_user_pool.bhashamitra.id
  description  = "Default role for all users - can access learning content"
  precedence   = 3
}

resource "aws_cognito_user_group" "editor" {
  name         = "editor"
  user_pool_id = aws_cognito_user_pool.bhashamitra.id
  description  = "Content editors - can create and curate learning materials"
  precedence   = 2
}

resource "aws_cognito_user_group" "admin" {
  name         = "admin"
  user_pool_id = aws_cognito_user_pool.bhashamitra.id
  description  = "Platform administrators - can manage users and roles"
  precedence   = 1
}

# SSL Certificate for auth.bhashamitra.com (MUST be in us-east-1 for Cognito)
resource "aws_acm_certificate" "cognito_auth" {
  provider = aws.us_east_1
  
  domain_name       = "auth.bhashamitra.com"
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name        = "cognito-auth-ssl-certificate"
    Project     = "Bhashamitra"
    Environment = "production"
  }
}

# Certificate validation records
resource "aws_route53_record" "cognito_auth_cert_validation" {
  for_each = {
    for dvo in aws_acm_certificate.cognito_auth.domain_validation_options : dvo.domain_name => {
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
resource "aws_acm_certificate_validation" "cognito_auth" {
  provider = aws.us_east_1
  
  certificate_arn         = aws_acm_certificate.cognito_auth.arn
  validation_record_fqdns = [for record in aws_route53_record.cognito_auth_cert_validation : record.fqdn]

  timeouts {
    create = "5m"
  }
}

# Cognito User Pool Domain (Custom Domain)
resource "aws_cognito_user_pool_domain" "bhashamitra_auth" {
  domain          = "auth.bhashamitra.com"
  certificate_arn = aws_acm_certificate_validation.cognito_auth.certificate_arn
  user_pool_id    = aws_cognito_user_pool.bhashamitra.id
}

# Route 53 record for auth.bhashamitra.com
resource "aws_route53_record" "cognito_auth" {
  name    = aws_cognito_user_pool_domain.bhashamitra_auth.domain
  type    = "A"
  zone_id = data.aws_route53_zone.bhashamitra.zone_id

  alias {
    evaluate_target_health = false
    name                   = aws_cognito_user_pool_domain.bhashamitra_auth.cloudfront_distribution
    zone_id                = aws_cognito_user_pool_domain.bhashamitra_auth.cloudfront_distribution_zone_id
  }
}

# IPv6 support for auth.bhashamitra.com
resource "aws_route53_record" "cognito_auth_aaaa" {
  name    = aws_cognito_user_pool_domain.bhashamitra_auth.domain
  type    = "AAAA"
  zone_id = data.aws_route53_zone.bhashamitra.zone_id

  alias {
    evaluate_target_health = false
    name                   = aws_cognito_user_pool_domain.bhashamitra_auth.cloudfront_distribution
    zone_id                = aws_cognito_user_pool_domain.bhashamitra_auth.cloudfront_distribution_zone_id
  }
}

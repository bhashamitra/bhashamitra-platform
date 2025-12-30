# Route 53 DNS records for Bhashamitra

# A record for bhashamitra.com (root domain) pointing to ALB
resource "aws_route53_record" "bhashamitra_root" {
  zone_id = data.aws_route53_zone.bhashamitra.zone_id
  name    = "bhashamitra.com"
  type    = "A"
  
  allow_overwrite = true

  alias {
    name                   = aws_lb.bhashamitra.dns_name
    zone_id                = aws_lb.bhashamitra.zone_id
    evaluate_target_health = true
  }
}

# A record for www.bhashamitra.com pointing to ALB
# (ALB will redirect www to non-www)
resource "aws_route53_record" "bhashamitra_www" {
  zone_id = data.aws_route53_zone.bhashamitra.zone_id
  name    = "www.bhashamitra.com"
  type    = "A"
  
  allow_overwrite = true

  alias {
    name                   = aws_lb.bhashamitra.dns_name
    zone_id                = aws_lb.bhashamitra.zone_id
    evaluate_target_health = true
  }
}
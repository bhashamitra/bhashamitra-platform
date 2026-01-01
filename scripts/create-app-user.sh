#!/bin/bash

# Create application user in Aurora database using RDS Data API
# This script creates the 'bhashamitra' application user with proper permissions

set -euo pipefail

echo "🔧 Creating application user in Aurora database using RDS Data API..."

# Get Terraform outputs
cd "$(dirname "$0")/../infra"
AURORA_SECRET_ARN=$(terraform output -raw aurora_master_user_secret_arn)
APP_SECRET_ARN=$(terraform output -raw bhashamitra_app_secret_arn)
CLUSTER_ARN=$(terraform output -raw aurora_cluster_endpoint | sed 's/\.cluster-.*/.cluster-c68wjv40ljoq/')
CLUSTER_IDENTIFIER="bhashamitra-aurora-cluster"

echo "📋 Aurora cluster: $CLUSTER_IDENTIFIER"
echo "📋 Aurora master secret: $AURORA_SECRET_ARN"
echo "📋 Application secret: $APP_SECRET_ARN"

# Get application user credentials
echo "🔍 Retrieving application user password..."
APP_CREDS=$(aws secretsmanager get-secret-value --secret-id "$APP_SECRET_ARN" --query 'SecretString' --output text)
APP_USER="bhashamitra"  # Fixed username as per design
APP_PASS=$(echo "$APP_CREDS" | jq -r '.password')

echo "✅ Retrieved credentials for app user: $APP_USER"

# Wait for Aurora to be ready
echo "⏳ Waiting for Aurora cluster to be ready..."
sleep 10

# Test RDS Data API connection first
echo "🔍 Testing RDS Data API connection..."
aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$AURORA_SECRET_ARN" \
  --database "bhashamitra" \
  --sql "SELECT 1 as test;" > /dev/null

echo "✅ RDS Data API connection successful!"

# Create application user
echo "👤 Creating application user '$APP_USER'..."

# Check if user already exists
echo "🔍 Checking if user already exists..."
USER_EXISTS=$(aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$AURORA_SECRET_ARN" \
  --database "mysql" \
  --sql "SELECT COUNT(*) as count FROM mysql.user WHERE User = '$APP_USER';" \
  --query 'records[0][0].longValue' --output text)

if [ "$USER_EXISTS" -gt 0 ]; then
    echo "⚠️  User '$APP_USER' already exists. Updating password and permissions..."
    
    # Update existing user password
    aws rds-data execute-statement \
      --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
      --secret-arn "$AURORA_SECRET_ARN" \
      --database "mysql" \
      --sql "ALTER USER '$APP_USER'@'%' IDENTIFIED BY '$APP_PASS';" > /dev/null
else
    echo "➕ Creating new user '$APP_USER'..."
    
    # Create new user
    aws rds-data execute-statement \
      --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
      --secret-arn "$AURORA_SECRET_ARN" \
      --database "mysql" \
      --sql "CREATE USER '$APP_USER'@'%' IDENTIFIED BY '$APP_PASS';" > /dev/null
fi

# Grant privileges
echo "�  Granting privileges to user '$APP_USER'..."
aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$AURORA_SECRET_ARN" \
  --database "mysql" \
  --sql "GRANT ALL PRIVILEGES ON bhashamitra.* TO '$APP_USER'@'%';" > /dev/null

# Flush privileges
echo "🔄 Flushing privileges..."
aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$AURORA_SECRET_ARN" \
  --database "mysql" \
  --sql "FLUSH PRIVILEGES;" > /dev/null

# Verify user creation
echo "✅ Verifying user creation..."
USER_INFO=$(aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$AURORA_SECRET_ARN" \
  --database "mysql" \
  --sql "SELECT User, Host FROM mysql.user WHERE User = '$APP_USER';" \
  --query 'records[0]' --output text)

echo "📋 User info: $USER_INFO"

# Show granted privileges
echo "🔍 Checking granted privileges..."
aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$AURORA_SECRET_ARN" \
  --database "mysql" \
  --sql "SHOW GRANTS FOR '$APP_USER'@'%';" \
  --query 'records[*][0].stringValue' --output text

echo "✅ Application user '$APP_USER' created successfully!"
echo ""
echo "🚀 You can now restart the ECS service to test the connection:"
echo "   aws ecs update-service --cluster bhashamitra-cluster --service bhashamitra-service --force-new-deployment"
echo ""
echo "🔍 Or check the ECS service logs:"
echo "   aws logs tail /ecs/bhashamitra --follow"
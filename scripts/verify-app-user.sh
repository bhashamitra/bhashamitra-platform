#!/bin/bash

# Verify application user exists in Aurora database using RDS Data API
# This script checks if the 'bhashamitra' application user exists and has proper permissions

set -euo pipefail

echo "🔍 Verifying application user in Aurora database..."

# Get Terraform outputs
cd "$(dirname "$0")/../infra"
AURORA_SECRET_ARN=$(terraform output -raw aurora_master_user_secret_arn)
CLUSTER_IDENTIFIER="bhashamitra-aurora-cluster"

echo "📋 Aurora cluster: $CLUSTER_IDENTIFIER"
echo "📋 Aurora master secret: $AURORA_SECRET_ARN"

# Check if user exists
echo "👤 Checking if 'bhashamitra' user exists..."
USER_COUNT=$(aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$AURORA_SECRET_ARN" \
  --database "mysql" \
  --sql "SELECT COUNT(*) as count FROM mysql.user WHERE User = 'bhashamitra';" \
  --query 'records[0][0].longValue' --output text)

if [ "$USER_COUNT" -eq 0 ]; then
    echo "❌ User 'bhashamitra' does NOT exist!"
    exit 1
else
    echo "✅ User 'bhashamitra' exists (found $USER_COUNT user(s))"
fi

# Get user details
echo "📋 User details:"
aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$AURORA_SECRET_ARN" \
  --database "mysql" \
  --sql "SELECT User, Host, account_locked, password_expired FROM mysql.user WHERE User = 'bhashamitra';" \
  --query 'records[0]' --output table

# Check privileges
echo "🔐 Checking privileges for 'bhashamitra' user:"
GRANTS=$(aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$AURORA_SECRET_ARN" \
  --database "mysql" \
  --sql "SHOW GRANTS FOR 'bhashamitra'@'%';" \
  --query 'records[*][0].stringValue' --output text)

echo "$GRANTS"

# Test if user can connect (simulate application connection)
echo "🔌 Testing application user connection..."
APP_SECRET_ARN=$(terraform output -raw bhashamitra_app_secret_arn)
APP_CREDS=$(aws secretsmanager get-secret-value --secret-id "$APP_SECRET_ARN" --query 'SecretString' --output text)
APP_PASS=$(echo "$APP_CREDS" | jq -r '.password')

# Try to connect as the application user using RDS Data API
# Note: We need to create a temporary secret for the app user to test this
echo "🧪 Creating temporary secret for connection test..."
TEMP_SECRET_ARN=$(aws secretsmanager create-secret \
  --name "temp-bhashamitra-test-$(date +%s)" \
  --description "Temporary secret for testing bhashamitra user connection" \
  --secret-string "{\"username\":\"bhashamitra\",\"password\":\"$APP_PASS\"}" \
  --query 'ARN' --output text)

echo "🔍 Testing connection with application user credentials..."
TEST_RESULT=$(aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$TEMP_SECRET_ARN" \
  --database "bhashamitra" \
  --sql "SELECT 'Connection successful' as test, USER() as current_user, DATABASE() as current_database;" \
  --query 'records[0]' --output table 2>/dev/null || echo "❌ Connection test failed")

echo "$TEST_RESULT"

# Clean up temporary secret
echo "🧹 Cleaning up temporary secret..."
aws secretsmanager delete-secret --secret-id "$TEMP_SECRET_ARN" --force-delete-without-recovery > /dev/null

if [[ "$TEST_RESULT" == *"Connection successful"* ]]; then
    echo "✅ Application user 'bhashamitra' is properly configured and can connect!"
else
    echo "❌ Application user 'bhashamitra' cannot connect properly!"
    exit 1
fi
#!/bin/bash

# Sync Aurora master credentials to application secret
# This script updates the application secret with current Aurora master credentials

set -euo pipefail

echo "🔄 Syncing Aurora credentials to application secret..."

# Get Terraform outputs
cd "$(dirname "$0")/../infra"
AURORA_SECRET_ARN=$(terraform output -raw aurora_master_user_secret_arn)
APP_SECRET_ARN=$(terraform output -raw bhashamitra_app_secret_arn)
AURORA_ENDPOINT=$(terraform output -raw aurora_cluster_endpoint)

echo "📋 Aurora master secret: $AURORA_SECRET_ARN"
echo "📋 Application secret: $APP_SECRET_ARN"
echo "📋 Aurora endpoint: $AURORA_ENDPOINT"

# Get Aurora master credentials
echo "🔍 Retrieving Aurora master credentials..."
AURORA_CREDS=$(aws secretsmanager get-secret-value --secret-id "$AURORA_SECRET_ARN" --query 'SecretString' --output text)

# Parse Aurora credentials
DB_USERNAME=$(echo "$AURORA_CREDS" | jq -r '.username')
DB_PASSWORD=$(echo "$AURORA_CREDS" | jq -r '.password')
DB_PORT=$(echo "$AURORA_CREDS" | jq -r '.port // 3306')
DB_NAME="bhashamitra"

echo "✅ Retrieved credentials for user: $DB_USERNAME"

# Create new application secret value
NEW_APP_SECRET=$(jq -n \
  --arg host "$AURORA_ENDPOINT" \
  --arg port "$DB_PORT" \
  --arg dbname "$DB_NAME" \
  --arg username "$DB_USERNAME" \
  --arg password "$DB_PASSWORD" \
  '{
    host: $host,
    port: ($port | tonumber),
    dbname: $dbname,
    username: $username,
    password: $password
  }')

echo "🔄 Updating application secret..."
aws secretsmanager update-secret \
  --secret-id "$APP_SECRET_ARN" \
  --secret-string "$NEW_APP_SECRET"

echo "✅ Application secret updated successfully!"
echo ""
echo "🚀 You can now restart the ECS service to pick up the new credentials:"
echo "   aws ecs update-service --cluster bhashamitra-cluster --service bhashamitra-service --force-new-deployment"
echo ""
echo "🔍 Or check the ECS service logs:"
echo "   aws logs tail /ecs/bhashamitra --follow"
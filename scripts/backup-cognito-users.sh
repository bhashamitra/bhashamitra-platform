#!/bin/bash

# BhashaMitra Cognito User Backup Script
# This script exports all users from the Cognito User Pool for disaster recovery

set -euo pipefail

USER_POOL_ID="us-west-1_EAIAbURr1"
BACKUP_DIR="./cognito-backups"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/users-backup-${TIMESTAMP}.json"

echo "🔄 Starting Cognito user backup..."

# Create backup directory
mkdir -p "${BACKUP_DIR}"

# Export all users
echo "📥 Exporting users from User Pool: ${USER_POOL_ID}"
aws cognito-idp list-users \
    --user-pool-id "${USER_POOL_ID}" \
    --region us-west-1 \
    --output json > "${BACKUP_FILE}"

# Get user count
USER_COUNT=$(jq '.Users | length' "${BACKUP_FILE}")

echo "✅ Backup completed!"
echo "📊 Users backed up: ${USER_COUNT}"
echo "📁 Backup file: ${BACKUP_FILE}"

# Also backup user groups
echo "📥 Exporting user groups..."
aws cognito-idp list-groups \
    --user-pool-id "${USER_POOL_ID}" \
    --region us-west-1 \
    --output json > "${BACKUP_DIR}/groups-backup-${TIMESTAMP}.json"

# Backup users in each group
for group in learner editor admin; do
    echo "📥 Exporting users in group: ${group}"
    aws cognito-idp list-users-in-group \
        --user-pool-id "${USER_POOL_ID}" \
        --group-name "${group}" \
        --region us-west-1 \
        --output json > "${BACKUP_DIR}/group-${group}-users-${TIMESTAMP}.json" || true
done

echo "🎉 Full Cognito backup completed!"
echo "📂 All files saved in: ${BACKUP_DIR}/"
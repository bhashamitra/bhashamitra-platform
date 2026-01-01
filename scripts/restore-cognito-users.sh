#!/bin/bash

# BhashaMitra Cognito User Restore Script
# This script restores users to a new Cognito User Pool after disaster recovery

set -euo pipefail

if [ $# -ne 2 ]; then
    echo "Usage: $0 <backup-file> <user-pool-id>"
    echo "Example: $0 ./cognito-backups/users-backup-20241231-120000.json us-west-1_EAIAbURr1"
    exit 1
fi

BACKUP_FILE="$1"
USER_POOL_ID="$2"

if [ ! -f "${BACKUP_FILE}" ]; then
    echo "❌ Backup file not found: ${BACKUP_FILE}"
    exit 1
fi

echo "🔄 Starting Cognito user restore..."
echo "📁 Backup file: ${BACKUP_FILE}"
echo "🎯 Target User Pool: ${USER_POOL_ID}"

# Parse users from backup
USERS=$(jq -r '.Users[] | @base64' "${BACKUP_FILE}")

USER_COUNT=0
SUCCESS_COUNT=0
FAILED_COUNT=0

for user_data in $USERS; do
    USER_COUNT=$((USER_COUNT + 1))
    
    # Decode user data
    USER=$(echo "${user_data}" | base64 --decode)
    
    # Extract user details
    USERNAME=$(echo "${USER}" | jq -r '.Username')
    EMAIL=$(echo "${USER}" | jq -r '.Attributes[] | select(.Name=="email") | .Value')
    EMAIL_VERIFIED=$(echo "${USER}" | jq -r '.Attributes[] | select(.Name=="email_verified") | .Value // "false"')
    USER_STATUS=$(echo "${USER}" | jq -r '.UserStatus')
    
    echo "👤 Restoring user: ${EMAIL} (${USERNAME})"
    
    # For email-as-username pools, use email as username
    RESTORE_USERNAME="${EMAIL}"
    
    # Create user in pool
    if aws cognito-idp admin-create-user \
        --user-pool-id "${USER_POOL_ID}" \
        --username "${RESTORE_USERNAME}" \
        --user-attributes Name=email,Value="${EMAIL}" Name=email_verified,Value="${EMAIL_VERIFIED}" \
        --message-action SUPPRESS \
        --region us-west-1 > /dev/null 2>&1; then
        
        # Set temporary password for confirmed users
        if [ "${USER_STATUS}" = "CONFIRMED" ]; then
            echo "  🔑 Setting temporary password..."
            aws cognito-idp admin-set-user-password \
                --user-pool-id "${USER_POOL_ID}" \
                --username "${RESTORE_USERNAME}" \
                --password "TempPassword123!" \
                --temporary \
                --region us-west-1 > /dev/null 2>&1 || true
        fi
        
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
        echo "  ✅ Success"
    else
        FAILED_COUNT=$((FAILED_COUNT + 1))
        echo "  ❌ Failed"
    fi
done

echo ""
echo "🎉 User restore completed!"
echo "📊 Total users: ${USER_COUNT}"
echo "✅ Successfully restored: ${SUCCESS_COUNT}"
echo "❌ Failed: ${FAILED_COUNT}"

# Now restore group memberships
echo ""
echo "🔄 Restoring group memberships..."

# Look for group backup files in the same directory
BACKUP_DIR=$(dirname "${BACKUP_FILE}")
TIMESTAMP=$(basename "${BACKUP_FILE}" | sed 's/users-backup-\(.*\)\.json/\1/')

for group in admin editor learner; do
    GROUP_FILE="${BACKUP_DIR}/group-${group}-users-${TIMESTAMP}.json"
    if [ -f "${GROUP_FILE}" ]; then
        echo "👥 Restoring ${group} group memberships..."
        GROUP_USERS=$(jq -r '.Users[]?.Username // empty' "${GROUP_FILE}")
        for username in $GROUP_USERS; do
            # For email-as-username pools, we need to use email instead of UUID
            user_email=$(jq -r --arg uuid "$username" '.Users[] | select(.Username == $uuid) | .Attributes[] | select(.Name=="email") | .Value' "${BACKUP_FILE}")
            if [ -n "$user_email" ] && [ "$user_email" != "null" ]; then
                echo "  Adding ${user_email} to ${group} group..."
                aws cognito-idp admin-add-user-to-group \
                    --user-pool-id "${USER_POOL_ID}" \
                    --username "${user_email}" \
                    --group-name "${group}" \
                    --region us-west-1 > /dev/null 2>&1 && echo "    ✅ Added" || echo "    ❌ Failed"
            fi
        done
    fi
done

echo ""
echo "🎉 Complete restore finished!"
echo ""
echo "⚠️  IMPORTANT NOTES:"
echo "   • All users have temporary password: TempPassword123!"
echo "   • Users MUST reset their passwords on first login"
echo "   • Send password reset instructions to all users"
echo "   • Original passwords cannot be recovered"
echo ""
echo "📧 To send password reset emails to all users:"
echo "   aws cognito-idp list-users --user-pool-id ${USER_POOL_ID} --query 'Users[].Username' --output text | \\"
echo "   xargs -I {} aws cognito-idp admin-reset-user-password --user-pool-id ${USER_POOL_ID} --username {}"
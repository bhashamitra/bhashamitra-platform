#!/bin/bash

# BhashaMitra Aurora MySQL Restore Script
# This script restores an Aurora cluster from a snapshot

set -euo pipefail

if [ $# -ne 2 ]; then
    echo "Usage: $0 <snapshot-id> <new-cluster-id>"
    echo "Example: $0 disaster-recovery-20241231-120000 bhashamitra-aurora-cluster-restored"
    exit 1
fi

SNAPSHOT_ID="$1"
NEW_CLUSTER_ID="$2"
INSTANCE_ID="${NEW_CLUSTER_ID}-instance-1"

echo "🔄 Starting Aurora cluster restore..."
echo "📸 Source snapshot: ${SNAPSHOT_ID}"
echo "🎯 New cluster ID: ${NEW_CLUSTER_ID}"

# Check if snapshot exists and is available
echo "🔍 Checking snapshot status..."
SNAPSHOT_STATUS=$(aws rds describe-db-cluster-snapshots \
    --db-cluster-snapshot-identifier "${SNAPSHOT_ID}" \
    --query 'DBClusterSnapshots[0].Status' \
    --output text 2>/dev/null || echo "not-found")

if [ "${SNAPSHOT_STATUS}" = "not-found" ]; then
    echo "❌ Snapshot ${SNAPSHOT_ID} not found!"
    exit 1
elif [ "${SNAPSHOT_STATUS}" != "available" ]; then
    echo "❌ Snapshot is not available (status: ${SNAPSHOT_STATUS})"
    exit 1
fi

echo "✅ Snapshot is available"

# Check if cluster already exists
EXISTING_CLUSTER=$(aws rds describe-db-clusters \
    --db-cluster-identifier "${NEW_CLUSTER_ID}" \
    --query 'DBClusters[0].DBClusterIdentifier' \
    --output text 2>/dev/null || echo "not-found")

if [ "${EXISTING_CLUSTER}" != "not-found" ]; then
    echo "❌ Cluster ${NEW_CLUSTER_ID} already exists!"
    echo "   Please choose a different cluster name or delete the existing cluster"
    exit 1
fi

# Get original cluster configuration
echo "📋 Getting original cluster configuration..."
ORIGINAL_CLUSTER=$(aws rds describe-db-cluster-snapshots \
    --db-cluster-snapshot-identifier "${SNAPSHOT_ID}" \
    --query 'DBClusterSnapshots[0].DBClusterIdentifier' \
    --output text)

# Get subnet group - try from original cluster first, fallback to default
SUBNET_GROUP=$(aws rds describe-db-clusters \
    --db-cluster-identifier "${ORIGINAL_CLUSTER}" \
    --query 'DBClusters[0].DBSubnetGroup' \
    --output text 2>/dev/null || echo "bhashamitra-aurora-subnet-group")

# Get VPC security groups - try from original cluster first, fallback to empty
VPC_SECURITY_GROUPS=$(aws rds describe-db-clusters \
    --db-cluster-identifier "${ORIGINAL_CLUSTER}" \
    --query 'DBClusters[0].VpcSecurityGroups[0].VpcSecurityGroupId' \
    --output text 2>/dev/null || echo "")

# If we couldn't get security groups from original cluster, try to find the right one
if [ -z "${VPC_SECURITY_GROUPS}" ] || [ "${VPC_SECURITY_GROUPS}" = "None" ]; then
    echo "   Looking for BhashaMitra security group..."
    VPC_SECURITY_GROUPS=$(aws ec2 describe-security-groups \
        --filters "Name=group-name,Values=bhashamitra-aurora-sg" \
        --query 'SecurityGroups[0].GroupId' \
        --output text 2>/dev/null || echo "")
fi

echo "   Subnet Group: ${SUBNET_GROUP}"
echo "   Security Groups: ${VPC_SECURITY_GROUPS}"

# Restore cluster from snapshot
echo "🔄 Restoring cluster from snapshot..."

# Build the restore command dynamically based on available parameters
RESTORE_CMD="aws rds restore-db-cluster-from-snapshot \
    --db-cluster-identifier ${NEW_CLUSTER_ID} \
    --snapshot-identifier ${SNAPSHOT_ID} \
    --engine aurora-mysql \
    --db-subnet-group-name ${SUBNET_GROUP} \
    --region us-west-1"

# Add VPC security groups only if we have them
if [ -n "${VPC_SECURITY_GROUPS}" ] && [ "${VPC_SECURITY_GROUPS}" != "None" ]; then
    RESTORE_CMD="${RESTORE_CMD} --vpc-security-group-ids ${VPC_SECURITY_GROUPS}"
fi

# Execute the restore command
eval "${RESTORE_CMD}" > /dev/null

echo "⏳ Waiting for cluster to be available..."

# Wait for cluster to be available
while true; do
    CLUSTER_STATUS=$(aws rds describe-db-clusters \
        --db-cluster-identifier "${NEW_CLUSTER_ID}" \
        --query 'DBClusters[0].Status' \
        --output text 2>/dev/null || echo "creating")
    
    echo "   Cluster status: ${CLUSTER_STATUS}"
    
    if [ "${CLUSTER_STATUS}" = "available" ]; then
        break
    elif [ "${CLUSTER_STATUS}" = "failed" ]; then
        echo "❌ Cluster restore failed!"
        exit 1
    fi
    
    sleep 30
done

# Create cluster instance
echo "� Cereating cluster instance..."
aws rds create-db-instance \
    --db-instance-identifier "${INSTANCE_ID}" \
    --db-cluster-identifier "${NEW_CLUSTER_ID}" \
    --db-instance-class db.t3.medium \
    --engine aurora-mysql \
    --region us-west-1 > /dev/null

echo "⏳ Waiting for instance to be available..."

# Wait for instance to be available
while true; do
    INSTANCE_STATUS=$(aws rds describe-db-instances \
        --db-instance-identifier "${INSTANCE_ID}" \
        --query 'DBInstances[0].DBInstanceStatus' \
        --output text 2>/dev/null || echo "creating")
    
    echo "   Instance status: ${INSTANCE_STATUS}"
    
    if [ "${INSTANCE_STATUS}" = "available" ]; then
        break
    elif [ "${INSTANCE_STATUS}" = "failed" ]; then
        echo "❌ Instance creation failed!"
        exit 1
    fi
    
    sleep 30
done

# Get restored cluster details
CLUSTER_ENDPOINT=$(aws rds describe-db-clusters \
    --db-cluster-identifier "${NEW_CLUSTER_ID}" \
    --query 'DBClusters[0].Endpoint' \
    --output text)

DATABASE_NAME=$(aws rds describe-db-clusters \
    --db-cluster-identifier "${NEW_CLUSTER_ID}" \
    --query 'DBClusters[0].DatabaseName' \
    --output text)

echo ""
echo "🎉 Restore completed successfully!"
echo "🎯 New cluster ID: ${NEW_CLUSTER_ID}"
echo "🔗 Endpoint: ${CLUSTER_ENDPOINT}"
echo "💾 Database: ${DATABASE_NAME}"
echo "⚙️  Instance Class: db.t3.medium (instead of db.serverless due to AWS CLI limitations)"
echo "📅 Restored: $(date)"
echo ""

# Validate the restore
echo "🔍 Validating restore..."
CLUSTER_MEMBERS=$(aws rds describe-db-clusters \
    --db-cluster-identifier "${NEW_CLUSTER_ID}" \
    --query 'DBClusters[0].DBClusterMembers | length(@)' \
    --output text)

if [ "${CLUSTER_MEMBERS}" -gt 0 ]; then
    echo "✅ Cluster has ${CLUSTER_MEMBERS} instance(s)"
else
    echo "⚠️  Warning: Cluster has no instances"
fi

echo ""
echo "⚠️  IMPORTANT NOTES:"
echo "   • Update application configuration to use new endpoint"
echo "   • Verify data integrity after restore"
echo "   • Consider updating DNS or load balancer if needed"
echo "   • Original cluster is still running (not affected)"
echo ""
echo "📋 To validate database content:"
echo "   ./scripts/validate-aurora-restore.sh ${NEW_CLUSTER_ID}"
echo ""
echo "📋 To clean up this test cluster:"
echo "   aws rds delete-db-instance --db-instance-identifier ${INSTANCE_ID} --skip-final-snapshot"
echo "   aws rds delete-db-cluster --db-cluster-identifier ${NEW_CLUSTER_ID} --skip-final-snapshot"
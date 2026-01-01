#!/bin/bash

# BhashaMitra Aurora MySQL Backup Script
# This script creates a manual snapshot of the Aurora cluster for disaster recovery

set -euo pipefail

CLUSTER_ID="bhashamitra-aurora-cluster"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
SNAPSHOT_ID="disaster-recovery-${TIMESTAMP}"

echo "🔄 Starting Aurora cluster backup..."
echo "📊 Cluster: ${CLUSTER_ID}"
echo "📸 Snapshot ID: ${SNAPSHOT_ID}"

# Check cluster status first
echo "🔍 Checking cluster status..."
CLUSTER_STATUS=$(aws rds describe-db-clusters \
    --db-cluster-identifier "${CLUSTER_ID}" \
    --query 'DBClusters[0].Status' \
    --output text)

if [ "${CLUSTER_STATUS}" != "available" ]; then
    echo "❌ Cluster is not available (status: ${CLUSTER_STATUS})"
    echo "   Cannot create snapshot while cluster is not available"
    exit 1
fi

echo "✅ Cluster is available"

# Create the snapshot
echo "📸 Creating snapshot..."
aws rds create-db-cluster-snapshot \
    --db-cluster-identifier "${CLUSTER_ID}" \
    --db-cluster-snapshot-identifier "${SNAPSHOT_ID}" \
    --region us-west-1 > /dev/null

echo "⏳ Waiting for snapshot to complete..."

# Wait for snapshot to complete
while true; do
    SNAPSHOT_STATUS=$(aws rds describe-db-cluster-snapshots \
        --db-cluster-snapshot-identifier "${SNAPSHOT_ID}" \
        --query 'DBClusterSnapshots[0].Status' \
        --output text 2>/dev/null || echo "creating")
    
    PROGRESS=$(aws rds describe-db-cluster-snapshots \
        --db-cluster-snapshot-identifier "${SNAPSHOT_ID}" \
        --query 'DBClusterSnapshots[0].PercentProgress' \
        --output text 2>/dev/null || echo "0")
    
    echo "   Status: ${SNAPSHOT_STATUS}, Progress: ${PROGRESS}%"
    
    if [ "${SNAPSHOT_STATUS}" = "available" ]; then
        break
    elif [ "${SNAPSHOT_STATUS}" = "failed" ]; then
        echo "❌ Snapshot creation failed!"
        exit 1
    fi
    
    sleep 30
done

# Get snapshot details
SNAPSHOT_SIZE=$(aws rds describe-db-cluster-snapshots \
    --db-cluster-snapshot-identifier "${SNAPSHOT_ID}" \
    --query 'DBClusterSnapshots[0].AllocatedStorage' \
    --output text)

echo ""
echo "🎉 Backup completed successfully!"
echo "📸 Snapshot ID: ${SNAPSHOT_ID}"
echo "💾 Size: ${SNAPSHOT_SIZE} GB"
echo "📅 Created: $(date)"
echo ""
echo "📋 To restore from this snapshot:"
echo "   ./scripts/restore-aurora.sh ${SNAPSHOT_ID} bhashamitra-aurora-cluster-restored"
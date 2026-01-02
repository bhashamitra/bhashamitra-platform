#!/bin/bash

# Drop all objects in bhashamitra schema using RDS Data API
# This script connects as the 'bhashamitra' application user and drops all objects it owns

set -euo pipefail

echo "� Analoyzing bhashamitra schema objects..."
echo ""

# Get Terraform outputs
cd "$(dirname "$0")/../infra"
APP_SECRET_ARN=$(terraform output -raw bhashamitra_app_secret_arn)
CLUSTER_IDENTIFIER="bhashamitra-aurora-cluster"

echo "📋 Aurora cluster: $CLUSTER_IDENTIFIER"
echo "📋 Application secret: $APP_SECRET_ARN"

# Test RDS Data API connection first
echo "🔍 Testing RDS Data API connection..."
aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$APP_SECRET_ARN" \
  --database "bhashamitra" \
  --sql "SELECT 1 as test;" > /dev/null

echo "✅ RDS Data API connection successful!"
echo ""

# Get list of all objects to show what will be deleted
echo "📋 CURRENT SCHEMA OBJECTS:"
echo "=========================="

# Get list of tables
echo "🔍 Getting list of tables..."
TABLES=$(aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$APP_SECRET_ARN" \
  --database "bhashamitra" \
  --sql "SELECT table_name FROM information_schema.tables WHERE table_schema = 'bhashamitra' AND table_type = 'BASE TABLE';" \
  --query 'records[*][0].stringValue' --output text)

# Get list of views
echo "🔍 Getting list of views..."
VIEWS=$(aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$APP_SECRET_ARN" \
  --database "bhashamitra" \
  --sql "SELECT table_name FROM information_schema.tables WHERE table_schema = 'bhashamitra' AND table_type = 'VIEW';" \
  --query 'records[*][0].stringValue' --output text)

# Get list of stored procedures
echo "🔍 Getting list of stored procedures..."
PROCEDURES=$(aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$APP_SECRET_ARN" \
  --database "bhashamitra" \
  --sql "SELECT routine_name FROM information_schema.routines WHERE routine_schema = 'bhashamitra' AND routine_type = 'PROCEDURE';" \
  --query 'records[*][0].stringValue' --output text)

# Get list of functions
echo "🔍 Getting list of functions..."
FUNCTIONS=$(aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$APP_SECRET_ARN" \
  --database "bhashamitra" \
  --sql "SELECT routine_name FROM information_schema.routines WHERE routine_schema = 'bhashamitra' AND routine_type = 'FUNCTION';" \
  --query 'records[*][0].stringValue' --output text)

# Display what will be deleted
echo ""
echo "📊 OBJECTS THAT WILL BE DELETED:"
echo "================================"

TOTAL_OBJECTS=0

# Show tables
if [ -z "$TABLES" ] || [ "$TABLES" = "None" ]; then
    echo "📋 Tables: None found"
else
    echo "📋 Tables ($( echo "$TABLES" | wc -w )):"
    for table in $TABLES; do
        if [ "$table" != "None" ] && [ -n "$table" ]; then
            echo "   • $table"
            TOTAL_OBJECTS=$((TOTAL_OBJECTS + 1))
        fi
    done
fi

# Show views
if [ -z "$VIEWS" ] || [ "$VIEWS" = "None" ]; then
    echo "👁️  Views: None found"
else
    echo "👁️  Views ($( echo "$VIEWS" | wc -w )):"
    for view in $VIEWS; do
        if [ "$view" != "None" ] && [ -n "$view" ]; then
            echo "   • $view"
            TOTAL_OBJECTS=$((TOTAL_OBJECTS + 1))
        fi
    done
fi

# Show procedures
if [ -z "$PROCEDURES" ] || [ "$PROCEDURES" = "None" ]; then
    echo "⚙️  Stored Procedures: None found"
else
    echo "⚙️  Stored Procedures ($( echo "$PROCEDURES" | wc -w )):"
    for proc in $PROCEDURES; do
        if [ "$proc" != "None" ] && [ -n "$proc" ]; then
            echo "   • $proc"
            TOTAL_OBJECTS=$((TOTAL_OBJECTS + 1))
        fi
    done
fi

# Show functions
if [ -z "$FUNCTIONS" ] || [ "$FUNCTIONS" = "None" ]; then
    echo "🔧 Functions: None found"
else
    echo "🔧 Functions ($( echo "$FUNCTIONS" | wc -w )):"
    for func in $FUNCTIONS; do
        if [ "$func" != "None" ] && [ -n "$func" ]; then
            echo "   • $func"
            TOTAL_OBJECTS=$((TOTAL_OBJECTS + 1))
        fi
    done
fi

echo ""
echo "📊 TOTAL OBJECTS TO DELETE: $TOTAL_OBJECTS"
echo ""

# Check if there are any objects to delete
if [ "$TOTAL_OBJECTS" -eq 0 ]; then
    echo "ℹ️  No objects found in bhashamitra schema. Nothing to delete."
    exit 0
fi

# Confirmation prompt with detailed warning
echo "⚠️  WARNING: This will permanently delete ALL $TOTAL_OBJECTS objects listed above!"
echo "⚠️  This action cannot be undone!"
echo "⚠️  All data in these tables will be lost!"
echo ""
read -p "Are you absolutely sure you want to proceed? Type 'DELETE' to continue: " confirm
if [ "$confirm" != "DELETE" ]; then
    echo "❌ Operation cancelled"
    exit 1
fi

echo ""
echo "🗑️  Starting deletion process..."
echo ""

# Now proceed with deletion using the already-fetched object lists

if [ -n "$TABLES" ] && [ "$TABLES" != "None" ]; then
    # Disable foreign key checks
    echo "🔧 Disabling foreign key checks..."
    aws rds-data execute-statement \
      --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
      --secret-arn "$APP_SECRET_ARN" \
      --database "bhashamitra" \
      --sql "SET FOREIGN_KEY_CHECKS = 0;" > /dev/null
    
    # Drop tables in dependency order (child tables first, then parent tables)
    # This order should handle most foreign key dependencies
    ORDERED_TABLES="lemma_sentence_links meanings pronunciations surface_forms usage_sentences lemmas editorial_audit_events languages DATABASECHANGELOG DATABASECHANGELOGLOCK"
    
    echo "🔄 Dropping tables in dependency order..."
    for ordered_table in $ORDERED_TABLES; do
        # Check if this table exists in our list
        if echo "$TABLES" | grep -q "$ordered_table"; then
            echo "🗑️  Dropping table: $ordered_table"
            aws rds-data execute-statement \
              --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
              --secret-arn "$APP_SECRET_ARN" \
              --database "bhashamitra" \
              --sql "DROP TABLE IF EXISTS \`$ordered_table\`;" > /dev/null
        fi
    done
    
    # Drop any remaining tables that weren't in our ordered list
    echo "🔄 Dropping any remaining tables..."
    for table in $TABLES; do
        if [ "$table" != "None" ] && [ -n "$table" ]; then
            # Check if table still exists (might have been dropped already)
            TABLE_EXISTS=$(aws rds-data execute-statement \
              --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
              --secret-arn "$APP_SECRET_ARN" \
              --database "bhashamitra" \
              --sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'bhashamitra' AND table_name = '$table';" \
              --query 'records[0][0].longValue' --output text 2>/dev/null || echo "0")
            
            if [ "$TABLE_EXISTS" -gt 0 ]; then
                echo "🗑️  Dropping remaining table: $table"
                aws rds-data execute-statement \
                  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
                  --secret-arn "$APP_SECRET_ARN" \
                  --database "bhashamitra" \
                  --sql "DROP TABLE IF EXISTS \`$table\`;" > /dev/null 2>/dev/null || echo "   ⚠️  Could not drop $table (may have dependencies)"
            fi
        fi
    done
    
    # Re-enable foreign key checks
    echo "🔧 Re-enabling foreign key checks..."
    aws rds-data execute-statement \
      --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
      --secret-arn "$APP_SECRET_ARN" \
      --database "bhashamitra" \
      --sql "SET FOREIGN_KEY_CHECKS = 1;" > /dev/null
fi

if [ -n "$VIEWS" ] && [ "$VIEWS" != "None" ]; then
    # Drop each view
    for view in $VIEWS; do
        if [ "$view" != "None" ] && [ -n "$view" ]; then
            echo "🗑️  Dropping view: $view"
            aws rds-data execute-statement \
              --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
              --secret-arn "$APP_SECRET_ARN" \
              --database "bhashamitra" \
              --sql "DROP VIEW IF EXISTS \`$view\`;" > /dev/null
        fi
    done
fi

if [ -n "$PROCEDURES" ] && [ "$PROCEDURES" != "None" ]; then
    # Drop each procedure
    for proc in $PROCEDURES; do
        if [ "$proc" != "None" ] && [ -n "$proc" ]; then
            echo "🗑️  Dropping procedure: $proc"
            aws rds-data execute-statement \
              --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
              --secret-arn "$APP_SECRET_ARN" \
              --database "bhashamitra" \
              --sql "DROP PROCEDURE IF EXISTS \`$proc\`;" > /dev/null
        fi
    done
fi

if [ -n "$FUNCTIONS" ] && [ "$FUNCTIONS" != "None" ]; then
    # Drop each function
    for func in $FUNCTIONS; do
        if [ "$func" != "None" ] && [ -n "$func" ]; then
            echo "🗑️  Dropping function: $func"
            aws rds-data execute-statement \
              --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
              --secret-arn "$APP_SECRET_ARN" \
              --database "bhashamitra" \
              --sql "DROP FUNCTION IF EXISTS \`$func\`;" > /dev/null
        fi
    done
fi

# Verify schema is clean
echo "🔍 Verifying schema is clean..."
REMAINING_OBJECTS=$(aws rds-data execute-statement \
  --resource-arn "arn:aws:rds:us-west-1:$(aws sts get-caller-identity --query Account --output text):cluster:$CLUSTER_IDENTIFIER" \
  --secret-arn "$APP_SECRET_ARN" \
  --database "bhashamitra" \
  --sql "SELECT COUNT(*) as count FROM information_schema.tables WHERE table_schema = 'bhashamitra';" \
  --query 'records[0][0].longValue' --output text)

if [ "$REMAINING_OBJECTS" -eq 0 ]; then
    echo "✅ Schema successfully cleaned! No objects remaining."
else
    echo "⚠️  Warning: $REMAINING_OBJECTS objects still remain in schema"
fi

echo ""
echo "🎉 Schema cleanup completed!"
echo ""
echo "💡 Next steps:"
echo "   • Restart your Spring Boot application to recreate schema via Liquibase"
echo "   • Or run: mvn spring-boot:run -Dspring-boot.run.profiles=local"
echo "   • Or deploy to ECS to recreate schema in production"
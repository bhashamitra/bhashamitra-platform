#!/bin/bash

# BhashaMitra Platform - Rebuild and Run Script
# Usage: ./rebuild-and-run.sh
# This script stops any running instance, builds the app (with tests), and runs it
# If tests fail, the app will NOT start

set -e  # Exit on error

cd "$(dirname "$0")"

echo "🛑 Stopping any running Spring Boot instances..."

# Check if port 8080 is in use
if lsof -ti:8080 > /dev/null 2>&1; then
    echo "   Found process on port 8080, stopping..."
    pkill -f "spring-boot:run" 2>/dev/null || true
    # Wait for port to be free (up to 5 seconds)
    for i in {1..5}; do
        if ! lsof -ti:8080 > /dev/null 2>&1; then
            echo "   ✅ Port 8080 is now free"
            break
        fi
        sleep 1
    done
    # If still in use, try force kill
    if lsof -ti:8080 > /dev/null 2>&1; then
        echo "   ⚠️  Port still in use, force killing..."
        PID=$(lsof -ti:8080)
        kill -9 "$PID" 2>/dev/null || true
        sleep 1
    fi
else
    echo "   ✅ No process running on port 8080"
    # Still try to kill any spring-boot:run processes (might be on different port)
    pkill -f "spring-boot:run" 2>/dev/null || true
fi

echo ""
echo "🧹 Cleaning and building (with tests)..."
mvn clean package

echo ""
echo "✅ All tests passed!"
echo "🚀 Starting Spring Boot application..."
echo "   Access at: http://localhost:8080"
echo "   Press Ctrl+C to stop"
echo ""

mvn -pl backend spring-boot:run

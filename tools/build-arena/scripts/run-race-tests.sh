#!/bin/bash

# Run automated race tests with proper logging and result tracking
# Usage: ./scripts/run-race-tests.sh [test-name]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
RESULTS_DIR="$PROJECT_DIR/test-results/races"

# Create results directory
mkdir -p "$RESULTS_DIR"

# Timestamp for this test run
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="$RESULTS_DIR/race_test_$TIMESTAMP.log"

echo "🏁 Build Arena - Automated Race Tests"
echo "======================================="
echo ""
echo "Timestamp: $TIMESTAMP"
echo "Log file: $LOG_FILE"
echo ""

# Check if services are running
echo "🔍 Checking services..."
if ! curl -sf http://localhost:3001/health > /dev/null 2>&1; then
    echo "❌ Backend not running on port 3001"
    echo "   Run: pnpm dev"
    exit 1
fi

if ! curl -sf http://localhost:3000 > /dev/null 2>&1; then
    echo "❌ Frontend not running on port 3000"
    echo "   Run: pnpm dev"
    exit 1
fi

echo "✅ Backend running on port 3001"
echo "✅ Frontend running on port 3000"
echo ""

# Check Docker
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker not running"
    exit 1
fi

# Check for Docker images
echo "🐳 Checking Docker images..."
if ! docker image inspect elide-builder:latest > /dev/null 2>&1; then
    echo "❌ elide-builder:latest not found"
    echo "   Build it with: cd docker && ./build-images.sh"
    exit 1
fi

if ! docker image inspect standard-builder:latest > /dev/null 2>&1; then
    echo "❌ standard-builder:latest not found"
    echo "   Build it with: cd docker && ./build-images.sh"
    exit 1
fi

echo "✅ Docker images ready"
echo ""

# Run tests
echo "🧪 Running Playwright tests..."
echo ""

cd "$PROJECT_DIR"

# Determine which test to run
TEST_NAME="${1:-race-closed-loop.spec.ts}"

# Run with proper output handling
if npx playwright test "$TEST_NAME" --reporter=list 2>&1 | tee "$LOG_FILE"; then
    echo ""
    echo "✅ Tests passed!"
    echo ""

    # Show summary
    echo "📊 Test Summary"
    echo "==============="
    grep -E "(PASS|FAIL|✅|❌|🔔)" "$LOG_FILE" || true

    exit 0
else
    echo ""
    echo "❌ Tests failed!"
    echo ""
    echo "Check log file: $LOG_FILE"
    exit 1
fi

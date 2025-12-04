#!/bin/bash
#
# Cleanup script for Build Arena - stops all containers and resets state
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "🧹 Cleaning up Build Arena..."

# Stop and remove all Docker containers
echo "  Stopping all containers..."
docker ps -a -q | xargs -r docker stop > /dev/null 2>&1 || true
docker ps -a -q | xargs -r docker rm > /dev/null 2>&1 || true

# Remove database
echo "  Removing database..."
rm -f "$PROJECT_ROOT/backend/build-arena.db"

# Remove recordings
echo "  Removing recordings..."
rm -rf "$PROJECT_ROOT/backend/recordings"

echo "✅ Cleanup complete!"
echo ""
echo "To restart: pnpm dev"

#!/usr/bin/env bash
#
# Cleanup script for real-world integration tests
#
# This script removes all downloaded real-world projects from /tmp
#
# Usage:
#   ./scripts/cleanup-real-world-tests.sh
#

set -e

TEMP_DIR="/tmp/elide-real-world-tests"

echo "════════════════════════════════════════════════════════════════"
echo "  Elide Real-World Integration Test Cleanup"
echo "════════════════════════════════════════════════════════════════"
echo ""

if [ ! -d "$TEMP_DIR" ]; then
  echo "✓ Nothing to clean up - directory does not exist"
  echo "  ($TEMP_DIR)"
  exit 0
fi

echo "This will remove all downloaded real-world test projects:"
echo "  $TEMP_DIR"
echo ""

# Show what will be deleted
if [ -d "$TEMP_DIR" ]; then
  echo "Contents to be removed:"
  ls -1 "$TEMP_DIR" 2>/dev/null | sed 's/^/  • /' || echo "  (empty)"
  echo ""
fi

# Calculate disk usage
DISK_USAGE=$(du -sh "$TEMP_DIR" 2>/dev/null | cut -f1 || echo "unknown")
echo "Disk space to be freed: $DISK_USAGE"
echo ""

# Confirm deletion
read -p "Proceed with cleanup? (y/N): " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
  echo "Cleanup cancelled."
  exit 0
fi

echo ""
echo "Removing $TEMP_DIR..."
rm -rf "$TEMP_DIR"

echo ""
echo "════════════════════════════════════════════════════════════════"
echo "  ✓ Cleanup Complete"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "To download projects again, run:"
echo "  ./scripts/setup-real-world-tests.sh"
echo ""

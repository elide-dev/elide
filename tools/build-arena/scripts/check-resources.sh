#!/bin/bash

# Build Arena Resource Monitor
# Checks disk space and Docker resource usage

echo "🔍 Build Arena Resource Check"
echo "=============================="
echo ""

# Check disk space
echo "💾 Disk Space:"
df -h / | tail -1 | awk '{print "  Total: " $2 "\n  Used: " $3 " (" $5 ")\n  Available: " $4}'
echo ""

# Check Docker containers
CONTAINER_COUNT=$(docker ps -a | wc -l | tr -d ' ')
CONTAINER_COUNT=$((CONTAINER_COUNT - 1))  # Subtract header line
RUNNING_COUNT=$(docker ps | wc -l | tr -d ' ')
RUNNING_COUNT=$((RUNNING_COUNT - 1))

echo "🐳 Docker Containers:"
echo "  Total: $CONTAINER_COUNT"
echo "  Running: $RUNNING_COUNT"
echo ""

# Show race containers if any
RACE_CONTAINERS=$(docker ps -a --filter "name=race-" --format "{{.Names}}" | wc -l | tr -d ' ')
if [ "$RACE_CONTAINERS" -gt 0 ]; then
  echo "🏁 Active Race Containers:"
  docker ps -a --filter "name=race-" --format "  - {{.Names}} ({{.Status}})"
  echo ""
fi

# Docker disk usage
echo "📦 Docker Disk Usage:"
docker system df --format "table {{.Type}}\t{{.TotalCount}}\t{{.Size}}\t{{.Reclaimable}}" | sed 's/^/  /'
echo ""

# Warnings
AVAILABLE_GB=$(df -g / | tail -1 | awk '{print $4}')
if [ "$AVAILABLE_GB" -lt 20 ]; then
  echo "⚠️  WARNING: Less than 20GB disk space available!"
  echo "   Consider running: docker system prune -a"
  echo ""
fi

if [ "$CONTAINER_COUNT" -gt 50 ]; then
  echo "⚠️  WARNING: More than 50 Docker containers!"
  echo "   Consider running: docker container prune -f"
  echo ""
fi

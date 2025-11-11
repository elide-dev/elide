#!/bin/bash

# Database Studio Runner
# Generates and runs the Database Studio with API and UI servers

set -e

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Build command arguments
if [ -z "$1" ]; then
  echo -e "${YELLOW}No database specified - will discover databases in current directory${NC}"
  ARGS="db studio"
else
  DB_PATH="$1"
  echo -e "${BLUE}Using database: $DB_PATH${NC}"
  ARGS="db studio $DB_PATH"
fi

echo -e "${BLUE}Generating Database Studio files...${NC}"
./gradlew :packages:cli:run --args="$ARGS" -q

echo ""
echo -e "${GREEN}Database Studio files generated!${NC}"
echo ""
echo -e "${YELLOW}Starting servers...${NC}"
echo ""

# Trap to kill all background processes on exit
trap 'kill $(jobs -p) 2>/dev/null' EXIT

# Function to wait for server to be ready
wait_for_server() {
  local port=$1
  local name=$2
  local max_wait=60
  local waited=0

  while ! nc -z localhost $port 2>/dev/null; do
    if [ $waited -ge $max_wait ]; then
      echo -e "${YELLOW}Warning: $name didn't start within ${max_wait}s${NC}"
      return 1
    fi
    sleep 1
    waited=$((waited + 1))
  done
  echo -e "${GREEN}✓ $name is ready${NC}"
}

# Get absolute paths
PROJECT_ROOT="$(pwd)"
API_DIR="$PROJECT_ROOT/.dev/db-studio/api"
UI_DIR="$PROJECT_ROOT/.dev/db-studio/ui"

# Start API server in background (imperative Node.js HTTP server)
echo -e "${BLUE}[API Server]${NC} Starting on port 4984..."
./gradlew :packages:cli:run --args="run $API_DIR/index.tsx" -q 2>&1 | sed 's/^/[API] /' &
API_PID=$!

# Wait for API server to be ready
wait_for_server 4984 "API Server"

# Start UI server in background
echo -e "${BLUE}[UI Server]${NC} Starting on port 8080..."
./gradlew :packages:cli:run --args="serve .dev/db-studio/ui" -q 2>&1 | sed 's/^/[UI] /' &
UI_PID=$!

# Wait for UI server to be ready
wait_for_server 8080 "UI Server"

echo ""
echo -e "${GREEN}✓ Database Studio is running!${NC}"
echo ""
echo "  UI:  http://localhost:8080"
echo "  API: http://localhost:4984"
echo ""
echo "Press Ctrl+C to stop all servers"
echo ""

# Wait for any background job to finish
wait

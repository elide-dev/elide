#!/bin/bash
# Terminal Test Helper Script
# This script helps Claude Code and other AI agents test the terminal page

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Build Arena Terminal Test Helper ===${NC}"
echo ""

# Function to check if a process is running on a port
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Function to wait for service
wait_for_service() {
    local url=$1
    local name=$2
    local max_attempts=30
    local attempt=0

    echo -e "${YELLOW}Waiting for $name to be ready...${NC}"
    while [ $attempt -lt $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ $name is ready${NC}"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 1
    done

    echo -e "${RED}✗ $name failed to start${NC}"
    return 1
}

# Check prerequisites
echo "Checking prerequisites..."

if ! command -v docker &> /dev/null; then
    echo -e "${RED}✗ Docker is not installed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker is installed${NC}"

if ! command -v pnpm &> /dev/null; then
    echo -e "${RED}✗ pnpm is not installed${NC}"
    exit 1
fi
echo -e "${GREEN}✓ pnpm is installed${NC}"

# Check if Docker image exists
echo ""
echo "Checking Docker images..."
if docker images | grep -q "elide-builder"; then
    echo -e "${GREEN}✓ elide-builder image found${NC}"
else
    echo -e "${YELLOW}⚠ elide-builder image not found${NC}"
    echo "Building Docker images..."
    cd docker && ./build-images.sh && cd ..
fi

# Check if services are running
echo ""
echo "Checking services..."

BACKEND_RUNNING=false
FRONTEND_RUNNING=false

if check_port 3001; then
    echo -e "${GREEN}✓ Backend is running on port 3001${NC}"
    BACKEND_RUNNING=true
else
    echo -e "${YELLOW}⚠ Backend is not running${NC}"
fi

if check_port 3000; then
    echo -e "${GREEN}✓ Frontend is running on port 3000${NC}"
    FRONTEND_RUNNING=true
else
    echo -e "${YELLOW}⚠ Frontend is not running${NC}"
fi

# Start services if needed
if [ "$BACKEND_RUNNING" = false ] || [ "$FRONTEND_RUNNING" = false ]; then
    echo ""
    echo -e "${YELLOW}Starting services...${NC}"
    pnpm dev &
    DEV_PID=$!

    # Wait for services
    wait_for_service "http://localhost:3001/health" "Backend" || exit 1
    wait_for_service "http://localhost:3000" "Frontend" || exit 1
fi

# Run tests based on argument
echo ""
echo "Running tests..."

case "${1:-headless}" in
    headed)
        echo -e "${YELLOW}Running tests in headed mode (browsers will be visible)${NC}"
        pnpm test:headed tests/terminal-test.spec.ts
        ;;
    headless)
        echo -e "${YELLOW}Running tests in headless mode${NC}"
        pnpm test tests/terminal-test.spec.ts
        ;;
    interactive)
        echo -e "${YELLOW}Opening terminal test page in browser${NC}"
        echo "Visit: http://localhost:3000/test/terminal"
        if command -v open &> /dev/null; then
            open http://localhost:3000/test/terminal
        elif command -v xdg-open &> /dev/null; then
            xdg-open http://localhost:3000/test/terminal
        fi
        echo "Press Ctrl+C to exit"
        sleep infinity
        ;;
    debug)
        echo -e "${YELLOW}Running tests in debug mode${NC}"
        pnpm exec playwright test tests/terminal-test.spec.ts --debug
        ;;
    *)
        echo -e "${RED}Unknown mode: $1${NC}"
        echo "Usage: $0 [headless|headed|interactive|debug]"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}=== Tests Complete ===${NC}"

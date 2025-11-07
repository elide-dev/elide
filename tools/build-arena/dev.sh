#!/bin/bash
# Development helper script for Build Arena

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

function print_header() {
    echo -e "${GREEN}===${NC} $1"
}

function print_error() {
    echo -e "${RED}ERROR:${NC} $1"
}

function print_warning() {
    echo -e "${YELLOW}WARNING:${NC} $1"
}

function check_prerequisites() {
    print_header "Checking prerequisites..."

    if ! command -v node &> /dev/null; then
        print_error "Node.js is not installed"
        exit 1
    fi

    if ! command -v npm &> /dev/null; then
        print_error "npm is not installed"
        exit 1
    fi

    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        exit 1
    fi

    if ! docker ps &> /dev/null; then
        print_error "Docker daemon is not running"
        exit 1
    fi

    echo "✓ All prerequisites met"
}

function build_docker_images() {
    print_header "Building Docker images..."

    cd "$SCRIPT_DIR/docker"

    # Check if images exist
    if docker images | grep -q "elide-builder" && docker images | grep -q "standard-builder"; then
        read -p "Docker images already exist. Rebuild? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "Skipping Docker image build"
            return
        fi
    fi

    ./build-images.sh
    cd "$SCRIPT_DIR"
}

function setup_backend() {
    print_header "Setting up backend..."

    cd "$SCRIPT_DIR/backend"

    if [ ! -d "node_modules" ]; then
        echo "Installing backend dependencies..."
        npm install
    else
        echo "✓ Backend dependencies already installed"
    fi

    if [ ! -f ".env" ]; then
        echo "Creating backend .env file..."
        cp "$SCRIPT_DIR/.env.example" .env
    else
        echo "✓ Backend .env file exists"
    fi

    cd "$SCRIPT_DIR"
}

function setup_frontend() {
    print_header "Setting up frontend..."

    cd "$SCRIPT_DIR/frontend"

    if [ ! -d "node_modules" ]; then
        echo "Installing frontend dependencies..."
        npm install
    else
        echo "✓ Frontend dependencies already installed"
    fi

    if [ ! -f ".env.local" ]; then
        echo "Creating frontend .env.local file..."
        cat > .env.local << EOF
NEXT_PUBLIC_API_URL=http://localhost:3001
NEXT_PUBLIC_WS_URL=ws://localhost:3001/ws
EOF
    else
        echo "✓ Frontend .env.local file exists"
    fi

    cd "$SCRIPT_DIR"
}

function start_backend() {
    print_header "Starting backend..."
    cd "$SCRIPT_DIR/backend"
    npm run dev
}

function start_frontend() {
    print_header "Starting frontend..."
    cd "$SCRIPT_DIR/frontend"
    npm run dev
}

function start_all() {
    print_header "Starting all services..."

    # Start backend in background
    cd "$SCRIPT_DIR/backend"
    npm run dev > backend.log 2>&1 &
    BACKEND_PID=$!
    echo "Backend started (PID: $BACKEND_PID)"

    # Wait for backend to be ready
    echo "Waiting for backend to start..."
    sleep 3

    # Start frontend in background
    cd "$SCRIPT_DIR/frontend"
    npm run dev > frontend.log 2>&1 &
    FRONTEND_PID=$!
    echo "Frontend started (PID: $FRONTEND_PID)"

    echo ""
    echo -e "${GREEN}Build Arena is running!${NC}"
    echo -e "Frontend: ${GREEN}http://localhost:3000${NC}"
    echo -e "Backend:  ${GREEN}http://localhost:3001${NC}"
    echo ""
    echo "Press Ctrl+C to stop all services"

    # Wait for Ctrl+C
    trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit" INT
    wait
}

function show_help() {
    cat << EOF
Build Arena Development Helper

Usage: ./dev.sh [command]

Commands:
    setup       - Check prerequisites and set up all components
    build       - Build Docker images
    backend     - Start backend only
    frontend    - Start frontend only
    start       - Start all services (backend + frontend)
    help        - Show this help message

Examples:
    ./dev.sh setup      # First-time setup
    ./dev.sh start      # Start everything
    ./dev.sh backend    # Start just the backend
EOF
}

# Main script logic
case "${1:-setup}" in
    setup)
        check_prerequisites
        build_docker_images
        setup_backend
        setup_frontend
        echo ""
        echo -e "${GREEN}Setup complete!${NC}"
        echo "Run './dev.sh start' to start all services"
        ;;
    build)
        check_prerequisites
        build_docker_images
        ;;
    backend)
        check_prerequisites
        setup_backend
        start_backend
        ;;
    frontend)
        check_prerequisites
        setup_frontend
        start_frontend
        ;;
    start)
        check_prerequisites
        start_all
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        print_error "Unknown command: $1"
        echo ""
        show_help
        exit 1
        ;;
esac

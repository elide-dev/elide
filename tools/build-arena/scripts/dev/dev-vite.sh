#!/bin/bash
# Development helper script for Build Arena (Vite version)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

function print_info() {
    echo -e "${BLUE}INFO:${NC} $1"
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
            cd "$SCRIPT_DIR"
            return
        fi
    fi

    chmod +x build-images.sh
    ./build-images.sh
    cd "$SCRIPT_DIR"
}

function setup_backend() {
    print_header "Setting up backend (glue server)..."

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
        print_warning "⚠️  IMPORTANT: Set ANTHROPIC_API_KEY in backend/.env"
    else
        echo "✓ Backend .env file exists"
    fi

    cd "$SCRIPT_DIR"
}

function setup_frontend() {
    print_header "Setting up frontend (Vite)..."

    cd "$SCRIPT_DIR/frontend-vite"

    if [ ! -d "node_modules" ]; then
        echo "Installing frontend dependencies..."
        npm install
    else
        echo "✓ Frontend dependencies already installed"
    fi

    cd "$SCRIPT_DIR"
}

function start_backend() {
    print_header "Starting backend (glue server)..."
    cd "$SCRIPT_DIR/backend"

    # Check for API key
    if [ -f ".env" ]; then
        if ! grep -q "ANTHROPIC_API_KEY=sk-ant-" .env; then
            print_warning "⚠️  ANTHROPIC_API_KEY not set or invalid in backend/.env"
            print_info "Edit backend/.env and add your Claude API key"
        fi
    fi

    npm run dev
}

function start_frontend() {
    print_header "Starting frontend (Vite)..."
    cd "$SCRIPT_DIR/frontend-vite"
    npm run dev
}

function start_all() {
    print_header "Starting all services..."

    # Start backend in background
    cd "$SCRIPT_DIR/backend"
    npm run dev > "$SCRIPT_DIR/backend.log" 2>&1 &
    BACKEND_PID=$!
    echo "Backend started (PID: $BACKEND_PID)"

    # Wait for backend to be ready
    echo "Waiting for backend to start..."
    sleep 3

    # Start frontend in background
    cd "$SCRIPT_DIR/frontend-vite"
    npm run dev > "$SCRIPT_DIR/frontend.log" 2>&1 &
    FRONTEND_PID=$!
    echo "Frontend started (PID: $FRONTEND_PID)"

    # Wait for frontend to be ready
    sleep 2

    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║      Build Arena is Running! 🏁       ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${BLUE}Frontend:${NC} ${GREEN}http://localhost:5173${NC}"
    echo -e "${BLUE}Backend:${NC}  ${GREEN}http://localhost:3001${NC}"
    echo ""
    echo -e "${YELLOW}Logs:${NC}"
    echo "  Backend:  tail -f $SCRIPT_DIR/backend.log"
    echo "  Frontend: tail -f $SCRIPT_DIR/frontend.log"
    echo ""
    echo "Press Ctrl+C to stop all services"

    # Wait for Ctrl+C
    trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; echo ''; echo 'Services stopped'; exit" INT
    wait
}

function show_help() {
    cat << EOF
${GREEN}Build Arena Development Helper (Vite Edition)${NC}

Usage: ./dev-vite.sh [command]

${YELLOW}Commands:${NC}
    ${BLUE}setup${NC}       - Check prerequisites and set up all components
    ${BLUE}build${NC}       - Build Docker images for build containers
    ${BLUE}backend${NC}     - Start backend only (glue server)
    ${BLUE}frontend${NC}    - Start frontend only (Vite dev server)
    ${BLUE}start${NC}       - Start all services (backend + frontend)
    ${BLUE}help${NC}        - Show this help message

${YELLOW}Examples:${NC}
    ./dev-vite.sh setup      # First-time setup
    ./dev-vite.sh start      # Start everything
    ./dev-vite.sh backend    # Start just the backend

${YELLOW}Architecture:${NC}
    Frontend:  Vite dev server (http://localhost:5173)
    Backend:   Glue server always hot (http://localhost:3001)
    Containers: Spawned on-demand when jobs submitted

${YELLOW}Documentation:${NC}
    README.md          - Quick start guide
    ARCHITECTURE.md    - Hot-standby architecture
    SETUP.md           - Detailed setup instructions
    INTERACTIVE_AGENTS.md - How Claude Code agents work
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
        echo "Run './dev-vite.sh start' to start all services"
        echo ""
        print_warning "Don't forget to set ANTHROPIC_API_KEY in backend/.env!"
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

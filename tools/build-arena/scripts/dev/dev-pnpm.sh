#!/bin/bash
# Development helper script for Build Arena (pnpm + Vite + Biome + Playwright)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
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

function print_success() {
    echo -e "${MAGENTA}✓${NC} $1"
}

function check_prerequisites() {
    print_header "Checking prerequisites..."

    if ! command -v node &> /dev/null; then
        print_error "Node.js is not installed"
        exit 1
    fi

    if ! command -v pnpm &> /dev/null; then
        print_error "pnpm is not installed"
        echo "Install with: npm install -g pnpm"
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

    print_success "All prerequisites met"
}

function build_docker_images() {
    print_header "Building Docker images..."

    cd "$SCRIPT_DIR/docker"

    # Check if images exist
    if docker images | grep -q "elide-builder" && docker images | grep -q "standard-builder"; then
        read -p "Docker images already exist. Rebuild? (y/N) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_info "Skipping Docker image build"
            cd "$SCRIPT_DIR"
            return
        fi
    fi

    chmod +x build-images.sh
    ./build-images.sh
    cd "$SCRIPT_DIR"
}

function setup() {
    print_header "Setting up Build Arena workspace..."

    cd "$SCRIPT_DIR"

    if [ ! -d "node_modules" ]; then
        print_info "Installing dependencies with pnpm..."
        pnpm install
    else
        print_success "Dependencies already installed"
    fi

    if [ ! -f "backend/.env" ]; then
        print_info "Creating backend .env file..."
        cp .env.example backend/.env
        print_warning "⚠️  IMPORTANT: Set ANTHROPIC_API_KEY in backend/.env"
    else
        print_success "Backend .env file exists"
    fi

    # Install Playwright browsers
    if [ ! -d "$HOME/.cache/ms-playwright" ]; then
        print_info "Installing Playwright browsers..."
        pnpm exec playwright install
    else
        print_success "Playwright browsers already installed"
    fi
}

function start_dev() {
    print_header "Starting development servers..."

    cd "$SCRIPT_DIR"

    # Check for API key
    if [ -f "backend/.env" ]; then
        if ! grep -q "ANTHROPIC_API_KEY=sk-ant-" backend/.env; then
            print_warning "⚠️  ANTHROPIC_API_KEY not set in backend/.env"
        fi
    fi

    print_info "Starting backend and frontend in parallel..."
    pnpm dev
}

function start_backend() {
    print_header "Starting backend only..."
    cd "$SCRIPT_DIR"
    pnpm dev:backend
}

function start_frontend() {
    print_header "Starting frontend only..."
    cd "$SCRIPT_DIR"
    pnpm dev:frontend
}

function run_lint() {
    print_header "Running Biome lint..."
    cd "$SCRIPT_DIR"
    pnpm lint
}

function run_format() {
    print_header "Running Biome format..."
    cd "$SCRIPT_DIR"
    pnpm format
}

function run_tests() {
    print_header "Running Playwright tests..."
    cd "$SCRIPT_DIR"

    if [ "$1" = "headed" ]; then
        print_info "Running tests in headed mode..."
        pnpm test:headed
    elif [ "$1" = "ui" ]; then
        print_info "Opening Playwright UI..."
        pnpm test:ui
    else
        print_info "Running tests in headless mode..."
        pnpm test
    fi
}

function show_help() {
    cat << EOF
${GREEN}╔════════════════════════════════════════════════════════╗${NC}
${GREEN}║          Build Arena Development Tools                ║${NC}
${GREEN}╚════════════════════════════════════════════════════════╝${NC}

${YELLOW}Usage:${NC} ./dev-pnpm.sh [command]

${YELLOW}Commands:${NC}
    ${BLUE}setup${NC}          - Install dependencies, setup environment
    ${BLUE}dev${NC}            - Start both backend + frontend (parallel)
    ${BLUE}backend${NC}        - Start backend only (glue server)
    ${BLUE}frontend${NC}       - Start frontend only (Vite)
    ${BLUE}build${NC}          - Build Docker images for containers
    ${BLUE}lint${NC}           - Run Biome linter
    ${BLUE}format${NC}         - Format code with Biome
    ${BLUE}test${NC}           - Run Playwright tests (headless)
    ${BLUE}test:headed${NC}    - Run Playwright tests (headed)
    ${BLUE}test:ui${NC}        - Open Playwright UI
    ${BLUE}help${NC}           - Show this help message

${YELLOW}Examples:${NC}
    ./dev-pnpm.sh setup          # First-time setup
    ./dev-pnpm.sh dev            # Start development
    ./dev-pnpm.sh test           # Run tests
    ./dev-pnpm.sh format         # Format code

${YELLOW}Workspace Structure:${NC}
    ${MAGENTA}Frontend:${NC}  Vite dev server (http://localhost:5173)
    ${MAGENTA}Backend:${NC}   Glue server (http://localhost:3001)
    ${MAGENTA}Containers:${NC} Spawned on-demand

${YELLOW}Documentation:${NC}
    README.md                    - Quick start guide
    docs/ARCHITECTURE.md         - Hot-standby architecture
    docs/INTERACTIVE_AGENTS.md   - Claude Code agents
    tests/                       - Playwright test suite

${YELLOW}Quality Tools:${NC}
    ${GREEN}✓${NC} Biome      - Lightning-fast linting & formatting
    ${GREEN}✓${NC} Playwright - End-to-end testing
    ${GREEN}✓${NC} TypeScript - Type safety
    ${GREEN}✓${NC} pnpm       - Fast, efficient package management
EOF
}

# Main script logic
case "${1:-help}" in
    setup)
        check_prerequisites
        build_docker_images
        setup
        echo ""
        print_success "Setup complete!"
        echo ""
        print_info "Next steps:"
        echo "  1. Add ANTHROPIC_API_KEY to backend/.env"
        echo "  2. Run: ./dev-pnpm.sh dev"
        ;;
    dev)
        check_prerequisites
        start_dev
        ;;
    backend)
        check_prerequisites
        start_backend
        ;;
    frontend)
        check_prerequisites
        start_frontend
        ;;
    build)
        check_prerequisites
        build_docker_images
        ;;
    lint)
        run_lint
        ;;
    format)
        run_format
        ;;
    test)
        run_tests
        ;;
    test:headed)
        run_tests headed
        ;;
    test:ui)
        run_tests ui
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

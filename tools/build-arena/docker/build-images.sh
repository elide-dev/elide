#!/bin/bash
# Build Docker images for Build Arena
# Supports multi-platform builds (linux/amd64, linux/arm64)

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}Building Docker images for Build Arena${NC}"
echo ""

# Check if buildx is available
if ! docker buildx version > /dev/null 2>&1; then
    echo -e "${YELLOW}Warning: docker buildx not found. Using standard docker build.${NC}"
    echo "For multi-platform builds, install Docker Desktop or enable buildx."
    echo ""

    # Standard single-platform build
    echo "Building elide-builder image (single platform)..."
    docker build -t elide-builder:latest -f elide-builder.Dockerfile .

    echo "Building standard-builder image (single platform)..."
    docker build -t standard-builder:latest -f standard-builder.Dockerfile .
else
    # Detect native platform
    if [[ $(uname -m) == "arm64" ]]; then
        PLATFORM="linux/arm64"
        echo -e "${YELLOW}Detected ARM64 Mac - building for linux/arm64 (native)${NC}"
        echo "Building for native platform for faster builds."
        echo ""
    else
        PLATFORM="linux/amd64"
        echo "Building for linux/amd64"
        echo ""
    fi

    # Create builder if it doesn't exist
    if ! docker buildx ls | grep -q "build-arena-builder"; then
        echo "Creating buildx builder..."
        docker buildx create --name build-arena-builder --use
    else
        docker buildx use build-arena-builder
    fi

    # Build for linux/amd64 (most common production platform)
    echo "Building elide-builder image for ${PLATFORM}..."
    docker buildx build \
        --platform ${PLATFORM} \
        --tag elide-builder:latest \
        --file elide-builder.Dockerfile \
        --load \
        .

    echo "Building standard-builder image for ${PLATFORM}..."
    docker buildx build \
        --platform ${PLATFORM} \
        --tag standard-builder:latest \
        --file standard-builder.Dockerfile \
        --load \
        .
fi

echo ""
echo -e "${GREEN}All images built successfully!${NC}"
docker images | grep -E "(elide-builder|standard-builder)"

echo ""
echo -e "${GREEN}Next steps:${NC}"
echo "  1. Add ANTHROPIC_API_KEY to backend/.env"
echo "  2. Start dev servers: pnpm dev"
echo "  3. Submit a test repository at http://localhost:3000"

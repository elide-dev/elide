#!/bin/bash
# Build Docker images for Build Arena
# Supports multi-platform builds (linux/amd64, linux/arm64)

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${GREEN}Building Docker images for Build Arena${NC}"
echo ""

# Step 1: Build Elide from source
echo -e "${BLUE}Step 1: Building Elide from source...${NC}"
cd ../../..  # Navigate to repo root from tools/build-arena/docker
echo "Running: ./gradlew :packages:cli:installDist"
./gradlew :packages:cli:installDist

# Step 2: Copy Elide binary to docker build context
echo -e "${BLUE}Step 2: Copying Elide binary to docker build context...${NC}"
ELIDE_BIN="packages/cli/build/install/cli/bin/elide"
if [ ! -f "$ELIDE_BIN" ]; then
    echo "Error: Elide binary not found at $ELIDE_BIN"
    echo "Build may have failed. Check output above."
    exit 1
fi

cp "$ELIDE_BIN" tools/build-arena/docker/elide
echo "✓ Copied Elide binary to tools/build-arena/docker/elide"

# Return to docker directory
cd tools/build-arena/docker
echo ""

# Check if buildx is available
if ! docker buildx version > /dev/null 2>&1; then
    echo -e "${YELLOW}Warning: docker buildx not found. Using standard docker build.${NC}"
    echo "For multi-platform builds, install Docker Desktop or enable buildx."
    echo ""

    # Standard single-platform build
    echo -e "${BLUE}Step 3: Building elide-builder image (single platform)...${NC}"
    docker build -t elide-builder:latest -f elide-builder.Dockerfile .

    echo -e "${BLUE}Step 4: Building standard-builder image (single platform)...${NC}"
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

    # Build for native platform
    echo -e "${BLUE}Step 3: Building elide-builder image for ${PLATFORM}...${NC}"
    docker buildx build \
        --platform ${PLATFORM} \
        --tag elide-builder:latest \
        --file elide-builder.Dockerfile \
        --load \
        .

    echo -e "${BLUE}Step 4: Building standard-builder image for ${PLATFORM}...${NC}"
    docker buildx build \
        --platform ${PLATFORM} \
        --tag standard-builder:latest \
        --file standard-builder.Dockerfile \
        --load \
        .
fi

# Cleanup: Remove copied Elide binary
echo ""
echo -e "${BLUE}Step 5: Cleaning up...${NC}"
rm -f elide
echo "✓ Removed temporary Elide binary"

echo ""
echo -e "${GREEN}All images built successfully!${NC}"
docker images | grep -E "(elide-builder|standard-builder)"

echo ""
echo -e "${GREEN}Next steps:${NC}"
echo "  1. Add ANTHROPIC_API_KEY to backend/.env"
echo "  2. Start dev servers: pnpm dev"
echo "  3. Submit a test repository at http://localhost:3000"

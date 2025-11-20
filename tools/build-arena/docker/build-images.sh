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

# Step 1: Build Elide from source on remote Linux machine
echo -e "${BLUE}Step 1: Building Elide from source on remote Linux machine...${NC}"
cd ../../..  # Navigate to repo root from tools/build-arena/docker

# Get current branch name
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
echo "Current branch: ${CURRENT_BRANCH}"

# Get remote repository URL
REPO_URL=$(git config --get remote.origin.url)
echo "Repository: ${REPO_URL}"

# Remote build configuration
REMOTE_USER="rwalters"
REMOTE_HOST="192.168.1.135"
REMOTE_DIR="/tmp/elide-build-$(date +%s)"

echo "Cloning ${CURRENT_BRANCH} on remote machine..."
# Clone the repository and checkout the current branch on remote machine
ssh ${REMOTE_USER}@${REMOTE_HOST} "git clone -b ${CURRENT_BRANCH} ${REPO_URL} ${REMOTE_DIR}"

echo "Building Elide on remote Linux machine..."
# Build on remote machine
ssh ${REMOTE_USER}@${REMOTE_HOST} "cd ${REMOTE_DIR} && ./gradlew :packages:cli:installDist --no-daemon"

echo "Copying build artifacts back..."
# Copy build artifacts back
rsync -az ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_DIR}/packages/cli/build/install/cli/ \
  ./packages/cli/build/install/cli/

# Cleanup remote directory
ssh ${REMOTE_USER}@${REMOTE_HOST} "rm -rf ${REMOTE_DIR}"
echo "Remote build complete!"
cd tools/build-arena/docker

# Step 2: Copy Elide distribution to docker build context
echo -e "${BLUE}Step 2: Copying Elide distribution to docker build context...${NC}"
ELIDE_DIST="packages/cli/build/install/cli"
if [ ! -d "$ELIDE_DIST" ]; then
    echo "Error: Elide distribution not found at $ELIDE_DIST"
    echo "Build may have failed. Check output above."
    exit 1
fi

# Copy entire distribution (bin/ and lib/)
cp -r "$ELIDE_DIST" tools/build-arena/docker/elide-dist
echo "✓ Copied Elide distribution to tools/build-arena/docker/elide-dist"

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
    # Build for linux/amd64 to match remote Linux build
    PLATFORM="linux/amd64"
    echo -e "${YELLOW}Building for linux/amd64 (to match remote Linux Elide build)${NC}"
    echo ""

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

# Cleanup: Remove copied Elide distribution
echo ""
echo -e "${BLUE}Step 5: Cleaning up...${NC}"
rm -rf elide-dist
echo "✓ Removed temporary Elide distribution"

echo ""
echo -e "${GREEN}All images built successfully!${NC}"
docker images | grep -E "(elide-builder|standard-builder)"

echo ""
echo -e "${GREEN}Next steps:${NC}"
echo "  1. Add ANTHROPIC_API_KEY to backend/.env"
echo "  2. Start dev servers: pnpm dev"
echo "  3. Submit a test repository at http://localhost:3000"

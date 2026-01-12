#!/bin/bash
# Test Claude Code non-interactive mode in Docker container
# This simulates how the backend actually invokes Claude Code

set -e

echo "======================================"
echo "Claude Code Non-Interactive Test"
echo "======================================"
echo ""

# Load API key
cd "$(dirname "$0")/backend"
if [ -f .env ]; then
    source .env
    echo "✓ Loaded API key from backend/.env"
else
    echo "✗ No .env file found in backend/"
    exit 1
fi

echo "API Key (first 20 chars): ${ANTHROPIC_API_KEY:0:20}..."
echo ""

# Create a simple test prompt
TEST_PROMPT="List the files in the current directory and echo 'Hello from Claude Code!'"

echo "Running Claude Code with --print flag (non-interactive mode)..."
echo "Command: claude --print --output-format text <prompt>"
echo ""

# Run Claude Code exactly as the backend does
docker run --rm \
    -e ANTHROPIC_API_KEY="${ANTHROPIC_API_KEY}" \
    elide-builder:latest \
    /bin/bash -c "claude --print --output-format text '$TEST_PROMPT' 2>&1"

EXIT_CODE=$?

echo ""
echo "======================================"
echo "Test completed with exit code: $EXIT_CODE"
echo "======================================"
echo ""

if [ $EXIT_CODE -eq 0 ]; then
    echo "✓ SUCCESS: Claude Code ran without prompts!"
else
    echo "✗ FAILED: Claude Code exited with error"
    echo ""
    echo "Common issues:"
    echo "1. API key might be invalid/expired"
    echo "2. Network connectivity issues"
    echo "3. Docker image not built correctly"
    echo ""
    echo "To rebuild images: cd docker && ./build-images.sh"
fi

exit $EXIT_CODE

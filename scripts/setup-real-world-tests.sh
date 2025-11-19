#!/usr/bin/env bash
#
# Setup script for real-world integration tests
#
# This script clones popular open-source projects to /tmp for testing
# the Elide adopt command against real-world codebases.
#
# Usage:
#   ./scripts/setup-real-world-tests.sh
#   ./gradlew :packages:cli:test --tests "*Integration*"
#

set -e

TEMP_DIR="/tmp/elide-real-world-tests"
CLONE_DEPTH=1

echo "════════════════════════════════════════════════════════════════"
echo "  Elide Real-World Integration Test Setup"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "This script will download real-world projects to validate the"
echo "Elide adopt command against actual open-source codebases."
echo ""
echo "Target directory: $TEMP_DIR"
echo ""

# Create base directory
mkdir -p "$TEMP_DIR"
cd "$TEMP_DIR"

# Function to clone or update a repo
clone_or_update() {
  local name=$1
  local url=$2
  local target=$3

  if [ -d "$target/.git" ]; then
    echo "  ↻ Updating $name..."
    (cd "$target" && git pull --depth=$CLONE_DEPTH 2>&1 | head -5)
  else
    echo "  ↓ Cloning $name..."
    rm -rf "$target"
    git clone --depth=$CLONE_DEPTH "$url" "$target" 2>&1 | grep -E "(Cloning|done)" || true
  fi
}

echo "────────────────────────────────────────────────────────────────"
echo "  Python Projects"
echo "────────────────────────────────────────────────────────────────"

clone_or_update "FastAPI" \
  "https://github.com/tiangolo/fastapi.git" \
  "$TEMP_DIR/fastapi"

clone_or_update "Requests" \
  "https://github.com/psf/requests.git" \
  "$TEMP_DIR/requests"

clone_or_update "Black" \
  "https://github.com/psf/black.git" \
  "$TEMP_DIR/black"

clone_or_update "Hypothesis" \
  "https://github.com/HypothesisWorks/hypothesis.git" \
  "$TEMP_DIR/hypothesis"

echo ""
echo "────────────────────────────────────────────────────────────────"
echo "  Node.js Projects"
echo "────────────────────────────────────────────────────────────────"

clone_or_update "Express" \
  "https://github.com/expressjs/express.git" \
  "$TEMP_DIR/express"

clone_or_update "Vite" \
  "https://github.com/vitejs/vite.git" \
  "$TEMP_DIR/vite"

clone_or_update "Axios" \
  "https://github.com/axios/axios.git" \
  "$TEMP_DIR/axios"

clone_or_update "React (examples)" \
  "https://github.com/facebook/react.git" \
  "$TEMP_DIR/react"

echo ""
echo "────────────────────────────────────────────────────────────────"
echo "  Maven Projects"
echo "────────────────────────────────────────────────────────────────"

clone_or_update "Apache Commons Lang" \
  "https://github.com/apache/commons-lang.git" \
  "$TEMP_DIR/apache-commons-lang"

clone_or_update "Jackson Databind" \
  "https://github.com/FasterXML/jackson-databind.git" \
  "$TEMP_DIR/jackson-databind"

clone_or_update "OkHttp" \
  "https://github.com/square/okhttp.git" \
  "$TEMP_DIR/okhttp"

clone_or_update "Spring Cloud" \
  "https://github.com/spring-cloud/spring-cloud-release.git" \
  "$TEMP_DIR/spring-cloud"

echo ""
echo "────────────────────────────────────────────────────────────────"
echo "  Gradle Projects"
echo "────────────────────────────────────────────────────────────────"

clone_or_update "Kotlin Compiler" \
  "https://github.com/JetBrains/kotlin.git" \
  "$TEMP_DIR/kotlin"

echo ""
echo "────────────────────────────────────────────────────────────────"
echo "  Bazel Projects"
echo "────────────────────────────────────────────────────────────────"

clone_or_update "gRPC Java" \
  "https://github.com/grpc/grpc-java.git" \
  "$TEMP_DIR/grpc-java"

echo ""
echo "════════════════════════════════════════════════════════════════"
echo "  ✓ Setup Complete"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "Downloaded projects:"
ls -1 "$TEMP_DIR" | sed 's/^/  • /'
echo ""
echo "Run integration tests with:"
echo "  ./gradlew :packages:cli:test --tests '*IntegrationTest'"
echo ""
echo "Or run tests for a specific ecosystem:"
echo "  ./gradlew :packages:cli:test --tests '*PythonIntegrationTest'"
echo "  ./gradlew :packages:cli:test --tests '*ApacheCommonsLangIntegrationTest'"
echo "  ./gradlew :packages:cli:test --tests '*ExpressNodeIntegrationTest'"
echo ""

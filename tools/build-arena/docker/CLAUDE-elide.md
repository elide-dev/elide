# Build Arena Challenge: Elide Team

Welcome to Build Arena! You are the **Elide** agent competing in a head-to-head build race.

## Your Mission

Build and test a Java project using Elide as fast as possible, competing against the standard toolchain.

## Rules of Engagement

You have **FULL AUTONOMY** to use any strategy you want:

- ✓ Read documentation (online or in the repo)
- ✓ Search the internet for solutions
- ✓ Install any programs or tools you need
- ✓ Optimize build configurations
- ✓ Run builds in parallel
- ✓ Cache dependencies aggressively
- ✓ Skip non-essential steps (if you can justify it)
- ✓ Use any trick, optimization, or technique you can think of

**The only requirement**: Ring the bell (`\a`) when you're confident the binary is built and validated.

You decide how to verify compilation success - whether that's running tests, checking file signatures, attempting to execute the binary, or any other method you deem appropriate.

## Suggested Workflow

Here's a suggested approach, but feel free to improvise:

### 1. Download and Setup Elide

```bash
# Check if Elide is already installed
if ! command -v elide &> /dev/null; then
    echo "Installing Elide..."
    curl -fsSL https://elide.dev/install.sh | bash
    export PATH="$HOME/.elide/bin:$PATH"
fi

# Verify installation
elide --version
```

### 2. Clone the Repository

The repository URL will be provided as an environment variable `$REPO_URL`.

```bash
echo "Cloning repository: $REPO_URL"
git clone "$REPO_URL" project
cd project
```

### 3. Analyze the Project

Examine the project structure to determine the build system:

- Look for `build.gradle` or `build.gradle.kts` → Gradle project
- Look for `pom.xml` → Maven project
- Check for any special build requirements in README.md

```bash
# List project structure
ls -la

# Check for build files
if [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
    echo "Detected Gradle project"
    BUILD_SYSTEM="gradle"
elif [ -f "pom.xml" ]; then
    echo "Detected Maven project"
    BUILD_SYSTEM="maven"
else
    echo "Could not detect build system"
    exit 1
fi
```

### 4. Build the Project with Elide

Use Elide to build the project:

```bash
echo "Starting build with Elide..."
START_TIME=$(date +%s)

if [ "$BUILD_SYSTEM" = "gradle" ]; then
    # Build with Elide's Gradle integration
    elide gradle build --no-daemon
elif [ "$BUILD_SYSTEM" = "maven" ]; then
    # Build with Elide's Maven integration
    elide mvn clean package
fi

BUILD_EXIT_CODE=$?
```

### 5. Ring the Bell

When compilation succeeds, ring the terminal bell to signal completion:

```bash
if [ $BUILD_EXIT_CODE -eq 0 ]; then
    echo "✓ Build completed successfully!"
    # Ring the bell!
    echo -e "\a"
    printf '\a'
else
    echo "✗ Build failed with exit code $BUILD_EXIT_CODE"
    exit $BUILD_EXIT_CODE
fi
```

### 6. Find and Run Tests

Discover and run all available tests:

```bash
echo "Searching for tests..."

if [ "$BUILD_SYSTEM" = "gradle" ]; then
    # Run tests with Gradle
    echo "Running tests with Gradle..."
    elide gradle test --no-daemon
    TEST_EXIT_CODE=$?

    # Show test results
    if [ -d "build/test-results" ]; then
        echo "Test results:"
        find build/test-results -name "*.xml" -exec grep -H "tests=" {} \;
    fi

elif [ "$BUILD_SYSTEM" = "maven" ]; then
    # Run tests with Maven
    echo "Running tests with Maven..."
    elide mvn test
    TEST_EXIT_CODE=$?

    # Show test results
    if [ -d "target/surefire-reports" ]; then
        echo "Test results:"
        find target/surefire-reports -name "*.xml" -exec grep -H "tests=" {} \;
    fi
fi

END_TIME=$(date +%s)
TOTAL_TIME=$((END_TIME - START_TIME))

echo ""
echo "================================"
echo "🏁 ELIDE FINISHED!"
echo "================================"
echo "Total time: ${TOTAL_TIME}s"
echo "Build: $([ $BUILD_EXIT_CODE -eq 0 ] && echo '✓ PASS' || echo '✗ FAIL')"
echo "Tests: $([ $TEST_EXIT_CODE -eq 0 ] && echo '✓ PASS' || echo '✗ FAIL')"
echo "================================"

# Ring the bell again for test completion
echo -e "\a\a"
```

## Important Notes

- **Speed matters**: The faster you complete the build and tests, the better
- **Accuracy matters**: Don't skip steps or compromise on test coverage
- **Be verbose**: Echo progress so spectators can follow along
- **Handle errors gracefully**: If something fails, explain what went wrong
- **Use Elide features**: Leverage Elide's optimizations for the best performance

## Success Criteria

1. ✓ Repository cloned successfully
2. ✓ Build system detected
3. ✓ Project compiled with Elide
4. ✓ Bell rung after successful compilation
5. ✓ All tests found and executed
6. ✓ Final results reported

## Environment Variables

- `REPO_URL` - The Git repository URL to build
- `BUILD_TOOL` - Set to "elide" for your team

Good luck! May the fastest build tool win! 🚀

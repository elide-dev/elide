/**
 * Generate standard Maven/Gradle build script (fallback when Claude Code isn't available)
 */
export function generateStandardScript(): string {
  return `
    # Clone repository
    git clone "$REPO_URL" project || exit 1
    cd project

    # Detect build system
    if [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
      BUILD_SYSTEM="gradle"
    elif [ -f "pom.xml" ]; then
      BUILD_SYSTEM="maven"
    else
      echo "Could not detect build system"
      exit 1
    fi

    echo "Detected build system: $BUILD_SYSTEM"
    START_TIME=$(date +%s)

    # Build with standard tools
    if [ "$BUILD_SYSTEM" = "gradle" ]; then
      if [ -f "gradlew" ]; then
        chmod +x gradlew
        ./gradlew build --no-daemon
      else
        gradle build --no-daemon
      fi
    else
      if [ -f "mvnw" ]; then
        chmod +x mvnw
        ./mvnw clean package
      else
        mvn clean package
      fi
    fi
    BUILD_CODE=$?

    if [ $BUILD_CODE -eq 0 ]; then
      echo "✓ Build completed!"
      printf '\\a'  # Ring the bell
    fi

    # Run tests
    if [ "$BUILD_SYSTEM" = "gradle" ]; then
      if [ -f "gradlew" ]; then
        ./gradlew test --no-daemon
      else
        gradle test --no-daemon
      fi
    else
      if [ -f "mvnw" ]; then
        ./mvnw test
      else
        mvn test
      fi
    fi
    TEST_CODE=$?

    END_TIME=$(date +%s)
    echo ""
    echo "Total time: $((END_TIME - START_TIME))s"
    printf '\\a\\a'  # Ring bell twice

    exit $TEST_CODE
  `;
}

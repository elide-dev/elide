# Claude Code Instructions - STANDARD RUNNER (Maven/Gradle)

You are competing in the **Build Arena Race** using **Maven or Gradle**!

## 🏁 YOUR MISSION

1. **Clone the target repository**
2. **Build the project using Maven/Gradle** (both are pre-installed)
3. **🔔 RING THE BELL** to signal completion

## ⚠️ CRITICAL: YOU MUST RING THE BELL!

When done (success OR failure), output:
```
🔔 BUILD COMPLETE 🔔
Runner: MAVEN (or GRADLE)
Status: [SUCCESS/FAILURE]
```

## Step 1: Verify Build Tools

Maven and Gradle are pre-installed. Verify they're available:

```bash
# Verify Maven
mvn --version

# Verify Gradle
gradle --version
```

## Step 2: Clone Repository

```bash
cd /workspace
git clone <repository-url>
cd <repo-name>
```

## Step 3: Detect Build Tool

```bash
# Check what build tool the project uses
if [ -f "pom.xml" ]; then
    echo "📦 Maven project detected"
    BUILD_TOOL="maven"
elif [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
    echo "📦 Gradle project detected"
    BUILD_TOOL="gradle"
fi
```

## Step 4: Build

**IMPORTANT**: Redirect Maven/Gradle output to see completion status!

```bash
# Maven build - pipe to file and show tail
if [ "$BUILD_TOOL" = "maven" ]; then
    mvn clean package -DskipTests 2>&1 | tee /tmp/build.log
    # Show final result
    tail -20 /tmp/build.log
fi

# Gradle build - pipe to file and show tail
if [ "$BUILD_TOOL" = "gradle" ]; then
    gradle build -x test 2>&1 | tee /tmp/build.log
    # Show final result
    tail -20 /tmp/build.log
fi

# Check if build succeeded
if grep -q "BUILD SUCCESS" /tmp/build.log; then
    BUILD_STATUS="SUCCESS"
elif grep -q "BUILD FAILURE" /tmp/build.log; then
    BUILD_STATUS="FAILURE"
elif grep -q "BUILD SUCCESSFUL" /tmp/build.log; then
    BUILD_STATUS="SUCCESS"
elif grep -q "BUILD FAILED" /tmp/build.log; then
    BUILD_STATUS="FAILURE"
else
    BUILD_STATUS="UNKNOWN"
fi
```

## Step 5: Verify Build Artifacts

**CRITICAL**: Don't just trust the log - verify artifacts were actually created!

```bash
# Check for Maven artifacts
if [ "$BUILD_TOOL" = "maven" ]; then
    if [ -d "target" ]; then
        echo "✓ Found target/ directory"
        if ls target/*.jar 1> /dev/null 2>&1; then
            echo "✓ JAR files created:"
            ls -lh target/*.jar
            # For CLI tools, try running with --version
            # java -jar target/*.jar --version 2>/dev/null || echo "(Library project - no CLI)"
        else
            echo "⚠ No JAR files found in target/"
            BUILD_STATUS="FAILURE"
        fi
    else
        echo "✗ No target/ directory found"
        BUILD_STATUS="FAILURE"
    fi
fi

# Check for Gradle artifacts
if [ "$BUILD_TOOL" = "gradle" ]; then
    if [ -d "build/libs" ]; then
        echo "✓ Found build/libs/ directory"
        if ls build/libs/*.jar 1> /dev/null 2>&1; then
            echo "✓ JAR files created:"
            ls -lh build/libs/*.jar
            # For CLI tools, try running with --version
            # java -jar build/libs/*.jar --version 2>/dev/null || echo "(Library project - no CLI)"
        else
            echo "⚠ No JAR files found in build/libs/"
            BUILD_STATUS="FAILURE"
        fi
    else
        echo "✗ No build/libs/ directory found"
        BUILD_STATUS="FAILURE"
    fi
fi
```

## Step 6: 🔔 RING THE BELL!

**THIS IS MANDATORY** - Always ring the bell, even if the build fails!

```bash
echo ""
echo "🔔 BUILD COMPLETE 🔔"
echo "Runner: MAVEN" # or GRADLE
echo "Status: ${BUILD_STATUS}"
```

## Example Complete Workflow

```bash
# Verify Maven is installed
mvn --version

# Clone repo
cd /workspace
git clone https://github.com/google/gson.git
cd gson

# Build
mvn clean package -DskipTests 2>&1 | tee /tmp/build.log

# Check build status
if grep -q "BUILD SUCCESS" /tmp/build.log; then
    BUILD_STATUS="SUCCESS"
else
    BUILD_STATUS="FAILURE"
fi

# Verify artifacts
if ls target/*.jar 1> /dev/null 2>&1; then
    echo "✓ JAR files created:"
    ls -lh target/*.jar
else
    echo "✗ No JAR files found"
    BUILD_STATUS="FAILURE"
fi

# Ring the bell!
echo ""
echo "🔔 BUILD COMPLETE 🔔"
echo "Runner: MAVEN"
echo "Status: ${BUILD_STATUS}"
```

---

**Remember**: Clone → Build → **Verify artifacts** → 🔔 RING THE BELL 🔔

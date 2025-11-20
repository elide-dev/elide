# Claude Code Instructions - Elide Build Arena

You are Claude Code running inside a Docker container designed to test Elide builds against standard Java build tools.

## 🏁 YOUR MISSION: THE BUILD ARENA RACE

This is an **automated benchmark race** between Elide and traditional build tools (Maven/Gradle). Your job is to:

1. **Download build tools** (if not already available)
2. **Clone the target repository**
3. **Analyze the project structure**
4. **Convert Maven project to Elide format** (CRITICAL STEP!)
5. **Build the project** using Elide
6. **Run tests** to verify the build
7. **🔔 RING THE BELL** to signal completion

## ⚠️ CRITICAL: THE FINISH LINE

**YOU MUST RING THE BELL WHEN DONE!**

When you complete the build (success or failure), you MUST output a completion signal. Use one of these formats:

```
🔔 BUILD COMPLETE 🔔
Status: [SUCCESS/FAILURE]
Build Time: X.XX seconds
Tool: Elide
```

OR use one of these bell patterns:
- `🔔` (bell emoji)
- `[BUILD COMPLETE]`
- `[SUCCESS]` or `[FAILURE]`
- `BUILD SUCCEEDED` or `BUILD FAILED`
- `Total time: XX seconds` (Maven/Gradle format)

**The WebSocket monitoring system watches for these signals to determine when the race is finished!**

## Environment

- **Java**: OpenJDK 17 (Temurin)
- **Elide**: Pre-installed (`elide --version` to verify)
- **Claude Code**: You! (version shown at startup)
- **Working Directory**: `/workspace`
- **API Key**: Pre-configured via `ANTHROPIC_API_KEY` environment variable

## Required Workflow Steps

### Step 1: Verify Build Tools

Elide is pre-installed - verify it's working:
```bash
elide --version
```

Expected output: `Elide 1.0.0-beta10` or similar

### Step 2: Clone the Repository

```bash
cd /workspace
git clone <repository-url>
cd <repo-name>
```

### Step 3: Analyze the Project

```bash
# Look for build files
ls -la

# Check for Maven
cat pom.xml

# Check for Gradle
cat build.gradle build.gradle.kts

# Check if multi-module project
ls -la */pom.xml 2>/dev/null

# Check README for special instructions
cat README.md
```

### Step 4: **CONVERT MAVEN TO ELIDE FORMAT** (CRITICAL!)

**This is the most important step!** Elide requires an `elide.pkl` file to build Maven projects.

Use the `elide adopt` command to convert Maven or Gradle projects:

```bash
# For Maven projects
elide adopt maven

# For Gradle projects
elide adopt gradle
```

**What `elide adopt` does:**
- Detects the build system (Maven or Gradle)
- Parses `pom.xml` or `build.gradle[.kts]` to extract dependencies
- Resolves versions from dependency management
- Maps source directories (src/main/java, src/test/java, etc.)
- Generates `elide.pkl` in Elide's PKL format
- Handles multi-module projects automatically

**Important notes:**
- Works for both single and multi-module projects
- Supports Maven and Gradle build systems
- May skip test dependencies if versions can't be resolved
- Main source compilation should succeed even if tests fail

### Step 5: Build with Elide (TIMED!)

```bash
# Use 'time' command to measure build duration
START_TIME=$(date +%s)
elide build
END_TIME=$(date +%s)
BUILD_TIME=$((END_TIME - START_TIME))
```

**Expected output:**
```
Building <project-name>
[Xms] Configuring project
[Xms] Dependencies ready
[Xms] Compiling N Java source files
[Xms] Packing main.jar
✓ Build successful
```

### Step 6: Verify Build Artifacts

**CRITICAL**: Don't just trust the output - verify artifacts were actually created!

```bash
# Check for Elide artifacts
if [ -d ".dev/artifacts" ]; then
    echo "✓ Found .dev/artifacts/ directory"
    if find .dev/artifacts -name "*.jar" | grep -q .; then
        echo "✓ JAR files created:"
        find .dev/artifacts -name "*.jar" -exec ls -lh {} \;
        BUILD_STATUS="SUCCESS"
    else
        echo "⚠ No JAR files found in .dev/artifacts/"
        BUILD_STATUS="FAILURE"
    fi
else
    echo "✗ No .dev/artifacts/ directory found"
    BUILD_STATUS="FAILURE"
fi
```

### Step 7: Run Tests (Optional)

```bash
elide java test
# OR
./mvnw test
# OR
./gradlew test
```

### Step 6: 🔔 RING THE BELL 🔔

**THIS IS MANDATORY!** Output a clear completion signal:

```bash
echo "🔔 BUILD COMPLETE 🔔"
echo "Status: SUCCESS"
echo "Build Time: 45.2 seconds"
echo "Tool: Elide"
```

## Example Complete Workflow

```bash
# Step 1: Verify tools
elide --version  # Should show: Elide 1.0.0-beta10

# Step 2: Clone repository
cd /workspace
git clone https://github.com/google/gson.git
cd gson

# Step 3: Analyze (multi-module project detected)
ls -la
cat pom.xml
ls -la */pom.xml

# Step 4: CONVERT TO ELIDE FORMAT
elide adopt maven
cat elide.pkl  # Verify conversion

# Step 5: Build (timed)
echo "Starting build..."
START_TIME=$(date +%s)
time elide build
END_TIME=$(date +%s)
BUILD_TIME=$((END_TIME - START_TIME))

# Verify artifacts
find . -name "*.jar" -path "*/.dev/artifacts/*"

# Step 6: Test (optional, may fail for test deps)
elide test || echo "Tests skipped or failed"

# Step 7: RING THE BELL!
echo ""
echo "🔔 BUILD COMPLETE 🔔"
echo "Status: SUCCESS"
echo "Build Time: ${BUILD_TIME} seconds"
echo "Tool: Elide"
echo "Repository: google/gson"
echo "Artifacts: $(find . -name "*.jar" -path "*/.dev/artifacts/*" | wc -l) JAR files"
```

## Elide Commands Reference

```bash
elide help                        # Show all commands
elide project advice              # Analyze project and get recommendations
elide build                       # Build project (requires elide.pkl)
elide test                        # Run tests
elide package                     # Create JAR/WAR
elide clean                       # Clean build artifacts
```

## Elide Adopt Reference

**Command:** `elide adopt <build-system>`

**Supported build systems:**
- `maven` - Converts Maven projects (pom.xml)
- `gradle` - Converts Gradle projects (build.gradle or build.gradle.kts)

**Usage:**
```bash
# Auto-detect build system
elide adopt

# Explicitly specify Maven
elide adopt maven

# Explicitly specify Gradle
elide adopt gradle
```

**Output:** Creates `elide.pkl` in the current directory

**Example output:**
```pkl
amends "elide:project.pkl"

name = "my-project"
description = "My Java project"

dependencies {
  maven {
    packages {
      "com.google.guava:guava:33.4.8-jre"
    }
    testPackages {
      "junit:junit:4.13.2"
    }
  }
}

sources {
  ["main"] = "src/main/java/**/*.java"
  ["test"] = "src/test/java/**/*.java"
}
```

## Popular Test Repositories

### Small & Fast (< 1 minute)
- `google/gson` - JSON library (Maven)
- `google/guice` - Dependency injection (Maven)
- `joda-time/joda-time` - Date/time library (Maven)

### Medium (1-5 minutes)
- `google/guava` - Core libraries (Maven)
- `square/okhttp` - HTTP client (Gradle)
- `junit-team/junit5` - Testing framework (Gradle)

### Large (5-15 minutes)
- `spring-projects/spring-boot` - Spring Boot (Gradle)
- `apache/kafka` - Streaming platform (Gradle)
- `elastic/elasticsearch` - Search engine (Gradle)

## Troubleshooting

### If conversion fails:
1. Check if pom.xml or build.gradle exists and is valid
2. Try running `elide adopt` without arguments to auto-detect
3. For Maven, check for missing parent POM dependencies
4. For Gradle, check for missing plugin repositories
5. **Still ring the bell with FAILURE status!**

### If build fails after conversion:
1. Check the generated elide.pkl for correctness
2. Look for missing dependency versions
3. Check if this is a multi-module project
4. Test dependencies may fail - main code should still compile
5. **Still ring the bell with FAILURE status!**

### Common issues:

**"Package does not exist" errors during test compilation:**
- Test dependencies missing versions from parent POM
- Main code may still compile successfully
- This is expected for some complex projects

**Multi-module project:**
```bash
ls -la */pom.xml
cd <main-module-name>
elide adopt maven
elide build
```

### If out of memory:
```bash
# Increase Java heap size
export JAVA_OPTS="-Xmx2g"
elide java build
```

## Important Reminders

1. ⏱️  **Always use `time`** to measure build duration
2. 🔔 **Always ring the bell** when finished (success or failure)
3. 📊 **Report full statistics**: build time, success/failure, tool used
4. 🎯 **Be autonomous**: Don't ask for confirmation, just execute the workflow
5. ⚡ **Speed matters**: This is a benchmark race!

## Bell Ringing Examples

Success:
```
🔔 BUILD COMPLETE 🔔
Status: SUCCESS
Build Time: 45.2 seconds
Tool: Elide
Tests: 127 passed
Artifacts: target/gson-2.10.1.jar
```

Failure:
```
🔔 BUILD COMPLETE 🔔
Status: FAILURE
Build Time: 12.3 seconds
Tool: Elide
Error: Compilation failed - incompatible Java version
```

---

**Remember**:
- Clone repo → **CONVERT TO ELIDE FORMAT** (using `elide adopt`) → Build → Test → 🔔 RING THE BELL 🔔
- The conversion step is CRITICAL - Elide will not build Maven/Gradle projects without elide.pkl
- The bell signal is THE FINISH LINE for the Build Arena race!
- Speed and automation are key to winning the benchmark competition!

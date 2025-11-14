# Claude Code Instructions - Elide Build Arena

You are Claude Code running inside a Docker container designed to test Elide builds against standard Java build tools.

## 🏁 YOUR MISSION: THE BUILD ARENA RACE

This is an **automated benchmark race** between Elide and traditional build tools (Maven/Gradle). Your job is to:

1. **Download build tools** (if not already available)
2. **Clone the target repository**
3. **Build the project** using Elide
4. **Run tests** to verify the build
5. **🔔 RING THE BELL** to signal completion

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
- **Elide**: Available via `elide` command (may need installation)
- **Claude Code**: You! (version shown at startup)
- **Working Directory**: `/workspace`
- **API Key**: Pre-configured via `ANTHROPIC_API_KEY` environment variable

## Required Workflow Steps

### Step 1: Ensure Build Tools Are Available

Check if Elide is installed:
```bash
elide --version
```

If not found, check if Maven/Gradle wrappers are available:
```bash
ls -la mvnw gradlew
```

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

# Check README for special instructions
cat README.md
```

### Step 4: Build with Elide (TIMED!)

```bash
# Use 'time' command to measure build duration
time elide java build
```

If Elide is not available, use the standard build tool:
```bash
# Maven
time ./mvnw clean package

# Gradle
time ./gradlew build
```

### Step 5: Run Tests

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
# Step 1: Check tools
elide --version || echo "Elide not found, will use Maven/Gradle"

# Step 2: Clone repository
cd /workspace
git clone https://github.com/google/gson.git
cd gson

# Step 3: Analyze
ls -la
cat pom.xml

# Step 4: Build (timed)
echo "Starting build..."
START_TIME=$(date +%s)
time elide java build
END_TIME=$(date +%s)
BUILD_TIME=$((END_TIME - START_TIME))

# Step 5: Test
elide java test

# Step 6: RING THE BELL!
echo ""
echo "🔔 BUILD COMPLETE 🔔"
echo "Status: SUCCESS"
echo "Build Time: ${BUILD_TIME} seconds"
echo "Tool: Elide"
echo "Repository: google/gson"
```

## Elide Commands Reference

```bash
elide help                        # Show all commands
elide project advice              # Analyze project and get recommendations
elide java build                  # Build Java project
elide java test                   # Run tests
elide java package                # Create JAR/WAR
elide java run <mainClass>        # Run main class
elide java clean                  # Clean build artifacts
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

### If Elide command not found:
```bash
which elide
echo $PATH
# Fall back to Maven/Gradle wrapper
./mvnw clean package
# OR
./gradlew build
```

### If build fails:
1. Check Java version compatibility
2. Look for missing dependencies
3. Check internet connectivity for downloads
4. Review error messages carefully
5. **Still ring the bell with FAILURE status!**

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
- Download tools → Clone repo → Build → Test → 🔔 RING THE BELL 🔔
- The bell signal is THE FINISH LINE for the Build Arena race!
- Speed and automation are key to winning the benchmark competition!

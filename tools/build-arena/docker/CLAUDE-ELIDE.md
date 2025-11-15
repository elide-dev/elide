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
- **Python 3**: Pre-installed for running conversion scripts
- **Claude Code**: You! (version shown at startup)
- **Working Directory**: `/workspace`
- **API Key**: Pre-configured via `ANTHROPIC_API_KEY` environment variable
- **Maven Converter**: Pre-installed at `/app/maven-to-elide.py`

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

For single-module projects:
```bash
python3 /app/maven-to-elide.py pom.xml
cat elide.pkl  # Verify conversion
```

For multi-module projects:
```bash
# Navigate to the main submodule (usually same name as repo)
cd <main-module-name>
python3 /app/maven-to-elide.py pom.xml
cat elide.pkl  # Verify conversion
```

**What the converter does:**
- Parses `pom.xml` to extract dependencies
- Resolves versions from dependency management
- Maps source directories (src/main/java, src/test/java)
- Generates `elide.pkl` in Elide's format

**Important notes:**
- Works best for single-module Maven projects
- Projects without custom plugins
- Test dependencies may fail if inherited from parent POM
- Main source compilation should succeed

### Step 5: Build with Elide (TIMED!)

```bash
# Use 'time' command to measure build duration
time elide build
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

**Verify artifacts:**
```bash
find . -name "*.jar" -path "*/.dev/artifacts/*"
ls -la .dev/artifacts/jvm/jars/
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

# Navigate to main module
cd gson

# Step 4: CONVERT TO ELIDE FORMAT
python3 /app/maven-to-elide.py pom.xml
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

## Maven-to-Elide Converter Reference

**Location:** `/app/maven-to-elide.py`

**Usage:**
```bash
python3 /app/maven-to-elide.py [path-to-pom.xml]
```

**Output:** Creates `elide.pkl` in the same directory as the pom.xml

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
1. Check if pom.xml exists and is valid XML
2. For multi-module projects, navigate to the submodule first
3. Check warnings about missing versions - may need parent POM
4. **Still ring the bell with FAILURE status!**

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
- This is a known limitation of the current converter

**Multi-module project:**
```bash
ls -la */pom.xml
cd <main-module-name>
python3 /app/maven-to-elide.py pom.xml
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
- Download tools → Clone repo → **CONVERT TO ELIDE FORMAT** → Build → Test → 🔔 RING THE BELL 🔔
- The conversion step is CRITICAL - Elide will not build Maven projects without elide.pkl
- The bell signal is THE FINISH LINE for the Build Arena race!
- Speed and automation are key to winning the benchmark competition!

# Claude Code Instructions - ELIDE RUNNER

You are competing in the **Build Arena Race** using **Elide**!

## 🏁 YOUR MISSION

1. **Download and install Elide**
2. **Clone the target repository**
3. **Build the project using Elide**
4. **🔔 RING THE BELL** to signal completion

## ⚠️ CRITICAL: YOU MUST RING THE BELL!

When done (success OR failure), output:
```
🔔 BUILD COMPLETE 🔔
Runner: ELIDE
Status: [SUCCESS/FAILURE]
```

## Step 1: Install Elide

Elide is NOT pre-installed. Download it with this EXACT command:

```bash
curl -sSL https://static.elide.dev/cli/install.sh | bash -s - --version=1.0.0-beta10 && export PATH="$HOME/.elide/bin:$PATH" && elide --version
```

That's it! This single command will:
1. Download and install Elide
2. Add it to your PATH
3. Verify it's working

**Copy and run that exact command above.** Don't modify it.

## Step 2: Clone Repository

```bash
cd /workspace
git clone <repository-url>
cd <repo-name>
```

## Step 3: Build with Elide

**IMPORTANT**: Elide is super simple! It automatically detects Maven, Gradle, NPM, and other project types.

Just run:
```bash
elide build
```

That's it! Elide will auto-detect the project type and build it.

**⚠️ CRITICAL RULE: NO FALLBACK TO MAVEN/GRADLE!**

You MUST use Elide for building. **DO NOT** fall back to Maven (`mvn`/`mvnw`) or Gradle (`gradle`/`gradlew`) commands even if:
- Elide is not installed (install it first with the command in Step 1!)
- Elide build fails (report FAILURE status)
- You see `mvnw` or `gradlew` wrapper scripts in the repo

This is the **Elide Runner** - using Maven/Gradle invalidates the benchmark!

## Step 4: Verify the Build

**CRITICAL**: Don't just trust the exit code - verify artifacts were created!

```bash
# Check for build artifacts
if [ -d "target" ]; then
    echo "✓ Found target/ directory"
    ls -lh target/*.jar 2>/dev/null && echo "✓ JAR files created" || echo "⚠ No JARs found"
fi

if [ -d "build/libs" ]; then
    echo "✓ Found build/libs/ directory"
    ls -lh build/libs/*.jar 2>/dev/null && echo "✓ JAR files created" || echo "⚠ No JARs found"
fi

# Try to run the artifact if it's a CLI tool
# (This will fail for library projects, which is fine)
if [ -f "target/*.jar" ] || [ -f "build/libs/*.jar" ]; then
    echo "Attempting to verify artifact..."
    # Try running with --help or --version if it's a CLI tool
    # java -jar target/*.jar --version 2>/dev/null || echo "Not a CLI tool (expected for libraries)"
fi
```

## Step 5: 🔔 RING THE BELL!

**THIS IS MANDATORY** - Determine success/failure based on build AND artifacts:

```bash
# Determine final status
if [ $? -eq 0 ] && ([ -f target/*.jar ] || [ -f build/libs/*.jar ]); then
    BUILD_STATUS="SUCCESS"
else
    BUILD_STATUS="FAILURE"
fi

echo ""
echo "🔔 BUILD COMPLETE 🔔"
echo "Runner: ELIDE"
echo "Status: ${BUILD_STATUS}"
```

## Example Complete Workflow

```bash
# 1. Install Elide (use the EXACT command from above)
curl -sSL https://static.elide.dev/cli/install.sh | bash -s - --version=1.0.0-beta10 && export PATH="$HOME/.elide/bin:$PATH" && elide --version

# 2. Clone repo
cd /workspace
git clone https://github.com/google/gson.git
cd gson

# 3. Build with Elide
elide build

# 4. Verify artifacts
ls -lh target/*.jar 2>/dev/null || ls -lh build/libs/*.jar 2>/dev/null

# 5. Determine status and ring the bell
if [ $? -eq 0 ] && ([ -f target/*.jar ] || [ -f build/libs/*.jar ]); then
    BUILD_STATUS="SUCCESS"
else
    BUILD_STATUS="FAILURE"
fi

echo ""
echo "🔔 BUILD COMPLETE 🔔"
echo "Runner: ELIDE"
echo "Status: ${BUILD_STATUS}"
```

## Key Points

- ✅ **Use the EXACT install command**: `curl -sSL https://static.elide.dev/cli/install.sh | bash -s - --version=1.0.0-beta10 && export PATH="$HOME/.elide/bin:$PATH" && elide --version`
- ✅ **Elide is simple**: Just run `elide build` - it auto-detects Maven, Gradle, NPM, etc.
- ✅ **Verify artifacts**: Check that JAR files were actually created before marking as SUCCESS
- ✅ **Always ring the bell**: Output the completion signal even if the build fails!

---

**Quick Reference:**
1. Install: `curl -sSL https://static.elide.dev/cli/install.sh | bash -s - --version=1.0.0-beta10 && export PATH="$HOME/.elide/bin:$PATH" && elide --version`
2. Clone: `git clone <url> && cd <repo>`
3. Build: `elide build`
4. Verify: `ls target/*.jar || ls build/libs/*.jar`
5. Bell: `echo "🔔 BUILD COMPLETE 🔔"`

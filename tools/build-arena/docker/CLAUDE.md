# Claude Code Instructions - Elide Build Arena

You are Claude Code running inside a Docker container designed to test Elide builds against standard Java build tools.

## Your Mission

Help users build Java projects using **Elide** - a high-performance polyglot runtime and build tool. Your goal is to:

1. Clone and analyze Java repositories
2. Build projects using Elide instead of Maven/Gradle
3. Report build times and results
4. Compare performance against standard toolchains

## Environment

- **Java**: OpenJDK 17 (Temurin)
- **Elide**: Available via `elide` command
- **Claude Code**: You! (version shown at startup)
- **Working Directory**: `/workspace`
- **API Key**: Pre-configured via `ANTHROPIC_API_KEY` environment variable

## Quick Commands

### Check Available Tools
```bash
java -version          # Java 17
elide --version        # Elide runtime
git --version          # Git for cloning repos
```

### Clone a Repository
```bash
cd /workspace
git clone <repository-url>
cd <repo-name>
```

### Analyze the Project
```bash
# Look for build files
ls -la

# Check for Maven
cat pom.xml

# Check for Gradle
cat build.gradle build.gradle.kts
```

### Build with Elide
```bash
# Elide can run Maven and Gradle projects directly
elide project advice              # Get Elide recommendations
elide java build                  # Build Java project
elide java test                   # Run tests
elide java package                # Create JAR/WAR
```

## Common Tasks

### Task 1: Build a Maven Project
```bash
# Example: google/gson
cd /workspace
git clone https://github.com/google/gson.git
cd gson

# Analyze the project
elide project advice

# Build with Elide
time elide java build

# Run tests
elide java test
```

### Task 2: Build a Gradle Project
```bash
# Example: square/okhttp
cd /workspace
git clone https://github.com/square/okhttp.git
cd okhttp

# Analyze the project
elide project advice

# Build with Elide
time elide java build
```

### Task 3: Compare Build Times
```bash
# Build with Elide (timed)
time elide java build

# For comparison, you could also try:
# ./mvnw clean package  # Maven wrapper
# ./gradlew build       # Gradle wrapper
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

## Tips

1. **Always use `time`** before build commands to measure performance
2. **Check for wrapper scripts** (mvnw, gradlew) in cloned repos
3. **Read README files** to understand project structure
4. **Report errors clearly** - include full stack traces
5. **Save build logs** for later analysis

## Example Workflow

When a user asks you to test a Java project:

1. **Clone the repository**
   ```bash
   cd /workspace
   git clone <url>
   cd <repo-name>
   ```

2. **Analyze the build setup**
   ```bash
   ls -la
   cat pom.xml || cat build.gradle
   ```

3. **Get Elide's advice**
   ```bash
   elide project advice
   ```

4. **Build with Elide (timed)**
   ```bash
   time elide java build
   ```

5. **Report results**
   - Build time
   - Success/failure
   - Any errors or warnings
   - Output artifacts (JARs, WARs, etc.)

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
# Elide may not be installed yet - use standard Maven/Gradle
```

### If build fails:
1. Check Java version compatibility
2. Look for missing dependencies
3. Check internet connectivity for downloads
4. Review error messages carefully

### If out of memory:
```bash
# Increase Java heap size
export JAVA_OPTS="-Xmx2g"
elide java build
```

## Notes

- Elide aims to be **faster** than Maven/Gradle by optimizing build steps
- Elide can **cache** dependencies and build artifacts
- Elide supports **polyglot** projects (Java, Python, JavaScript, Ruby, etc.)
- This container is designed for **testing and benchmarking** only

## Questions?

If you're unsure how to proceed:
1. Ask the user for clarification
2. Check the project's README
3. Use `elide project advice` for guidance
4. Try standard build tools (mvn, gradle) as fallback

---

**Remember**: Your goal is to help test Elide's performance and compatibility with real-world Java projects. Be thorough, measure everything, and report honestly!

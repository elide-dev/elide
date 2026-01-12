# Build Arena Race - Standard Runner (Maven/Gradle)

You are competing in a build race! Your goal is to build a Java project using Maven or Gradle.

## Critical Instructions

1. **Clone the repository** into /workspace
2. **Detect build tool**: Check for pom.xml (Maven) or build.gradle (Gradle)
3. **Build the project**: Use `mvn clean package -DskipTests` or `gradle build -x test`
4. **Verify artifacts**: Check that target/*.jar or build/libs/*.jar were created
5. **Ring the bell**: Output "🔔 BUILD COMPLETE 🔔" with Status: SUCCESS or FAILURE

## Important Notes

- Maven and Gradle are pre-installed in this container
- For Maven projects: Use `mvn` command
- For Gradle projects: Use `gradle` command
- Skip tests with `-DskipTests` (Maven) or `-x test` (Gradle) for faster builds
- ALWAYS ring the bell when done, even on failure

## Example Workflow (Maven)

```bash
cd /workspace
git clone REPO_URL
cd REPO_NAME
mvn clean package -DskipTests
ls -lh target/*.jar
echo "🔔 BUILD COMPLETE 🔔"
echo "Status: SUCCESS"
```

## Example Workflow (Gradle)

```bash
cd /workspace
git clone REPO_URL
cd REPO_NAME
gradle build -x test
ls -lh build/libs/*.jar
echo "🔔 BUILD COMPLETE 🔔"
echo "Status: SUCCESS"
```

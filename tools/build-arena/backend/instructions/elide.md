# Build Arena Race - Elide Runner

You are competing in a build race! Your goal is to build a Java/Maven project using Elide.

## Critical Instructions

1. **Clone the repository** into /workspace
2. **Convert to Elide format**: Run `elide adopt maven` (or `elide adopt gradle`)
3. **Build with Elide**: Run `elide build`
4. **Verify artifacts**: Check that .dev/artifacts/ was created with JAR files
5. **Ring the bell**: Output "🔔 BUILD COMPLETE 🔔" with Status: SUCCESS or FAILURE

## Important Notes

- `elide adopt` does NOT require Maven/Gradle to be installed - it just parses pom.xml/build.gradle
- After conversion, use ONLY Elide commands (`elide build`), NOT Maven/Gradle
- If `elide adopt` completes successfully, proceed to `elide build`
- ALWAYS ring the bell when done, even on failure

## Example Workflow

```bash
cd /workspace
git clone REPO_URL
cd REPO_NAME
elide adopt maven
elide build
find .dev/artifacts -name "*.jar"
echo "🔔 BUILD COMPLETE 🔔"
echo "Status: SUCCESS"
```

# CLI Reference

Complete reference for all Elide command-line interface commands.

## Global Options

These options work with all commands:

```
--debug              Activate debugging features and extra logging
--frozen             Treat lockfile as frozen (CI mode)
--no-frozen          Allow lockfile modifications
-h, --help           Show help message
--lockfile           Enable Elide's lockfile system (default)
--no-lockfile        Disable lockfile
-p, --project=<path> Path to the project
--pretty             Enable colorized output (default)
--no-pretty          Disable colorized output
-q, --quiet          Squelch most output
--timeout=<secs>     Timeout when exiting (in seconds)
-v, --verbose        Enable verbose output (debug logging)
--version            Print version information
--no-telemetry       Disable all telemetry features
```

## Quick Command Reference

| Command | Description |
|---------|-------------|
| `elide <file>` | Run a source file directly |
| `elide run` | Run a script or start a server |
| `elide repl` | Start interactive shell |
| `elide serve` | Start HTTP server |
| `elide init` | Initialize new project |
| `elide install` | Install dependencies |
| `elide add` | Add dependencies |
| `elide build` | Build project |
| `elide test` | Run tests |
| `elide dev` | Development mode |
| `elide project` | Manage projects |
| `elide javac` | Java compiler |
| `elide kotlinc` | Kotlin compiler |
| `elide jar` | JAR tool |
| `elide native-image` | Native compilation |
| `elide pkl` | Pkl tools |
| `elide secrets` | Manage secrets |
| `elide lsp` | Language Server Protocol |
| `elide mcp` | Model Context Protocol |
| `elide info` | System information |
| `elide help` | Help and documentation |

---

## Running Code

### `elide <file>`

Run a source file directly (most common usage):

```bash
elide script.ts
elide script.js
elide script.py
elide script.kt
elide script.kts
```

**Supported extensions:**
- `.js`, `.mjs`, `.cjs` - JavaScript
- `.ts`, `.mts`, `.cts` - TypeScript
- `.jsx`, `.tsx` - JSX/TSX (limited at CLI, works when imported)
- `.py` - Python 3.12
- `.kt`, `.kts` - Kotlin
- `.java` - Java (compiled first)

**With arguments:**
```bash
elide script.ts --arg1 value --flag
```

**With options:**
```bash
elide --debug script.ts      # Debug mode
elide --verbose script.ts    # Verbose logging
```

### `elide run`

Alias for running files, or run project scripts:

```bash
# Run a file
elide run script.ts

# Run a project script (from elide.pkl)
elide run dev

# With code
elide run --code "console.log('Hello')"
```

**Options:**
```
--code <CODE>        Execute code string
--port <PORT>        Port for server mode
```

### `elide repl`

Start an interactive REPL:

```bash
# JavaScript REPL (default)
elide repl

# Python REPL
elide repl --python

# TypeScript REPL
elide repl --typescript
```

**REPL Features:**
- Multi-line input
- Auto-completion (where supported)
- Access to Node APIs
- Import npm modules

### `elide serve`

Start an HTTP server:

```bash
# Serve current directory
elide serve

# Serve specific directory
elide serve ./public

# Custom port
elide serve --port 8080

# With code
elide serve --code "app.ts"
```

**Options:**
```
--port <PORT>        Port to listen on (default: 3000)
--code <FILE>        Server entry point
--host <HOST>        Host to bind to
```

**Server Types:**
- Static files (if directory)
- Dynamic app (if code specified)
- Web builder output

---

## Project Management

### `elide init`

Initialize a new Elide project:

```bash
# Interactive mode (may crash in beta10)
elide init

# Non-interactive with template
elide init my-app --yes

# Specific template
elide init my-app empty --yes
elide init my-app ktjvm --yes
elide init my-app flask --yes
```

**Templates:**
- `empty` - Blank project
- `ktjvm` - Kotlin/JVM
- `java` - Java project
- `mavenjvm` - Maven-based JVM
- `containers` - Container-ready app
- `flask` - Python Flask app
- `flask-react` - Flask + React SSR
- `web-static-worker` - Static site

**Options:**
```
--build              Run build after init
--no-build           Skip build (default)
--force              Overwrite existing files
-i, --interactive    Prompt for questions (default if TTY)
--no-interactive     Non-interactive mode
--install            Run install after init
--no-install         Skip install (default)
--mcp                Create .mcp.json file
-n, --name=<str>     Project name
--test               Run tests after init
--yes                Assume yes to all questions
```

**⚠️ Beta10 Issue:** Interactive mode crashes. Always use `--yes`!

### `elide install` / `elide i`

Install project dependencies:

```bash
# Install all dependencies
elide install

# Install with frozen lockfile (CI mode)
elide install --frozen

# Install without lockfile
elide install --no-lockfile
```

**What it does:**
1. Parses `elide.pkl` (or `package.json`, `pom.xml`)
2. Resolves dependencies (npm, Maven, PyPI)
3. Downloads to cache
4. Symlinks into project
5. Generates lockfile

**Options:**
```
--frozen             Treat lockfile as frozen
--no-lockfile        Skip lockfile generation
```

### `elide add`

Add new dependencies:

```bash
# Add npm package
elide add express
elide add express@4.18.2

# Add Maven package
elide add com.google.guava:guava:32.1.3-jre
```

**Coordinate formats:**
- NPM: `package` or `package@version`
- Maven: `group:artifact:version`
- PyPI: `package` or `package==version` (alpha)

**What it does:**
1. Adds to `elide.pkl`
2. Runs `elide install`

### `elide build`

Build the project:

```bash
# Standard build
elide build

# Native binary
elide build --native

# Container image
elide build --container

# JAR file
elide build --jar
```

**Options:**
```
--native             Build GraalVM native binary
--container          Build container image
--jar                Build JAR file
--output=<path>      Output location
--frozen             Use frozen lockfile
```

**Build outputs:**
- Bytecode/interpreted (default)
- Native binary (`.exe` or no extension)
- Container image (OCI format)
- JAR file (`.jar`)

### `elide test`

Run project tests:

```bash
# Run all tests
elide test

# Run specific test
elide test ./tests/my-test.ts

# With reports
elide test --junit-xml --html
```

**Options:**
```
--junit-xml          Generate JUnit XML report
--html               Generate HTML report
--coverage           Generate coverage report
--frozen             Use frozen lockfile
```

**Test Runner:**
- Discovers tests automatically
- Supports multiple languages
- Parallel execution
- Detailed output

**Test discovery:**
- Files matching `**/*.test.{js,ts,py,kt}`
- Files matching `**/*.spec.{js,ts,py,kt}`
- Files in `tests/` or `test/` directories

### `elide dev`

Run in development mode:

```bash
elide dev
```

**Features:**
- Auto-reload on file changes
- Hot module replacement (where supported)
- Detailed error messages
- Debug mode enabled

**Options:**
```
--port <PORT>        Port for dev server
--host <HOST>        Host to bind to
```

### `elide project`

Manage Elide projects:

```bash
# Show project info
elide project info

# Validate project
elide project validate

# Show project advice (AI-powered)
elide project advice
```

**Options:**
```
-p, --project=<path> Project path
```

---

## Toolchain Commands

Elide can replace standard JVM tools:

### `elide javac`

Drop-in replacement for `javac`:

```bash
elide javac -- Main.java

# With options
elide javac -- -source 17 -target 17 Main.java
elide javac -- --release 21 Main.java
```

**Supported flags:**
- `--source <version>`
- `--target <version>`
- `--release <version>`
- `-d <directory>`
- `-s <directory>`
- `-classpath <path>`

**Benefits:**
- Faster compilation
- Integrated with Elide build system
- Dependency-aware
- Caching

### `elide kotlinc`

Drop-in replacement for `kotlinc`:

```bash
elide kotlinc -- Main.kt

# With options
elide kotlinc -- -jvm-target 17 Main.kt
```

**Benefits:**
- No separate Kotlin installation needed
- Integrated build graph
- Better error messages
- Faster builds

### `elide jar`

Build JAR files:

```bash
elide jar -- cvf app.jar .
elide jar -- xvf app.jar
elide jar -- tvf app.jar
```

**Supported operations:**
- `c` - Create JAR
- `x` - Extract JAR
- `t` - List contents
- `u` - Update JAR

### `elide javadoc`

Generate Java documentation:

```bash
elide javadoc -- Main.java
```

### `elide native-image`

Build native binary with GraalVM:

```bash
elide native-image -- Main

# With options
elide native-image -- --no-fallback Main
```

**Options:** All GraalVM Native Image options supported

**Benefits:**
- No separate GraalVM installation
- Integrated reflection config
- Better defaults for Elide apps

### `elide jib`

Build container images:

```bash
elide jib -- build
```

**Features:**
- Build Docker/OCI images
- No Docker daemon needed
- Fast, incremental builds
- Push to registries

### `elide pkl`

Apple Pkl tools:

```bash
# Evaluate Pkl file
elide pkl eval config.pkl

# Generate from Pkl
elide pkl codegen config.pkl
```

---

## Advanced Commands

### `elide lsp`

Start Language Server Protocol server:

```bash
elide lsp
```

**Features:**
- IDE integration
- Auto-completion
- Go to definition
- Type checking
- Refactoring support

**Usage:**
- Configure your IDE to use `elide lsp`
- Works with VS Code, IntelliJ, vim, emacs

### `elide mcp`

Start Model Context Protocol server:

```bash
elide mcp
```

**Features:**
- AI assistant integration
- Project understanding
- Code suggestions
- Architecture advice

**Configuration:**
Automatically created in `.mcp.json` by `elide init`

### `elide secrets`

Manage secrets:

```bash
# Set a secret
elide secrets set API_KEY "xxx"

# Get a secret
elide secrets get API_KEY

# List secrets
elide secrets list

# Delete a secret
elide secrets delete API_KEY
```

**Features:**
- Encrypted storage
- Environment-specific (dev/staging/prod)
- Rotation support
- Audit logging

**Access in code:**
```typescript
const apiKey = process.env.API_KEY;
```

### `elide which`

Print path to tool binary:

```bash
elide which elide
elide which javac
elide which kotlinc
```

---

## Information Commands

### `elide info`

Show system and Elide information:

```bash
elide info
```

**Displays:**
- Elide version
- Platform information
- Installed components
- Java/GraalVM version
- Available languages

### `elide help` / `elide docs`

Get help:

```bash
# General help
elide help

# Command-specific help
elide help build
elide build --help

# Open documentation
elide docs
```

### `elide bug` / `elide issue`

Report issues:

```bash
elide bug
```

Opens browser to GitHub issues with template.

### `elide discord`

Join Discord community:

```bash
elide discord
```

Opens link to Discord server.

### `elide completions`

Generate shell completions:

```bash
# Bash
elide completions bash > ~/.bash_completions/elide

# Zsh
elide completions zsh > ~/.zsh/completions/_elide
```

---

## Exit Codes

Elide uses standard exit codes:

- `0` - Success
- `1` - Generic failure
- `2` - Exception in user code
- `-1` - Uncaught system exception

**Usage in scripts:**
```bash
if elide test; then
  echo "Tests passed!"
else
  echo "Tests failed with code: $?"
fi
```

---

## Environment Variables

Elide respects these environment variables:

### `JAVA_HOME`

Path to Java/GraalVM installation (optional in beta8+):

```bash
export JAVA_HOME=/opt/graalvm-jdk-25
elide --version
```

### `ELIDE_HOME`

Elide installation directory:

```bash
export ELIDE_HOME=/usr/local/bin
```

### `NO_COLOR`

Disable colored output:

```bash
export NO_COLOR=1
elide build
```

### `ELIDE_DEBUG`

Enable debug mode:

```bash
export ELIDE_DEBUG=1
elide run app.ts
```

---

## Configuration Files

### `.eliderc`

User-level configuration:

```bash
~/.eliderc
```

**Example:**
```
telemetry=false
pretty=true
verbose=false
```

### `elide.pkl`

Project manifest (see [Core Concepts](../core-concepts/README.md)):

```pkl
amends "elide:project.pkl"

name = "my-app"
scripts { "dev" = "elide serve" }
dependencies { npm { packages { "express@4.18.2" } } }
```

### `.mcp.json`

Model Context Protocol configuration:

```json
{
  "mcpServers": {
    "elide": {
      "command": "elide",
      "args": ["mcp"],
      "type": "stdio"
    }
  }
}
```

---

## Tips & Tricks

### Faster Builds

```bash
# Use frozen lockfile (no dependency resolution)
elide build --frozen

# Parallel tasks
elide build --parallel
```

### Debugging

```bash
# Enable all debug features
elide --debug --verbose run app.ts

# With profiling
elide --profile run app.ts
```

### CI/CD

```bash
# Reproducible builds
elide install --frozen
elide test --frozen
elide build --frozen --native
```

### Development

```bash
# Watch mode (in development)
elide dev

# Or use external tools
watchexec -e ts,js "elide run app.ts"
```

---

**Next**:
- [Troubleshooting →](../troubleshooting/README.md)
- [Examples →](../examples/README.md)

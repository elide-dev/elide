# Getting Started with Elide

Welcome to Elide! This guide will have you running your first polyglot application in minutes.

## What is Elide?

Elide is a fast, batteries-included polyglot runtime that combines JavaScript, TypeScript, Python, Kotlin, and Java in a single, optimized environment built on GraalVM.

### Why Choose Elide?

**For JavaScript/TypeScript Developers:**
- Run TypeScript instantly without `tsc` or build steps
- Node.js API compatibility (most code works unchanged)
- 100x faster cold starts than Node.js
- Native compilation for production deployments

**For Python Developers:**
- Mix Python with JavaScript/TypeScript seamlessly
- Access the npm ecosystem from Python
- 3x faster than CPython in many benchmarks

**For JVM Developers:**
- Run Kotlin without prior compilation
- Drop-in replacements for `javac`, `kotlinc`, `jar`
- Native image compilation included
- Gradle integration

## Installation

### Prerequisites

- **Operating System**: Linux (amd64/arm64) or macOS (arm64/M1)
- **Disk Space**: ~200MB for Elide binary
- **RAM**: 1GB minimum (4GB+ recommended)

> ⚠️ **Windows Support**: Coming soon (not available in beta10)

### Quick Install

**Linux (amd64/arm64) and macOS (arm64):**
```bash
curl -sSL elide.sh | bash
```

This script will:
1. Detect your platform
2. Download the latest Elide binary
3. Install to `/usr/local/bin/elide`
4. Add to your PATH

### Homebrew (macOS/Linux)

```bash
brew tap elide-dev/elide
brew install elide
```

### Manual Installation

1. **Download** the release for your platform:
   - [Linux amd64](https://github.com/elide-dev/elide/releases/download/1.0.0-beta10/elide-1.0.0-beta10-linux-amd64.tgz)
   - [Linux arm64](https://github.com/elide-dev/elide/releases/download/1.0.0-beta10/elide-1.0.0-beta10-linux-aarch64.tgz)
   - [macOS arm64](https://github.com/elide-dev/elide/releases/download/1.0.0-beta10/elide-1.0.0-beta10-darwin-aarch64.tgz)

2. **Extract** the archive:
   ```bash
   tar -xzf elide-1.0.0-beta10-*.tgz
   ```

3. **Install** the binary:
   ```bash
   sudo mv elide-*/elide /usr/local/bin/
   sudo chmod +x /usr/local/bin/elide
   ```

4. **Verify** installation:
   ```bash
   elide --version
   # Should output: 1.0.0-beta10
   ```

## Your First Elide Program

### 1. Hello World (TypeScript)

Create a file `hello.ts`:

```typescript
// hello.ts - TypeScript runs without build steps!

interface Greeting {
  message: string;
  timestamp: Date;
}

function greet(name: string): Greeting {
  return {
    message: `Hello, ${name}!`,
    timestamp: new Date(),
  };
}

const result = greet("Elide");
console.log(result.message);
console.log(`Time: ${result.timestamp.toISOString()}`);
```

**Run it:**
```bash
elide hello.ts
```

**Output:**
```
Hello, Elide!
Time: 2025-11-05T08:30:00.000Z
```

✨ **Notice**: No `tsc`, no `build` step, no `tsconfig.json` needed!

### 2. Using Node.js APIs

Create `node-example.ts`:

```typescript
// node-example.ts - Node APIs just work

import * as os from "node:os";
import * as path from "node:path";
import { Buffer } from "node:buffer";

console.log("System Information:");
console.log(`  Platform: ${os.platform()}`);
console.log(`  CPUs: ${os.cpus().length} cores`);
console.log(`  Memory: ${(os.totalmem() / 1024 ** 3).toFixed(2)} GB`);

const projectPath = path.join(os.homedir(), "projects", "my-app");
console.log(`\nProject path: ${projectPath}`);

const encoded = Buffer.from("Hello Elide").toString("base64");
console.log(`\nBase64 encoded: ${encoded}`);
```

**Run it:**
```bash
elide node-example.ts
```

### 3. Python Example

Create `hello.py`:

```python
# hello.py - Python 3.12 support

def fibonacci(n: int) -> int:
    """Calculate fibonacci number"""
    if n <= 1:
        return n
    return fibonacci(n - 1) + fibonacci(n - 2)

def main():
    print("Python in Elide!")

    numbers = [fibonacci(i) for i in range(10)]
    print(f"First 10 Fibonacci numbers: {numbers}")

    # Python 3.12 f-string features
    value = 42
    print(f"The answer is {value = }")

if __name__ == "__main__":
    main()
```

**Run it:**
```bash
elide hello.py
```

## Creating a Project

For anything beyond a single file, create an Elide project:

### 1. Initialize Project

```bash
# Create a new project (non-interactive)
elide init my-app --yes

# Or choose a template interactively (may crash in beta10)
# elide init my-app
```

**Available templates:**
- `empty` - Blank project
- `ktjvm` - Kotlin/JVM project
- `java` - Java project
- `mavenjvm` - Maven-based JVM project
- `flask` - Python Flask web app
- `flask-react` - Flask + React SSR
- `web-static-worker` - Static website

### 2. Project Structure

```
my-app/
├── .gitignore
├── .mcp.json          # Model Context Protocol config
├── elide.pkl          # Project manifest (like package.json)
├── README.md
└── .dev/              # Build artifacts
```

### 3. Project Manifest (`elide.pkl`)

```pkl
amends "elide:project.pkl"

name = "my-app"
description = "My awesome Elide project"

scripts {
  "dev" = "elide serve --port 3000"
  "build" = "elide build"
  "test" = "elide test"
}

dependencies {
  npm {
    packages {
      "lodash@4.17.21"
      "date-fns@2.30.0"
    }
  }
}
```

### 4. Working with Your Project

```bash
cd my-app

# Install dependencies
elide install

# Run a script
elide dev

# Run tests
elide test

# Build for production
elide build
```

## Key Concepts

### 1. No Build Step for TypeScript

Elide uses **OXC** (Oxidation Compiler) written in Rust to parse TypeScript in milliseconds. Type information is stripped at runtime, so you get instant execution.

```bash
# These both work instantly:
elide app.ts
elide app.js
```

### 2. Project Manifests with Pkl

Instead of JSON, Elide uses [Apple's Pkl](https://pkl-lang.org) for configuration. Pkl is:
- Type-safe
- Can validate at write-time
- Supports code generation
- More expressive than JSON/YAML

**But:** Elide also supports `package.json` and `pom.xml` directly!

### 3. Polyglot Dependencies

One project can depend on:
- NPM packages (JavaScript/TypeScript)
- Maven packages (Java/Kotlin)
- PyPI packages (Python) - *alpha support*

```pkl
dependencies {
  npm {
    packages {
      "express@4.18.2"
    }
  }
  maven {
    packages {
      "com.google.guava:guava:32.1.3-jre"
    }
  }
}
```

## Common Tasks

### Running Scripts

```bash
# Run a file directly
elide script.ts

# Run with arguments
elide script.ts --arg1 value

# Run a project script
elide build
```

### REPL (Interactive Shell)

```bash
# JavaScript REPL
elide repl

# Python REPL
elide repl --python
```

### Serving Applications

```bash
# Serve a web app
elide serve

# Serve static directory
elide serve ./public

# Custom port
elide serve --port 8080
```

### Testing

```bash
# Run all tests
elide test

# Run specific test file
elide test ./tests/my-test.ts

# Generate reports
elide test --junit-xml --html
```

### Building

```bash
# Standard build
elide build

# Native binary (GraalVM Native Image)
elide build --native

# Container image
elide build --container

# JAR file (for JVM projects)
elide build --jar
```

## Next Steps

Now that you have Elide running:

1. **[Core Concepts](../core-concepts/README.md)** - Learn how Elide works under the hood
2. **[Language Guides](../languages/README.md)** - Deep dive into your language of choice
3. **[Node.js API Reference](../api-reference/nodejs.md)** - See what Node APIs are supported
4. **[Examples](../examples/README.md)** - Browse real-world example projects

## Troubleshooting

### "Command not found: elide"

The binary isn't in your PATH. Either:
- Re-run the install script
- Or manually add to PATH: `export PATH="/usr/local/bin:$PATH"`

### TypeScript syntax error

Make sure your TypeScript syntax is valid. Elide supports TypeScript 5.8.x.

Some features have limited support:
- ❌ JSX/TSX at command-line (works when imported)
- ✅ All standard TS features (interfaces, generics, etc.)

### "Module not found"

For NPM packages:
1. Run `elide install` first
2. Check that `node_modules/` exists
3. Some modern packages use "exports" field - not supported in beta10

### Interactive prompts crash

This is a known issue in beta10. Always use `--yes` flag:
```bash
elide init my-app --yes
```

## Getting Help

- **Documentation**: [Full docs](../README.md)
- **Discord**: `elide discord` or visit [elide.dev/discord](https://elide.dev/discord)
- **Issues**: `elide bug` or visit [GitHub Issues](https://github.com/elide-dev/elide/issues)
- **Examples**: Check the [examples/](../examples/) directory

---

**Next**: [Core Concepts →](../core-concepts/README.md)

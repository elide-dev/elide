# Elide Sample Projects

This directory contains sample projects demonstrating Elide's capabilities across different languages and use cases.

## Quick Start

Initialize a sample project using `elide init`:

```bash
# Interactive mode - choose from available templates
elide init

# Or specify a template directly
elide init ktjvm
```

## Available Samples

| Sample | Description | Languages |
|--------|-------------|-----------|
| [`ktjvm`](./ktjvm) | Kotlin/JVM project with tests | Kotlin |
| [`java`](./java) | Pure Java project with tests | Java |
| [`mavenjvm`](./mavenjvm) | Maven-based Java project | Java |
| [`containers`](./containers) | JARs, Native Image, and containers | Kotlin |
| [`flask`](./flask) | Python Flask web server | Python |
| [`flask-react`](./flask-react) | Polyglot Flask + React app | Python, TypeScript |
| [`web-static-worker`](./web-static-worker) | Static site with Cloudflare Workers | Markdown, TypeScript |

## Sample Details

### Kotlin/JVM (`ktjvm`)

Basic Kotlin project using Elide's built-in toolchain.

```bash
elide init ktjvm my-kotlin-app
cd my-kotlin-app
elide build
elide test
```

### Java (`java`)

Pure Java project compiled with Elide's embedded `javac`.

```bash
elide init java my-java-app
cd my-java-app
elide build
elide test
```

### Flask (`flask`)

Python Flask server running on Elide's Python engine.

```bash
elide init flask my-flask-app
cd my-flask-app
elide serve
```

### Flask + React (`flask-react`)

Polyglot application combining Python backend with React frontend - demonstrates cross-language interoperability.

```bash
elide init flask-react my-fullstack-app
cd my-fullstack-app
elide install
elide serve
```

## Project Structure

Each sample uses `elide.pkl` as its manifest file (Elide's equivalent of `package.json` or `pom.xml`):

```pkl
amends "elide:project.pkl"

name = "my-project"
description = "Project description"

dependencies {
  maven {
    packages {
      "com.google.guava:guava"
    }
  }
  npm {
    packages {
      "react@18"
    }
  }
}
```

## Running Samples

```bash
# Build the project
elide build

# Run tests
elide test

# Start a server (for web projects)
elide serve

# Run a specific file
elide run ./src/main.kt
```

## Learn More

- [Elide Documentation](https://docs.elide.dev)
- [Project Guide](https://docs.elide.dev/guides/projects)
- [CLI Reference](https://docs.elide.dev/cli)

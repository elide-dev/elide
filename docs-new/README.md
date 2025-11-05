# Elide Documentation

**Complete, accurate, hands-on tested documentation for Elide 1.0.0-beta10**

> 🚀 Elide is a polyglot runtime for JavaScript, TypeScript, Python, Kotlin, and Java
> Built on GraalVM for extreme performance and zero-overhead cross-language calls

## Quick Links

- [Getting Started Guide](./getting-started/README.md) - Install and run your first Elide app
- [Core Concepts](./core-concepts/README.md) - Understand how Elide works
- [Language Guides](./languages/README.md) - Deep dives for each language
- [API Reference](./api-reference/README.md) - Complete Node.js API compatibility matrix
- [Migration Guides](./migration/README.md) - Migrate from Node.js, Deno, Bun, or Python

## What Makes Elide Different?

### 1. Polyglot by Design
Run JavaScript, TypeScript, Python, Kotlin, and Java in a single process with zero-overhead cross-language calls. No JSON serialization. No separate runtimes. Just pure, optimized code.

### 2. TypeScript Without Build Steps
```bash
elide app.ts  # TypeScript runs instantly, no tsc needed
```

### 3. Sub-5ms Cold Starts
- **Node.js**: ~200ms cold start
- **Deno**: ~15ms cold start
- **Elide**: **<5ms cold start**

### 4. Native Compilation
```bash
elide build --native  # Single binary, <1ms startup
```

### 5. Node.js API Compatible
Most Node APIs work out of the box. Your existing code likely runs unchanged.

## Installation

### Quick Install (Linux amd64, macOS arm64)
```bash
curl -sSL elide.sh | bash
```

### Homebrew
```bash
brew tap elide-dev/elide
brew install elide
```

### Manual Download
Download from [GitHub Releases](https://github.com/elide-dev/elide/releases/tag/1.0.0-beta10)

## Quick Start

```bash
# Run TypeScript directly
echo 'console.log("Hello Elide!")' > hello.ts
elide hello.ts

# Create a new project
elide init my-app --yes
cd my-app

# Install dependencies
elide install

# Run tests
elide test

# Build for production
elide build
```

## Documentation Status

This documentation is based on:
- ✅ Official release notes (beta1-beta10)
- ✅ Hands-on testing of all features
- ✅ Source code analysis
- ✅ Real-world usage validation

**Last Updated**: 2025-11-05
**Elide Version**: 1.0.0-beta10
**Status**: Comprehensive rewrite (replacing outdated docs.elide.dev)

## Structure

```
elide-docs-new/
├── getting-started/     # Installation, first app, basics
├── core-concepts/       # Runtime architecture, how it works
├── languages/           # JS, TS, Python, Kotlin, Java guides
├── api-reference/       # Node.js API compatibility
├── advanced/            # Native compilation, polyglot, performance
├── migration/           # Moving from other runtimes
├── troubleshooting/     # Common issues and solutions
└── examples/            # Real-world example projects
```

## Contributing to These Docs

These docs were created through systematic exploration:
1. Analysis of all beta release notes (beta1-beta10)
2. Hands-on testing of every feature
3. Source code examination (40+ packages)
4. Validation of claims and capabilities

Found an issue? See [CONTRIBUTING.md](./CONTRIBUTING.md)

---

**Next**: [Getting Started →](./getting-started/README.md)

# Core Concepts

Understanding how Elide works will help you build faster, more efficient applications.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                     Elide CLI                           │
│  (elide run, elide build, elide test, elide serve)     │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│              Elide Runtime Engine                       │
│  • OXC TypeScript Parser (Rust)                        │
│  • Module Router & Loader                              │
│  • VFS (Virtual File System)                           │
│  • Intrinsics Manager                                  │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│               GraalVM Truffle                           │
│  ┌────────┬────────┬────────┬────────┬──────────┐     │
│  │GraalJS │GraalPy │Espresso│GraalWasm│TruffleRuby│   │
│  │(JS/TS) │(Python)│ (Java) │ (Wasm)  │ (Ruby)   │     │
│  └────────┴────────┴────────┴────────┴──────────┘     │
│                                                         │
│  • Cross-language optimization                         │
│  • Shared GC across languages                         │
│  • Speculative inlining                               │
│  • Graal JIT Compiler                                 │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│              Native Platform                            │
│  Linux (amd64/arm64), macOS (arm64), JVM                │
└─────────────────────────────────────────────────────────┘
```

## Key Technologies

### 1. GraalVM & Truffle

**GraalVM** is an advanced JVM that includes:
- **Graal Compiler**: High-performance JIT compiler
- **Truffle**: Language implementation framework
- **Native Image**: Ahead-of-time (AOT) compiler

**Why GraalVM?**
- **Polyglot by design**: Languages can interoperate at the AST level
- **Peak performance**: Often faster than V8 (Node.js) or CPython
- **Native compilation**: Create standalone binaries
- **Cross-language optimization**: Inline across language boundaries

### 2. Language Engines

#### GraalJS (JavaScript/TypeScript)
- ECMAScript 2024 compatible
- TypeScript via OXC parser (Rust)
- Node.js API compatibility layer
- WebAssembly support

#### GraalPython (Python 3.12)
- CPython 3.12 compatible
- Supports most standard library
- Can use some C extensions
- ⚠️ Alpha status in beta10

#### Espresso (Java/JVM)
- Java 25 support
- Full JVM bytecode execution
- Can run Kotlin, Scala, Groovy
- Native image compilation

#### TruffleRuby (Ruby)
- Ruby 3.x compatible
- ⚠️ Experimental in beta10

#### GraalWasm (WebAssembly)
- WASI support
- Can interop with other languages

### 3. OXC Parser

**OXC** (Oxidation Compiler) is a Rust-based JavaScript/TypeScript parser that's:
- **Fast**: Parses in milliseconds
- **Complete**: Supports all TS 5.8 features
- **Accurate**: Maintains source maps

Elide uses OXC to:
1. Parse TypeScript files instantly
2. Strip type information
3. Generate JavaScript AST
4. Pass to GraalJS for execution

**This is why TypeScript runs without `tsc`!**

## Runtime Execution Flow

### Single File Execution

```bash
elide app.ts
```

**What happens:**

```
1. CLI parses command line
   └→ Identifies: source file = app.ts

2. Runtime detects language
   └→ Extension .ts = TypeScript

3. OXC Parser (if TypeScript)
   ├→ Parse TypeScript
   ├→ Strip types
   └→ Generate JS AST

4. Module Router
   ├→ Resolve imports/requires
   ├→ Load dependencies from node_modules
   └→ Handle node: prefixed imports

5. GraalJS Execution
   ├→ Interpret or compile (depending on warmup)
   ├→ Run with Node API polyfills
   └→ Output to console

Total time: ~20ms cold, ~2ms hot
```

### Project Execution

```bash
cd my-project
elide install
elide build
```

**What happens:**

```
1. elide install
   ├→ Parse elide.pkl (or package.json)
   ├→ Resolve dependencies (npm, maven, pypi)
   ├→ Download to cache
   ├→ Generate lockfile
   └→ Symlink into project

2. elide build
   ├→ Parse project manifest
   ├→ Discover source files
   ├→ Determine build graph
   ├→ Compile sources (if needed)
   ├→ Bundle or package
   └→ Output build artifacts
```

## Module Resolution

Elide implements a sophisticated module router that handles:

### ES Modules (ESM)
```javascript
import { foo } from "./module.js";
import * as os from "node:os";
```

### CommonJS (CJS)
```javascript
const foo = require("./module");
const os = require("os");
```

### Resolution Algorithm

1. **Check if node: prefix**
   - `node:fs` → Use Elide's Node API polyfill
   - Implemented in Kotlin as intrinsics

2. **Check if relative path**
   - `./module.js` → Load from file system
   - Uses VFS (can be embedded or host)

3. **Check if npm package**
   - `lodash` → Look in node_modules/lodash
   - Parse package.json for entry point
   - ⚠️ "exports" field not supported in beta10

4. **Check if Maven coordinate**
   - `com.google.guava:guava` → Load from Maven cache
   - Only in JVM/Kotlin contexts

### Module Caching

Modules are cached at multiple levels:
1. **Parsed AST cache** - Avoid reparsing
2. **Bytecode cache** - Skip compilation
3. **Import cache** - Reuse resolved modules

## Virtual File System (VFS)

Elide uses a **Hybrid VFS** that supports:

### Host FS
- Direct access to host file system
- Used for development
- Can be restricted in production

### Embedded VFS
- Files bundled into the binary
- Used for native images
- Zero I/O overhead

### Network VFS
- Fetch remote modules (future)
- Similar to Deno's HTTP imports

**Configuration:**
```pkl
vfs {
  mode = "hybrid"  // or "host" or "embedded"
  allowHostAccess = true
}
```

## Node.js API Compatibility

Elide implements Node APIs as **intrinsics** - native Kotlin implementations that are injected into the JavaScript runtime.

### Implementation Architecture

```
┌──────────────────────────────────────┐
│  JavaScript Code                     │
│  import * as fs from "node:fs"       │
└──────────────┬───────────────────────┘
               │
┌──────────────▼───────────────────────┐
│  Module Router                       │
│  Detects "node:" prefix              │
└──────────────┬───────────────────────┘
               │
┌──────────────▼───────────────────────┐
│  Intrinsics Manager                  │
│  Maps "fs" → NodeFilesystem class    │
└──────────────┬───────────────────────┘
               │
┌──────────────▼───────────────────────┐
│  NodeFilesystem (Kotlin)             │
│  Implements: readFile, writeFile...  │
│  Uses: GraalVM Polyglot APIs        │
└──────────────────────────────────────┘
```

**31 Node API modules are implemented:**
- `buffer` - ✅ Full support
- `os` - ✅ Full support
- `path` - ✅ Full support
- `process` - ✅ Most features
- `fs` - ⚠️ Read operations work, writes limited
- `events` - ❌ EventEmitter not exported (beta10 issue)
- `crypto` - ⚠️ Use Web Crypto API instead
- Many more...

See [Node.js API Reference](../api-reference/nodejs.md) for complete compatibility matrix.

## Polyglot Capabilities

### How Cross-Language Works

**Traditional approach (Node + Python):**
```
┌──────┐     JSON/HTTP      ┌────────┐
│ Node │ ←──────────────→  │ Python │
└──────┘   Serialization    └────────┘
  Runtime 1                  Runtime 2
  Heap 1                     Heap 2
```

**Elide approach:**
```
┌────────────────────────────────────┐
│    Single Runtime (GraalVM)        │
│  ┌──────┐         ┌────────┐      │
│  │  JS  │ ←────→  │ Python │      │
│  └──────┘ Direct  └────────┘      │
│           calls                     │
│                                     │
│  Shared Heap, Shared GC            │
│  Zero serialization overhead       │
└────────────────────────────────────┘
```

### ⚠️ Beta10 Status

Full polyglot Python integration is **alpha**. TypeScript→Python imports don't work yet.

**What works:**
- ✅ JavaScript + Java/Kotlin
- ✅ Python execution standalone
- ❌ JavaScript → Python imports
- ❌ Python `@bind` decorator

## Performance Characteristics

### Cold Start (First Run)

| Runtime | Cold Start Time |
|---------|----------------|
| Node.js v20 | ~200ms |
| Deno v1.38 | ~15ms |
| Bun v1.0 | ~6.5ms |
| **Elide beta10** | **~20ms** |

### Hot Requests (After Warmup)

| Runtime | Response Time |
|---------|--------------|
| Node.js v20 | ~7ms |
| Deno v1.38 | ~71ms |
| Bun v1.0 | ~1.5ms |
| **Elide beta10** | **~2ms** |

### Why Elide is Fast

1. **GraalVM JIT Compiler**
   - More aggressive optimization than V8
   - Cross-language inlining
   - Speculative optimization

2. **OXC Parser**
   - Rust implementation is extremely fast
   - Aggressive AST caching

3. **Native Image Mode**
   - Ahead-of-time compilation
   - Zero JVM warmup
   - **<1ms startup time**

4. **Efficient Module System**
   - Smart caching at multiple levels
   - Pre-compilation of common modules
   - Virtual file system for embedded resources

### Native Image Performance

```bash
elide build --native
```

**Benefits:**
- **Startup**: <1ms (vs ~200ms for Node)
- **Memory**: 50-70% smaller than Node + node_modules
- **Distribution**: Single binary, no runtime needed
- **Deployment**: Works anywhere, no dependencies

**Tradeoffs:**
- **Build time**: Longer (minutes vs seconds)
- **Peak throughput**: Slightly lower than JIT
- **Dynamic features**: Some reflection/proxy features limited

## Build System

Elide includes a sophisticated build system that understands multiple languages:

### Build Graph

```
elide build
```

**Steps:**
1. **Parse manifests** - Read elide.pkl, package.json, pom.xml
2. **Discover sources** - Find all .ts, .js, .py, .kt, .java files
3. **Analyze dependencies** - Build dependency graph
4. **Determine tasks** - What needs compilation?
5. **Execute tasks** - Parallel compilation where possible
6. **Generate artifacts** - JAR, native binary, or bundle

### Build Targets

```bash
# Standard build (bytecode/interpreted)
elide build

# Native binary (GraalVM Native Image)
elide build --native

# Container image (Docker/OCI)
elide build --container

# JAR file (Java projects)
elide build --jar
```

### Build Caching

Elide caches aggressively:
- **Source-level cache** - Track file changes
- **Dependency cache** - Don't re-download
- **Compilation cache** - Reuse compiled bytecode
- **Lockfile** - Ensure reproducibility

## Configuration (Pkl)

Elide uses Pkl instead of JSON/YAML because:

### Type Safety
```pkl
// This is validated at write-time!
dependencies {
  npm {
    packages {
      "express@4.18.2"  // Valid
      123                // ERROR: must be string
    }
  }
}
```

### Inheritance
```pkl
amends "elide:project.pkl"  // Inherit from base
```

### Code Generation
```pkl
// Can generate TypeScript types, JSON schemas, etc.
```

### Validation
```pkl
port: Int(isBetween(1000, 65535))  // Compile-time check
```

## Toolchain Integration

Elide can replace standard tools:

```bash
# Instead of javac
elide javac -- Main.java

# Instead of kotlinc
elide kotlinc -- Main.kt

# Instead of jar
elide jar -- cvf app.jar .

# Instead of native-image
elide native-image -- Main
```

**Benefits:**
- Consistent interface across languages
- Shared dependency resolution
- Integrated build caching
- Polyglot project support

## Security Model

### Sandboxing

Elide can restrict access:
```pkl
security {
  allowHostFileAccess = false
  allowHostNetwork = false
  allowEnv = false
}
```

### Secrets Management

```bash
# Store secrets securely
elide secrets set API_KEY "xxx"

# Access in code
const apiKey = process.env.API_KEY;
```

**Features:**
- Encrypted storage
- Environment-specific (dev/staging/prod)
- Rotation support
- Audit logging

## Development Tools

### LSP (Language Server Protocol)

```bash
elide lsp
```

Provides IDE integration:
- Auto-completion
- Go to definition
- Type checking (TypeScript)
- Refactoring support

### MCP (Model Context Protocol)

```bash
elide mcp
```

Allows AI assistants to:
- Understand your project structure
- Suggest code improvements
- Answer questions about your code

### Debugging

```bash
elide --debug app.ts
```

**Features:**
- Chrome DevTools integration
- Breakpoints
- Step debugging
- Variable inspection

### Profiling

```bash
elide --profile app.ts
```

Generates:
- CPU flame graphs
- Memory allocation reports
- GC statistics

---

**Next**:
- [JavaScript/TypeScript Guide →](../languages/javascript-typescript.md)
- [Node.js API Reference →](../api-reference/nodejs.md)
- [Advanced Topics →](../advanced/README.md)

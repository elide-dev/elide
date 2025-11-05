# Elide OSS Conversion Strategy

**Date**: 2025-11-05 (Birthday Mission! 🎂)
**Elide Version**: beta10
**Goal**: Convert 20+ OSS projects to Elide

## Key Intel from Elide Team

**From Dario (2025-11-05)**:
> "dario: on pause while I finish HTTP
> dario: not for long though I'm finishing that this week"

**HTTP is coming soon!** But we can't wait - let's convert what works NOW.

## What WORKS in Beta10

✅ **Core Node APIs**:
- `node:os` - Full support
- `node:path` - Full support
- `node:buffer` - Full support
- `node:process` - Full support (env fixed!)
- `node:url` - Full support
- `node:querystring` - Full support
- `node:util` - Full support
- `node:fs` - READ operations work

✅ **Language Features**:
- TypeScript without build steps
- ES modules & CommonJS
- Async/await
- Top-level await
- Modern JS/TS features

✅ **Performance**:
- 10x faster cold starts
- Native compilation support

## What DOESN'T Work (Beta10 Blockers)

❌ **HARD BLOCKERS**:
1. `http.createServer` - NOT IMPLEMENTED (coming this week!)
2. `events.EventEmitter` export - Broken
3. Package.json "exports" field - Not supported
4. Native modules (C++ addons)
5. Python polyglot - Alpha

⚠️ **WORKAROUND NEEDED**:
6. `node:crypto` - Use Web Crypto API instead
7. `fs` write operations - Limited
8. Complex `child_process` usage

## Conversion Strategy: 3 Tiers

### 🥇 **TIER 1: CLI Tools & Utilities** (Convert THESE!)

Perfect for Elide - no HTTP needed:
- Command-line parsers
- File processors
- Data converters (CSV, JSON, YAML)
- Text utilities (markdown, formatters)
- Build tools
- Code generators
- Linters & formatters
- Testing utilities

**Why these work**:
- Use only supported Node APIs
- No HTTP server
- Pure computation
- Easy to test

### 🥈 **TIER 2: Libraries** (Good candidates)

Pure computation libraries:
- Math libraries
- String manipulation
- Date/time utilities
- Validation libraries
- Parsing libraries
- Algorithm implementations

### 🥉 **TIER 3: Web Apps** (Wait for HTTP)

Need `http.createServer`:
- REST APIs
- Web servers
- GraphQL servers
- WebSocket servers

**Status**: BLOCKED until this week's HTTP update

## 20+ Conversion Targets

### **A. CLI Tools** (10 projects)

1. **JSON/YAML Processors**
   - `json-diff` - Compare JSON files
   - `yaml-lint` - YAML validator
   - `jsonlint` - JSON formatter

2. **Markdown Tools**
   - `tiny-markdown-parser` - Minimal parser
   - `@croct/md-lite` - Zero-dep parser
   - `markdown-link-check` - Link validator

3. **File Utilities**
   - `rimraf` alternatives - File deletion
   - `glob` alternatives - Pattern matching
   - `chokidar` alternatives - File watching

4. **Build/Dev Tools**
   - Simple TypeScript linters
   - Code formatters
   - AST processors

### **B. Data Processing** (5 projects)

5. **CSV Tools**
   - Simple CSV parser implementations
   - CSV to JSON converters

6. **JSON Tools**
   - JSON schema validators
   - JSON transformers

7. **Text Processing**
   - String utilities
   - Template engines (no I/O)

### **C. Algorithms & Math** (5 projects)

8. **Pure Computation**
   - Sorting algorithms
   - Search algorithms
   - Math utilities
   - Data structures

### **D. When HTTP Lands** (Future)

9. **REST APIs**
   - Simple Express apps
   - API examples
   - CRUD apps

10. **Static Servers**
    - File servers
    - Development servers

## Conversion Process

1. **Clone** original repo
2. **Analyze** dependencies
3. **Identify blockers**
4. **Convert** to TypeScript if needed
5. **Test** with Elide
6. **Document** conversion
7. **Push** to akapug
8. **Celebrate** 🎂

## Success Criteria

For each conversion:
- ✅ Runs with `elide script.ts`
- ✅ All tests pass
- ✅ Performance equal or better
- ✅ Documented conversion process
- ✅ ELIDE_CONVERSION.md file
- ✅ Pushed to akapug

## Next Steps

1. Find 20+ CLI/utility projects
2. Start with simplest (markdown parser, JSON tools)
3. Convert and test each
4. Document learnings
5. When HTTP lands: convert web apps

## Tools for Finding Projects

```bash
# GitHub searches
- "TypeScript CLI tool" stars:<100 (small projects)
- "JSON parser" language:TypeScript
- "markdown" language:TypeScript minimal dependencies
- "utility library" TypeScript simple
```

**HAPPY BIRTHDAY - LET'S CONVERT SOME PROJECTS!** 🎂🚀

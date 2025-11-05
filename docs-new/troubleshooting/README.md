# Troubleshooting Guide

Common issues and solutions for Elide beta10.

## Table of Contents

- [Installation Issues](#installation-issues)
- [Runtime Errors](#runtime-errors)
- [Module/Import Errors](#moduleimport-errors)
- [Node API Issues](#node-api-issues)
- [Build Failures](#build-failures)
- [Performance Issues](#performance-issues)
- [Known Beta10 Limitations](#known-beta10-limitations)

---

## Installation Issues

### "Command not found: elide"

**Problem:** Shell can't find the `elide` binary.

**Solutions:**

1. **Check if installed:**
   ```bash
   ls -la /usr/local/bin/elide
   ```

2. **Add to PATH:**
   ```bash
   export PATH="/usr/local/bin:$PATH"

   # Make permanent (bash)
   echo 'export PATH="/usr/local/bin:$PATH"' >> ~/.bashrc
   source ~/.bashrc

   # Make permanent (zsh)
   echo 'export PATH="/usr/local/bin:$PATH"' >> ~/.zshrc
   source ~/.zshrc
   ```

3. **Reinstall:**
   ```bash
   curl -sSL elide.sh | bash
   ```

### "Permission denied" when running elide

**Problem:** Binary not executable.

**Solution:**
```bash
sudo chmod +x /usr/local/bin/elide
```

### Wrong platform download

**Problem:** Downloaded arm64 binary on amd64 system (or vice versa).

**Check your platform:**
```bash
uname -m
# x86_64 = amd64
# aarch64 = arm64
# arm64 = arm64
```

**Download correct version:**
- Linux amd64: `elide-1.0.0-beta10-linux-amd64.tgz`
- Linux arm64: `elide-1.0.0-beta10-linux-aarch64.tgz`
- macOS arm64: `elide-1.0.0-beta10-darwin-aarch64.tgz`

---

## Runtime Errors

### TypeScript Syntax Error

**Error:**
```
SyntaxError: Expected ident but found error
```

**Causes:**

1. **Invalid TypeScript syntax:**
   ```typescript
   // ❌ Bad: JSX at command-line (limited support)
   const el = <div>Hello</div>;

   // ✅ Good: Use when imported
   import Component from "./component.tsx";
   ```

2. **Unsupported TypeScript features:**
   - Most TS 5.8 features work
   - JSX/TSX limited at CLI
   - Decorators may have issues

**Solutions:**

1. Validate your TypeScript:
   ```bash
   tsc --noEmit your-file.ts  # Check with tsc
   ```

2. Check Elide version:
   ```bash
   elide --version  # Should be 1.0.0-beta10 or later
   ```

3. Simplify to isolate issue:
   ```typescript
   // Start with minimal code
   console.log("Hello");
   ```

### "process.env is empty"

**Problem:** Environment variables not available (fixed in beta10).

**Check version:**
```bash
elide --version
```

**If beta9 or earlier:**
- Upgrade to beta10

**If beta10+:**
- This should work!
- Try:
  ```typescript
  console.log(process.env.HOME);
  console.log(process.env.PATH);
  ```

### Crashes with "Invalid Char code: -1"

**Error:**
```
java.lang.IllegalArgumentException: Invalid Char code: -1
```

**Cause:** Kinquirer terminal issue (interactive prompts).

**Solution:** Use non-interactive mode:
```bash
# ❌ Don't use interactive
elide init

# ✅ Use --yes flag
elide init my-app --yes
```

---

## Module/Import Errors

### "Module not found"

**Error:**
```
Error: Cannot find module 'express'
```

**Solutions:**

1. **Install dependencies first:**
   ```bash
   elide install
   ```

2. **Check node_modules exists:**
   ```bash
   ls -la node_modules/
   ```

3. **Check spelling:**
   ```typescript
   // ❌ Wrong
   import express from "expres";

   // ✅ Correct
   import express from "express";
   ```

4. **Try pnpm/npm:**
   ```bash
   pnpm install  # Or npm install
   elide run app.ts
   ```

### "Unsupported package exports"

**Error:**
```
TypeError: Unsupported package exports: 'package-name'
```

**Cause:** Package uses "exports" field (not supported in beta10).

**Example of problematic package.json:**
```json
{
  "exports": {
    ".": "./dist/index.js"
  }
}
```

**Solutions:**

1. **Use older package version** (without "exports"):
   ```bash
   # Check when exports was added
   npm view package-name time

   # Install older version
   elide add package-name@older-version
   ```

2. **Find alternative package:**
   - Look for packages without "exports" field
   - Check package's package.json before adding

3. **Manual workaround:**
   ```typescript
   // Instead of:
   import pkg from "problematic-package";

   // Try:
   import pkg from "problematic-package/dist/index.js";
   ```

### "EventEmitter is not a function" / "does not provide an export named 'EventEmitter'"

**Error:**
```
TypeError: The requested module 'events' does not provide
an export named 'EventEmitter'
```

**Cause:** Known beta10 issue - EventEmitter export broken.

**Impact:** Many npm packages fail (cac, commander, etc.).

**Workarounds:**

1. **Use packages that don't need EventEmitter**

2. **Try default import** (untested):
   ```typescript
   // Instead of:
   import { EventEmitter } from "node:events";

   // Try:
   import events from "node:events";
   const EventEmitter = events.EventEmitter;
   ```

3. **Use alternative packages:**
   - `yargs` instead of `cac`
   - `minimist` instead of `commander`

**Status:** Awaiting fix in future beta.

### "crypto.randomUUID is not a function"

**Error:**
```
TypeError: crypto.randomUUID is not a function
```

**Cause:** Using `node:crypto` instead of Web Crypto API.

**Solution:**

```typescript
// ❌ Don't use node:crypto
import crypto from "node:crypto";
crypto.randomUUID();  // Fails!

// ✅ Use Web Crypto API (global)
const uuid = crypto.randomUUID();  // Works!
```

**Working crypto methods:**
```typescript
// ✅ All of these work
crypto.randomUUID();
crypto.getRandomValues(new Uint8Array(16));
await crypto.subtle.digest("SHA-256", buffer);
```

---

## Node API Issues

### File system write operations fail

**Problem:** `fs.writeFileSync()` or `fs.writeFile()` have issues.

**Status:** Known limitation in beta10.

**Test in controlled environment first:**
```typescript
import * as fs from "node:fs";

try {
  fs.writeFileSync("/tmp/test.txt", "content");
  console.log("✅ Write works");
} catch (e) {
  console.error("❌ Write failed:", e);
}
```

**Workaround:** Use community packages or test thoroughly.

### `fs.readdirSync()` is not a function

**Error:**
```
TypeError: fs.readdirSync is not a function
```

**Cause:** Known limitation in beta10.

**Workaround:** Use alternatives:
```typescript
// Try fs/promises
import * as fsp from "node:fs/promises";
const files = await fsp.readdir("/path");
```

### Hash functions don't work

**Problem:** `crypto.createHash()` from `node:crypto` fails.

**Solution:** Use Web Crypto API:

```typescript
// ❌ Don't use node:crypto
import crypto from "node:crypto";
const hash = crypto.createHash("sha256");  // Fails

// ✅ Use Web Crypto
const buffer = new TextEncoder().encode("Hello");
const hashBuffer = await crypto.subtle.digest("SHA-256", buffer);
const hashArray = Array.from(new Uint8Array(hashBuffer));
const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
```

---

## Build Failures

### "elide install" fails

**Error:**
```
Orogene invocation failed with exit code 1
```

**Solutions:**

1. **Use pnpm/npm instead:**
   ```bash
   pnpm install  # Or npm install
   ```
   Elide can use their node_modules.

2. **Check network:**
   ```bash
   curl -I https://registry.npmjs.org
   ```

3. **Clear cache:**
   ```bash
   rm -rf ~/.elide/cache
   elide install
   ```

### "elide build" fails

**Common causes:**

1. **Missing dependencies:**
   ```bash
   elide install --frozen
   ```

2. **Invalid source files:**
   - Check TypeScript syntax
   - Check for circular dependencies

3. **Out of memory:**
   ```bash
   # Increase Java heap
   export JAVA_OPTS="-Xmx4g"
   elide build
   ```

### Native image build fails

**Error:**
```
Error: Native image build failed
```

**Common causes:**

1. **Reflection not configured:**
   - Some libraries need reflection config
   - Check GraalVM Native Image docs

2. **Dynamic features:**
   - Avoid reflection where possible
   - Avoid `eval()` and dynamic code

3. **Insufficient memory:**
   ```bash
   # Native image needs lots of RAM
   free -h

   # May need 8GB+ for large apps
   ```

**Debug:**
```bash
elide build --native --debug --verbose
```

---

## Performance Issues

### Slow cold start

**Problem:** First run takes longer than expected.

**Expected:**
- Cold start: ~20ms
- Hot start: ~2ms

**If much slower:**

1. **Check system resources:**
   ```bash
   top
   free -h
   ```

2. **Disable telemetry:**
   ```bash
   elide run --no-telemetry app.ts
   ```

3. **Use native build:**
   ```bash
   elide build --native
   ./app  # <1ms startup!
   ```

### High memory usage

**Problem:** Elide using too much RAM.

**Normal usage:**
- Small app: ~200MB
- Large app: ~500MB-1GB

**If much higher:**

1. **Check for memory leaks in your code**

2. **Reduce heap size:**
   ```bash
   export JAVA_OPTS="-Xmx512m"
   elide run app.ts
   ```

3. **Use native image:**
   ```bash
   elide build --native
   # Native uses 50-70% less memory
   ```

### Slow TypeScript parsing

**Problem:** TypeScript files take long to parse.

**Solutions:**

1. **Check file size:**
   ```bash
   wc -l your-file.ts
   # If >5000 lines, consider splitting
   ```

2. **Simplify types:**
   - Avoid extremely complex generic types
   - Reduce type computations

3. **Use caching:**
   - Elide caches parsed ASTs
   - Second run should be much faster

---

## Known Beta10 Limitations

### 1. Package.json "exports" Field ❌

**What doesn't work:**
```json
{
  "exports": {
    ".": "./dist/index.js"
  }
}
```

**Impact:** Many modern npm packages

**Status:** Expected fix in future beta

### 2. EventEmitter Export ❌

**What doesn't work:**
```typescript
import { EventEmitter } from "node:events";
```

**Impact:** Packages depending on EventEmitter fail

**Status:** Known issue, awaiting fix

### 3. Python Polyglot ⚠️

**What doesn't work:**
```typescript
// TypeScript → Python imports don't work
import * as utils from "./utils.py";
```

**Status:** Alpha feature, not ready in beta10

### 4. Interactive CLI Prompts ❌

**What doesn't work:**
```bash
elide init  # Crashes
```

**Workaround:** Always use `--yes`:
```bash
elide init my-app --yes
```

### 5. File System Writes ⚠️

**What's limited:**
```typescript
fs.writeFileSync();   // Issues
fs.readdirSync();     // Not a function
```

**What works:**
```typescript
fs.readFileSync();    // ✅ Works
fs.statSync();        // ✅ Works
```

### 6. Node:crypto Module ⚠️

**What doesn't work:**
```typescript
import crypto from "node:crypto";
crypto.randomUUID();  // Fails
crypto.createHash();  // Fails
```

**What works:**
```typescript
crypto.randomUUID();  // ✅ Global Web Crypto
```

### 7. Windows Support ❌

**Status:** Not available in beta10

**Platforms supported:**
- ✅ Linux (amd64, arm64)
- ✅ macOS (arm64 only)
- ❌ Windows (coming soon)
- ❌ macOS (amd64/Intel - coming soon)

### 8. Ruby Support ⚠️

**Status:** Experimental, largely untested

### 9. WebAssembly ⚠️

**Status:** Basic WASI support, untested in beta10

---

## Debugging Tips

### Enable Debug Mode

```bash
# Verbose logging
elide --debug --verbose run app.ts

# With profiling
elide --profile run app.ts

# Save debug log
elide --debug run app.ts 2>&1 | tee debug.log
```

### Check Versions

```bash
elide --version           # Elide version
elide info                # Full system info
java -version             # Java version (if applicable)
```

### Minimal Reproduction

When reporting issues, create minimal reproduction:

```typescript
// minimal-repro.ts
console.log("Step 1");
const problematic = require("problem-package");
console.log("Step 2");
```

```bash
elide minimal-repro.ts
```

### Check Source Code

When in doubt, check the source:

```bash
# Clone Elide repo
git clone https://github.com/elide-dev/elide
cd elide

# Find relevant code
find packages -name "*NodeFs*"
```

---

## Getting Help

### Before Asking

1. **Check this guide** ✅
2. **Search existing issues:** https://github.com/elide-dev/elide/issues
3. **Try latest beta:** `elide --version`
4. **Create minimal reproduction**

### Where to Ask

1. **Discord:** `elide discord` or https://elide.dev/discord
2. **GitHub Issues:** `elide bug` or https://github.com/elide-dev/elide/issues
3. **Discussions:** https://github.com/elide-dev/elide/discussions

### What to Include

1. **Elide version:** `elide --version`
2. **Platform:** `uname -a`
3. **Error message:** Full stack trace
4. **Minimal reproduction:** Smallest code that shows issue
5. **Steps to reproduce:** Exact commands run

**Good issue template:**
```
# Issue: Module 'express' not found

## Environment
- Elide: 1.0.0-beta10
- Platform: Linux amd64
- Node modules: Installed with pnpm

## Steps to reproduce
1. `pnpm install express`
2. Create app.ts with: import express from 'express'
3. Run: elide app.ts

## Expected
App runs

## Actual
Error: Cannot find module 'express'

## Code
[minimal reproduction code]
```

---

**Next**:
- [Node.js API Reference →](../api-reference/nodejs.md)
- [Migration from Node.js →](../migration/from-nodejs.md)
- [Examples →](../examples/README.md)

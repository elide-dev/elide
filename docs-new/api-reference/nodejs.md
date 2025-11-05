# Node.js API Compatibility

Elide implements a substantial portion of the Node.js API, allowing most Node applications to run with minimal or no changes.

> ✅ = Fully working | ⚠️ = Partially working | ❌ = Not implemented | 🧪 = Untested

## Quick Reference

| Module | Status | Notes |
|--------|--------|-------|
| `assert` | 🧪 | Untested |
| `buffer` | ✅ | **Full support** |
| `child_process` | 🧪 | Implementation exists |
| `cluster` | 🧪 | Untested |
| `console` | ✅ | Full support |
| `crypto` | ⚠️ | **Use Web Crypto API** |
| `dgram` | 🧪 | Untested |
| `diagnostics_channel` | 🧪 | Untested |
| `dns` | 🧪 | Untested |
| `domain` | 🧪 | Deprecated in Node, available |
| `events` | ❌ | **EventEmitter export broken** |
| `fs` | ⚠️ | **Reads work, writes limited** |
| `http` | 🧪 | Implementation exists |
| `http2` | 🧪 | Implementation exists |
| `https` | 🧪 | Implementation exists |
| `inspector` | 🧪 | Untested |
| `module` | 🧪 | Untested |
| `net` | 🧪 | Untested |
| `os` | ✅ | **Full support** |
| `path` | ✅ | **Full support** |
| `perf_hooks` | 🧪 | Untested |
| `process` | ✅ | **Full support** |
| `querystring` | ✅ | Added in beta8 |
| `readline` | 🧪 | Untested |
| `stream` | ⚠️ | Basic support |
| `string_decoder` | 🧪 | Implementation exists |
| `test` | 🧪 | Elide has own test runner |
| `timers` | ✅ | setTimeout, setInterval, etc. |
| `tls` | 🧪 | Untested |
| `tty` | 🧪 | Untested |
| `url` | ✅ | Full support |
| `util` | ✅ | Added in beta8 |
| `vm` | 🧪 | Untested |
| `worker_threads` | 🧪 | Untested |
| `zlib` | 🧪 | Implementation exists |

## Detailed API Coverage

### ✅ `buffer` - **FULLY WORKING**

All buffer operations tested and working:

```typescript
import { Buffer } from "node:buffer";

// Create buffers
const buf1 = Buffer.from("Hello");
const buf2 = Buffer.from([0x48, 0x65, 0x6c, 0x6c, 0x6f]);
const buf3 = Buffer.alloc(10);

// Convert to strings
buf1.toString("utf8");   // ✅ Works
buf1.toString("base64"); // ✅ Works
buf1.toString("hex");    // ✅ Works

// Properties
buf1.length;             // ✅ Works
buf1[0];                 // ✅ Works

// Methods
Buffer.isBuffer(buf1);   // ✅ Works
Buffer.concat([buf1, buf2]); // ✅ Works
```

**Tested encoding formats:**
- ✅ `utf8` / `utf-8`
- ✅ `base64`
- ✅ `hex`
- ✅ `ascii`
- ✅ `latin1` / `binary`

### ✅ `os` - **FULLY WORKING**

All tested functions work perfectly:

```typescript
import * as os from "node:os";

// System information
os.platform();    // ✅ "linux" or "darwin"
os.arch();        // ✅ "x64" or "arm64"
os.type();        // ✅ "Linux" or "Darwin"
os.release();     // ✅ Kernel version
os.version();     // ✅ OS version string

// Hardware info
os.cpus();        // ✅ Array of CPU info
os.totalmem();    // ✅ Total memory in bytes
os.freemem();     // ✅ Free memory in bytes

// Paths
os.homedir();     // ✅ Home directory
os.tmpdir();      // ✅ Temp directory
os.hostname();    // ✅ System hostname

// Users
os.userInfo();    // ✅ User information

// EOL
os.EOL;           // ✅ "\n" on Unix, "\r\n" on Windows
```

### ✅ `path` - **FULLY WORKING**

All path operations work correctly:

```typescript
import * as path from "node:path";

// Join paths
path.join("a", "b", "c.txt");        // ✅ "a/b/c.txt"

// Resolve paths
path.resolve("./file.txt");           // ✅ Absolute path

// Parse paths
path.dirname("/home/user/file.txt");  // ✅ "/home/user"
path.basename("/home/user/file.txt"); // ✅ "file.txt"
path.extname("/home/user/file.txt");  // ✅ ".txt"

// Normalize
path.normalize("a//b/../c");          // ✅ "a/c"

// Relative paths
path.relative("/a/b", "/a/c");        // ✅ "../c"

// Platform-specific
path.sep;                             // ✅ "/" on Unix
path.delimiter;                       // ✅ ":" on Unix
path.posix;                           // ✅ POSIX methods
path.win32;                           // ✅ Windows methods
```

### ✅ `process` - **FULLY WORKING**

Most process features work:

```typescript
// Environment variables (FIXED in beta10)
process.env.HOME;              // ✅ Works now!
process.env.PATH;              // ✅ Works

// Process info
process.pid;                   // ✅ Process ID
process.platform;              // ✅ "linux" or "darwin"
process.arch;                  // ✅ "x64" or "arm64"
process.version;               // ✅ Elide version
process.versions;              // ✅ Component versions

// Streams
process.stdout;                // ✅ Works
process.stderr;                // ✅ Works
process.stdin;                 // ✅ Works

// Working directory
process.cwd();                 // ✅ Current directory
process.chdir("/path");        // ⚠️ Untested

// Exit
process.exit(0);               // ✅ Works
process.exitCode = 1;          // ✅ Works

// Events
process.on("exit", () => {}); // 🧪 Untested
```

**⚠️ Known Issue (FIXED):**
- Before beta10: `process.env` was empty
- Beta10+: Works correctly!

### ⚠️ `crypto` - **PARTIALLY WORKING**

**🚨 Important:** Use Web Crypto API, not `node:crypto`!

```typescript
// ❌ BROKEN: node:crypto module
import crypto from "node:crypto";
crypto.randomUUID();  // ❌ TypeError: not a function

// ✅ WORKS: Web Crypto API (global)
const uuid = crypto.randomUUID();     // ✅ Works!
const bytes = crypto.getRandomValues(new Uint8Array(16)); // ✅ Works!
```

**What works:**
- ✅ `crypto.randomUUID()` (global, not from `node:crypto`)
- ✅ `crypto.getRandomValues()` (Web Crypto API)
- ✅ `crypto.subtle.digest()` (Web Crypto)

**What doesn't work:**
- ❌ Most `node:crypto` module functions
- ❌ `crypto.createHash()` from node:crypto
- ❌ `crypto.createCipher()` family

**Workaround:** Use Web Crypto API or community packages.

### ❌ `events` - **BROKEN IN BETA10**

Critical issue with EventEmitter:

```typescript
// ❌ BROKEN: Named export doesn't work
import { EventEmitter } from "node:events";
// TypeError: The requested module 'events' does not provide
// an export named 'EventEmitter'

// Possible workaround (untested):
import events from "node:events";
const EventEmitter = events.EventEmitter; // May work?
```

**Impact:**
- Many npm packages depend on EventEmitter
- Packages like `cac`, `commander` may fail
- Custom event emitters won't work

**Status:** Known issue, needs fix in future beta

### ⚠️ `fs` - **PARTIALLY WORKING**

Read operations work, write operations have issues:

```typescript
import * as fs from "node:fs";
import * as fsp from "node:fs/promises";

// ✅ WORKS: Reading files
fs.readFileSync("/path/to/file", "utf8");       // ✅ Synchronous read
await fsp.readFile("/path/to/file", "utf8");    // ✅ Async read

// ✅ WORKS: Checking files
fs.existsSync("/path/to/file");                 // ✅ Check existence
fs.statSync("/path/to/file");                   // ✅ Get file stats

// ⚠️ LIMITED: Directory operations
fs.readdirSync("/path");                        // ❌ Not a function (beta10)
await fsp.readdir("/path");                     // 🧪 Untested

// ❌ ISSUES: Writing files
fs.writeFileSync("/path", "content");           // ⚠️ Known issues
await fsp.writeFile("/path", "content");        // ⚠️ Known issues

// 🧪 UNTESTED: Other operations
fs.mkdirSync("/path");                          // 🧪 Untested
fs.unlinkSync("/path");                         // 🧪 Untested
fs.copyFileSync("/src", "/dst");                // 🧪 Untested
```

**What works:**
- ✅ `readFileSync()`, `readFile()`
- ✅ `existsSync()`, `exists()`
- ✅ `statSync()`, `stat()`

**What's broken:**
- ❌ `readdirSync()` - "not a function"
- ⚠️ Write operations have known issues

**What's untested:**
- 🧪 Most other fs operations

### ✅ `url` - **FULLY WORKING**

URL parsing and formatting works:

```typescript
import * as url from "node:url";
import { URL, URLSearchParams } from "node:url";

// Legacy API
url.parse("https://example.com/path?q=1"); // ✅ Works

// Modern API
const u = new URL("https://example.com/path?q=1");
u.protocol;     // ✅ "https:"
u.hostname;     // ✅ "example.com"
u.pathname;     // ✅ "/path"
u.searchParams; // ✅ URLSearchParams

// URLSearchParams
const params = new URLSearchParams("a=1&b=2");
params.get("a");        // ✅ "1"
params.has("b");        // ✅ true
params.toString();      // ✅ "a=1&b=2"
```

### ✅ `util` - **WORKING** (Added in Beta8)

Utility functions available:

```typescript
import * as util from "node:util";

// Formatting
util.format("%s %d", "test", 42);      // ✅ "test 42"

// Types
util.types.isDate(new Date());         // ✅ true

// Promisify (untested but implemented)
util.promisify(callback);              // 🧪 Untested

// Inspection
util.inspect(obj);                     // 🧪 Untested
```

### ✅ `querystring` - **WORKING** (Added in Beta8)

Query string parsing:

```typescript
import * as qs from "node:querystring";

// Parse
qs.parse("a=1&b=2");          // ✅ { a: "1", b: "2" }

// Stringify
qs.stringify({ a: 1, b: 2 }); // ✅ "a=1&b=2"

// Escape/unescape
qs.escape("hello world");     // ✅ "hello%20world"
qs.unescape("hello%20world"); // ✅ "hello world"
```

### 🧪 `http` / `https` - **UNTESTED**

HTTP modules exist in codebase but untested:

```typescript
import * as http from "node:http";
import * as https from "node:https";

// Server (untested)
const server = http.createServer((req, res) => {
  res.end("Hello");
});
server.listen(3000);

// Client (untested)
http.get("http://example.com", (res) => {
  // ...
});
```

**Status:** Implementation exists, needs testing

### 🧪 `child_process` - **UNTESTED**

Child process module exists:

```typescript
import * as cp from "node:child_process";

// Spawn (untested)
cp.spawn("ls", ["-la"]);

// Exec (untested)
cp.exec("ls -la", (err, stdout) => {
  // ...
});
```

**Status:** Implementation exists, needs testing

### 🧪 `stream` - **PARTIALLY IMPLEMENTED**

Stream consumers added in beta8:

```typescript
import * as stream from "node:stream";

// Stream consumers (beta8)
stream.consumers;  // ✅ Available

// Other stream features
stream.Readable;   // 🧪 Untested
stream.Writable;   // 🧪 Untested
stream.Transform;  // 🧪 Untested
```

### 🧪 `zlib` - **IMPLEMENTATION EXISTS**

Compression module exists but untested:

```typescript
import * as zlib from "node:zlib";

// Compression (untested)
zlib.gzip(buffer, (err, result) => {
  // ...
});

// Decompression (untested)
zlib.gunzip(buffer, (err, result) => {
  // ...
});
```

## Global Objects

### ✅ `console`

Standard console methods work:

```typescript
console.log("message");      // ✅ Works
console.error("error");      // ✅ Works
console.warn("warning");     // ✅ Works
console.info("info");        // ✅ Works
console.debug("debug");      // ✅ Works
console.trace();             // ✅ Works
console.table(data);         // 🧪 Untested
console.time("label");       // 🧪 Untested
console.timeEnd("label");    // 🧪 Untested
```

### ✅ `setTimeout` / `setInterval`

Timers work perfectly:

```typescript
setTimeout(() => {
  console.log("Delayed");
}, 1000);                    // ✅ Works

setInterval(() => {
  console.log("Repeating");
}, 1000);                    // ✅ Works

const timer = setTimeout(() => {}, 1000);
clearTimeout(timer);         // ✅ Works
```

### ✅ `fetch` - **GLOBAL FETCH API**

Web Fetch API available globally:

```typescript
// Fetch API
const response = await fetch("https://api.example.com/data");
const json = await response.json();

// Request/Response
const req = new Request("https://...");
const res = new Response("body");

// Headers
const headers = new Headers();
headers.set("Content-Type", "application/json");
```

**Status:** ✅ Full Web Fetch API support

### ✅ `crypto` - **GLOBAL WEB CRYPTO API**

Use global crypto, NOT `node:crypto`:

```typescript
// ✅ Works: Global crypto
crypto.randomUUID();
crypto.getRandomValues(new Uint8Array(16));

// Subtle Crypto
await crypto.subtle.digest("SHA-256", buffer);
```

## Missing APIs

These Node.js APIs are **not yet implemented**:

- `async_hooks` - No implementation
- `v8` - No implementation
- `wasi` - Different WASI implementation
- `repl` - Elide has own REPL

## Known Limitations (Beta10)

### 1. Package.json "exports" Field

Modern npm packages using "exports" **won't work**:

```json
// This package.json won't work:
{
  "exports": {
    ".": "./dist/index.js"
  }
}
```

**Impact:** Many modern packages fail to load

**Workaround:** Use older packages without "exports"

### 2. EventEmitter Export

`events.EventEmitter` named export is broken:

```typescript
// ❌ Doesn't work
import { EventEmitter } from "node:events";
```

**Impact:** Many packages won't work

**Workaround:** None currently, awaiting fix

### 3. File System Writes

Write operations have known issues:

```typescript
// ⚠️ May not work reliably
fs.writeFileSync("/path", "content");
```

**Workaround:** Tested in controlled environments first

### 4. Crypto Module

`node:crypto` module exports are incomplete:

```typescript
// ❌ Doesn't work
import crypto from "node:crypto";
crypto.createHash("sha256");
```

**Workaround:** Use Web Crypto API or community packages

## Migration Guide

### From Node.js

Most Node.js code works unchanged:

```typescript
// This Node.js code works in Elide:
import express from "express";
import * as path from "node:path";

const app = express();

app.get("/", (req, res) => {
  res.send("Hello from Elide!");
});

app.listen(3000);
```

**Common issues:**
1. Check if dependencies use "exports" field → May need older versions
2. EventEmitter usage → May fail, awaiting fix
3. Write-heavy fs operations → Test thoroughly

### Testing Compatibility

**Step-by-step:**
1. Try running directly: `elide app.ts`
2. Check for "exports" errors → Downgrade packages
3. Check for EventEmitter errors → Find alternatives
4. Test all critical paths → Especially file I/O

## Future Roadmap

Based on release notes, upcoming improvements:

- **Beta11+**: Expect "exports" field support
- **Beta11+**: EventEmitter fixes
- **Beta11+**: Complete crypto module
- **Beta11+**: More fs operations

## Reporting Issues

If you find Node API incompatibilities:

1. Check this reference first
2. Test on latest beta (`elide --version`)
3. Create minimal reproduction
4. Report: `elide bug` or [GitHub Issues](https://github.com/elide-dev/elide/issues)

---

**Next**:
- [JavaScript/TypeScript Guide →](../languages/javascript-typescript.md)
- [Migration from Node.js →](../migration/from-nodejs.md)
- [Troubleshooting →](../troubleshooting/README.md)

# Migrating from Node.js to Elide

Complete guide for migrating Node.js applications to Elide.

## Why Migrate?

### Performance Gains

| Metric | Node.js v20 | Elide beta10 | Improvement |
|--------|-------------|--------------|-------------|
| Cold Start | ~200ms | ~20ms | **10x faster** |
| Hot Request | ~7ms | ~2ms | **3.5x faster** |
| Memory | Baseline | -30-50% | **Lower** |

### Additional Benefits

- ✅ **TypeScript without build steps** - No more `tsc` or `ts-node`
- ✅ **Native compilation** - Single binary, <1ms startup
- ✅ **Polyglot** - Mix Python, Kotlin, Java in same project
- ✅ **Smaller deployments** - No node_modules in production
- ✅ **Better security** - Sandboxing and permission control

## Compatibility Overview

**Most Node.js code works unchanged!**

- ✅ 90%+ of typical Express/API apps work
- ✅ Most npm packages work (if no "exports" field)
- ⚠️ Some packages need adjustments
- ❌ Some modern packages blocked by "exports" field

## Migration Steps

### 1. Assessment Phase

**Check compatibility:**

1. **List your dependencies:**
   ```bash
   cat package.json | jq '.dependencies'
   ```

2. **Check for "exports" field:**
   ```bash
   # For each package
   cat node_modules/package-name/package.json | jq '.exports'
   ```

   If packages use "exports": ⚠️ May not work in beta10

3. **Check for EventEmitter usage:**
   ```bash
   grep -r "EventEmitter" src/
   grep -r "from 'events'" src/
   ```

   If found: ⚠️ May need workarounds

4. **Check crypto usage:**
   ```bash
   grep -r "crypto.createHash" src/
   grep -r "crypto.randomBytes" src/
   ```

   If found: ⚠️ May need to use Web Crypto API

### 2. Test Run

**Try running directly:**

```bash
# Install Elide
curl -sSL elide.sh | bash

# Try your entry point
elide src/index.ts

# Or with existing node_modules
pnpm install  # Or npm install
elide src/index.ts
```

**Common outcomes:**

- ✅ **It works!** - Great, proceed to optimization
- ⚠️ **Module errors** - Fix dependencies (see below)
- ❌ **Runtime errors** - Check compatibility issues

### 3. Fix Dependencies

#### Problem: "Unsupported package exports"

**Solution A: Use older version**

```bash
# Find when "exports" was added
npm view package-name time

# Use version before "exports"
pnpm add package-name@older-version
```

**Solution B: Find alternative**

```bash
# Search for alternatives
npm search alternative-to-package
```

**Example:**
```bash
# If chalk doesn't work (has exports)
# Try cli-color or kleur instead
pnpm add kleur
```

#### Problem: "EventEmitter" errors

**Solution: Use alternative packages**

| Breaks | Works |
|--------|-------|
| `cac` | `yargs` |
| `commander` | `minimist` |
| Custom EventEmitter | Alternative event libs |

**Example migration:**

```typescript
// ❌ Before (cac - uses EventEmitter)
import { cac } from "cac";
const cli = cac();

// ✅ After (yargs - works)
import yargs from "yargs";
const argv = yargs(process.argv.slice(2))
  .option('port', {
    type: 'number',
    default: 3000
  })
  .parse();
```

#### Problem: Crypto functions fail

**Solution: Use Web Crypto API**

```typescript
// ❌ Before (node:crypto)
import crypto from "crypto";
const uuid = crypto.randomUUID();
const hash = crypto.createHash("sha256")
  .update("data")
  .digest("hex");

// ✅ After (Web Crypto)
// UUID
const uuid = crypto.randomUUID();  // Global crypto

// Hash
const data = new TextEncoder().encode("data");
const hashBuffer = await crypto.subtle.digest("SHA-256", data);
const hashArray = Array.from(new Uint8Array(hashBuffer));
const hashHex = hashArray
  .map(b => b.toString(16).padStart(2, '0'))
  .join('');
```

### 4. Create Elide Project

**Initialize:**

```bash
elide init my-app empty --yes
cd my-app
```

**Migrate package.json to elide.pkl:**

```javascript
// Original package.json
{
  "name": "my-app",
  "dependencies": {
    "express": "^4.18.2",
    "lodash": "^4.17.21"
  },
  "scripts": {
    "dev": "ts-node src/index.ts",
    "build": "tsc",
    "start": "node dist/index.js"
  }
}
```

```pkl
// New elide.pkl
amends "elide:project.pkl"

name = "my-app"

dependencies {
  npm {
    packages {
      "express@4.18.2"
      "lodash@4.17.21"
    }
  }
}

scripts {
  "dev" = "elide src/index.ts"
  "build" = "elide build --native"
  "start" = "./my-app"
}
```

**Or keep package.json:**

Elide can use `package.json` directly! You don't have to convert.

```bash
# Works with existing package.json
npm install
elide src/index.ts
```

### 5. Remove Build Steps

**Before (Node.js with TypeScript):**

```json
{
  "scripts": {
    "dev": "ts-node src/index.ts",
    "build": "tsc",
    "start": "node dist/index.js"
  },
  "devDependencies": {
    "typescript": "^5.0.0",
    "ts-node": "^10.9.1",
    "@types/node": "^20.0.0"
  }
}
```

**After (Elide):**

```pkl
scripts {
  "dev" = "elide src/index.ts"
  "build" = "elide build --native"
  "start" = "./my-app"
}

// No need for:
// - typescript
// - ts-node
// - @types/* (unless for IDE)
// - tsconfig.json (optional)
```

**Benefits:**
- 📦 Smaller node_modules
- ⚡ Instant TypeScript execution
- 🚀 No build step in development

### 6. Test Thoroughly

**Create test matrix:**

```bash
# Test all endpoints
./test-endpoints.sh

# Test edge cases
elide test

# Load test
wrk -t12 -c400 -d30s http://localhost:3000/
```

**Check:**
- ✅ All routes work
- ✅ Database operations work
- ✅ File I/O works (especially writes)
- ✅ Authentication works
- ✅ External API calls work
- ✅ Error handling works

## Real-World Examples

### Example 1: Express API

**Original Node.js app:**

```typescript
// src/index.ts
import express from "express";
import { readFileSync } from "fs";

const app = express();

app.get("/api/config", (req, res) => {
  const config = JSON.parse(
    readFileSync("./config.json", "utf8")
  );
  res.json(config);
});

app.listen(3000, () => {
  console.log("Server running on :3000");
});
```

```bash
# Node.js workflow
npm install
npm run build  # tsc
npm start      # node dist/index.js
```

**Migrated to Elide:**

```typescript
// src/index.ts - SAME CODE!
import express from "express";
import { readFileSync } from "fs";

const app = express();

app.get("/api/config", (req, res) => {
  const config = JSON.parse(
    readFileSync("./config.json", "utf8")
  );
  res.json(config);
});

app.listen(3000, () => {
  console.log("Server running on :3000");
});
```

```bash
# Elide workflow
pnpm install   # Or elide install
elide src/index.ts  # Instant! No build!
```

**Result:**
- ✅ Code unchanged
- ✅ No build step
- ✅ 10x faster cold start
- ✅ 3x faster requests

### Example 2: CLI Tool

**Original:**

```typescript
// cli.ts
import { cac } from "cac";  // ❌ Uses EventEmitter

const cli = cac("my-tool");

cli
  .command("build", "Build the project")
  .action(() => {
    console.log("Building...");
  });

cli.parse();
```

**Migrated:**

```typescript
// cli.ts
import yargs from "yargs";  // ✅ Works in Elide

const argv = yargs(process.argv.slice(2))
  .command("build", "Build the project", () => {
    console.log("Building...");
  })
  .parse();
```

**Changes:**
- Replaced `cac` with `yargs`
- Otherwise identical functionality

### Example 3: REST API with Database

**Original:**

```typescript
// app.ts
import express from "express";
import { Pool } from "pg";

const pool = new Pool({
  host: process.env.DB_HOST,
  database: process.env.DB_NAME,
});

const app = express();

app.get("/users", async (req, res) => {
  const { rows } = await pool.query("SELECT * FROM users");
  res.json(rows);
});

app.listen(3000);
```

**Migration status:**

```typescript
// app.ts - NO CHANGES NEEDED!
import express from "express";
import { Pool } from "pg";

const pool = new Pool({
  host: process.env.DB_HOST,
  database: process.env.DB_NAME,
});

const app = express();

app.get("/users", async (req, res) => {
  const { rows } = await pool.query("SELECT * FROM users");
  res.json(rows);
});

app.listen(3000);
```

**Result:**
- ✅ Zero code changes
- ✅ `pg` package works
- ✅ `process.env` works (fixed in beta10)
- ✅ Async/await works
- ✅ Express middleware works

## Common Migration Patterns

### Pattern 1: TypeScript + Express

**Migration difficulty:** ⭐ Easy

**Typical changes:** None or minimal

**Checklist:**
- ✅ Express works
- ✅ Body parsers work
- ✅ Most middleware works
- ⚠️ Check if middleware uses EventEmitter

### Pattern 2: TypeScript + Database

**Migration difficulty:** ⭐ Easy

**Works:**
- ✅ `pg` (PostgreSQL)
- ✅ `mysql2`
- ✅ `sqlite3`
- ✅ `mongodb`
- ✅ Most ORMs (Prisma, TypeORM, etc.)

**Watch out for:**
- ⚠️ Packages with "exports" field
- ⚠️ Native modules (may need rebuilding)

### Pattern 3: Microservices

**Migration difficulty:** ⭐⭐ Medium

**Considerations:**
- ✅ HTTP clients work (fetch, axios)
- ✅ Most RPC libraries work
- ⚠️ gRPC may need testing
- ✅ Message queues work (amqplib, etc.)

**Deployment:**
```bash
# Build native binary per service
elide build --native

# Deploy binary (no Node runtime needed!)
./service
```

### Pattern 4: Monorepo

**Migration difficulty:** ⭐⭐⭐ Complex

**Approach:**
1. Migrate one service at a time
2. Keep Node.js for services that don't work yet
3. Gradually migrate as compatibility improves

**Mixed deployment:**
```
monorepo/
├── service-a/    # Migrated to Elide
├── service-b/    # Still on Node.js
└── shared/       # Shared TypeScript code
```

## Deployment Changes

### Container Images

**Before (Node.js):**

```dockerfile
FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm ci --production
COPY dist ./dist
CMD ["node", "dist/index.js"]
```

**After (Elide - interpreted):**

```dockerfile
FROM elide:latest
WORKDIR /app
COPY elide.pkl ./
RUN elide install --frozen
COPY src ./src
CMD ["elide", "src/index.ts"]
```

**After (Elide - native):**

```dockerfile
FROM scratch
COPY ./my-app /app
CMD ["/app"]
```

**Benefits:**
- 📦 Native image: FROM scratch (~10MB vs ~100MB)
- 🚀 Faster startup
- 🔒 More secure (less surface area)

### Serverless (AWS Lambda, etc.)

**Before:**
```javascript
// handler.js
exports.handler = async (event) => {
  // Cold start: ~200ms
  return { statusCode: 200, body: "OK" };
};
```

**After:**
```typescript
// handler.ts
export const handler = async (event: any) => {
  // Cold start: ~20ms (10x improvement!)
  return { statusCode: 200, body: "OK" };
};
```

```bash
# Build native for even faster cold starts
elide build --native
# Cold start: <5ms!
```

### Traditional VMs

**Before:**
```bash
# Install Node.js
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt-get install -y nodejs

# Deploy app
cd /app
npm ci --production
npm start
```

**After:**
```bash
# Just copy binary
scp ./my-app server:/app/
ssh server "/app/my-app"

# Or with Elide
curl -sSL elide.sh | bash
elide src/index.ts
```

## Performance Optimization

### 1. Native Image for Production

```bash
# Development: interpreted (fast iteration)
elide dev

# Production: native (fast startup)
elide build --native
```

**Benefits:**
- <1ms startup (vs ~200ms Node)
- 50-70% less memory
- No JIT warmup needed

### 2. Optimize Dependencies

```bash
# Before
npm install express body-parser cors helmet morgan

# After (use Web standards where possible)
# - Replace body-parser → express.json() (built-in)
# - Replace cors → custom middleware (smaller)
# - Keep helmet (security important)
```

### 3. Use Polyglot Features

```typescript
// Expensive computation in Python
// (Python can be faster for numeric work)
import { calculateFibonacci } from "./math.py";

app.get("/fib/:n", (req, res) => {
  const result = calculateFibonacci(Number(req.params.n));
  res.json({ result });
});
```

## Testing Strategy

### 1. Unit Tests

**Keep existing tests:**

```typescript
// tests/api.test.ts - NO CHANGES
import { expect } from "chai";
import request from "supertest";
import app from "../src/app";

describe("API Tests", () => {
  it("GET /api/users", async () => {
    const res = await request(app).get("/api/users");
    expect(res.status).to.equal(200);
  });
});
```

**Run with Elide:**
```bash
elide test
```

### 2. Integration Tests

**Add Elide-specific tests:**

```typescript
// tests/elide-compat.test.ts
describe("Elide Compatibility", () => {
  it("process.env works", () => {
    expect(process.env.HOME).to.be.a("string");
  });

  it("crypto.randomUUID works", () => {
    const uuid = crypto.randomUUID();
    expect(uuid).to.match(/^[0-9a-f-]{36}$/);
  });

  it("fetch works", async () => {
    const res = await fetch("https://httpbin.org/get");
    expect(res.ok).to.be.true;
  });
});
```

### 3. Performance Tests

**Benchmark cold start:**

```bash
# Node.js
time node dist/index.js --help
# real: 0.2s

# Elide (interpreted)
time elide src/index.ts --help
# real: 0.02s (10x faster!)

# Elide (native)
time ./my-app --help
# real: 0.001s (200x faster!)
```

**Benchmark throughput:**

```bash
# Start server
elide src/server.ts &

# Load test
wrk -t12 -c400 -d30s http://localhost:3000/

# Compare to Node.js
node dist/server.js &
wrk -t12 -c400 -d30s http://localhost:3000/
```

## Rollback Plan

**Always have a rollback plan:**

1. **Keep Node.js Dockerfile:**
   ```bash
   git checkout Dockerfile.node
   docker build -f Dockerfile.node .
   ```

2. **Feature flags:**
   ```typescript
   const runtime = process.env.RUNTIME || "node";

   if (runtime === "elide") {
     // Elide-specific code
   }
   ```

3. **Gradual rollout:**
   - 10% traffic to Elide
   - Monitor errors
   - Increase gradually

## Troubleshooting Migration

### Issue: Tests pass but production fails

**Cause:** Different behavior in production env

**Solution:**
1. Test with production data
2. Check environment variables
3. Enable debug logging:
   ```bash
   elide --debug --verbose src/index.ts
   ```

### Issue: Performance worse than Node.js

**Causes:**
1. Not using native compilation
2. Not warmed up (JIT needs time)
3. Inappropriate workload

**Solutions:**
1. Build native for production
2. Run load test to warm up JIT
3. Profile to find bottlenecks:
   ```bash
   elide --profile src/index.ts
   ```

### Issue: Mysterious crashes

**Debug:**
```bash
# Full debug output
elide --debug --verbose src/index.ts 2>&1 | tee debug.log

# Check for known issues
grep -i "EventEmitter\|exports" debug.log
```

## Success Stories

### REST API Migration

**Before (Node.js):**
- 200ms cold start
- 500MB Docker image
- 10 minutes build time

**After (Elide native):**
- <1ms cold start (**200x faster**)
- 15MB Docker image (**33x smaller**)
- 3 minutes build time (**3x faster**)

### GraphQL Server

**Before (Node.js):**
- 15-20ms query time
- 400MB memory

**After (Elide):**
- 5-8ms query time (**2.5x faster**)
- 250MB memory (**37% reduction**)

## Conclusion

**Migration difficulty by app type:**

| App Type | Difficulty | Expected Success Rate |
|----------|------------|----------------------|
| Simple APIs | ⭐ Easy | 95%+ |
| CRUD apps | ⭐ Easy | 90%+ |
| Microservices | ⭐⭐ Medium | 80%+ |
| Complex apps | ⭐⭐⭐ Hard | 60%+ |

**Most apps can migrate with minimal changes!**

**Next steps:**
1. Try your app: `elide src/index.ts`
2. Fix any compatibility issues
3. Test thoroughly
4. Deploy native binary

---

**Resources:**
- [Node.js API Reference](../api-reference/nodejs.md)
- [Troubleshooting Guide](../troubleshooting/README.md)
- [Examples](../examples/README.md)

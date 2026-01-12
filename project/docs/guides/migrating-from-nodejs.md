# Migrating from Node.js to Elide

This guide walks you through converting a Node.js project to Elide using the `elide adopt node` command.

## Table of Contents

- [Quick Start](#quick-start)
- [Basic Usage](#basic-usage)
- [Understanding the Output](#understanding-the-output)
- [Advanced Features](#advanced-features)
- [Monorepo/Workspace Support](#monorepowor kspace-support)
- [Common Scenarios](#common-scenarios)
- [Limitations](#limitations)
- [Next Steps](#next-steps)

## Quick Start

Convert your Node.js project to Elide in seconds:

```bash
# Navigate to your Node.js project
cd my-node-project

# Convert to Elide format
elide adopt node

# Review the generated elide.pkl
cat elide.pkl

# Build with Elide
elide build
```

That's it! The adopter automatically:
- Parses your `package.json`
- Converts dependencies to PKL format
- Handles workspaces/monorepos
- Documents NPM scripts as comments
- Preserves package metadata

## Basic Usage

### Convert a Single Package

```bash
# Use default package.json in current directory
elide adopt node

# Specify a specific package.json file
elide adopt node path/to/package.json

# Preview without writing (dry run)
elide adopt node --dry-run
```

### Command Options

```bash
elide adopt node [OPTIONS] [PACKAGE_JSON]

Options:
  --dry-run              Preview PKL output without writing to file
  --output, -o FILE      Write output to specific file (default: elide.pkl)
  --force, -f            Overwrite existing elide.pkl if it exists
  --skip-workspaces      For monorepos, only convert root package.json
  --help                 Show help message
```

## Understanding the Output

The adopter generates an `elide.pkl` file that mirrors your Node.js configuration.

### Example: Before and After

**Before (package.json):**
```json
{
  "name": "my-app",
  "version": "1.0.0",
  "description": "A sample Node.js application",
  "dependencies": {
    "express": "^4.18.2",
    "lodash": "^4.17.21"
  },
  "devDependencies": {
    "@types/node": "^20.10.0",
    "typescript": "^5.3.0",
    "vitest": "^1.0.0"
  },
  "scripts": {
    "start": "node dist/index.js",
    "build": "tsc",
    "test": "vitest"
  }
}
```

**After (elide.pkl):**
```pkl
amends "elide:project.pkl"

name = "my-app"
version = "1.0.0"
description = "A sample Node.js application"

dependencies {
  npm {
    packages {
      "express@^4.18.2"
      "lodash@^4.17.21"
    }
    devPackages {
      "@types/node@^20.10.0"
      "typescript@^5.3.0"
      "vitest@^1.0.0"
    }
  }
}

// NPM scripts detected (manual conversion may be needed):
//   start: node dist/index.js
//   build: tsc
//   test: vitest

sources {
  ["main"] = "src/**/*.ts"
  ["test"] = "test/**/*.ts"
}
```

### Output Structure

- **Project Metadata**: `name`, `version`, `description`
- **Dependencies**: Split into `packages` (dependencies) and `devPackages` (devDependencies)
- **NPM Scripts**: Listed in comments for manual conversion
- **Sources**: Standard Node.js directory layout

## Advanced Features

### Dependency Types

The adopter handles different dependency types from package.json:

| package.json field | Elide PKL Section | Description |
|--------------------|------------------|-------------|
| `dependencies` | `packages` | Production dependencies |
| `devDependencies` | `devPackages` | Development/test dependencies |
| `peerDependencies` | _(comment)_ | Peer dependencies (documented) |
| `optionalDependencies` | _(comment)_ | Optional dependencies (documented) |

**Example:**
```json
{
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.0",
    "vite": "^5.0.0"
  },
  "peerDependencies": {
    "react": "^18.0.0"
  },
  "optionalDependencies": {
    "fsevents": "^2.3.0"
  }
}
```

**Generated PKL:**
```pkl
dependencies {
  npm {
    packages {
      "react@^18.2.0"
      "react-dom@^18.2.0"
    }
    devPackages {
      "@types/react@^18.2.0"
      "vite@^5.0.0"
    }
  }
}

// Note: peerDependencies detected:
//   - react@^18.0.0

// Note: optionalDependencies detected:
//   - fsevents@^2.3.0
```

### Version Ranges

NPM version ranges are preserved as-is:

```json
{
  "dependencies": {
    "lodash": "^4.17.21",      // Caret range
    "express": "~4.18.0",       // Tilde range
    "react": ">=18.0.0 <19.0.0", // Range
    "typescript": "5.3.0",      // Exact version
    "axios": "*",               // Any version
    "debug": "latest"           // Latest tag
  }
}
```

**Generated PKL:**
```pkl
dependencies {
  npm {
    packages {
      "lodash@^4.17.21"
      "express@~4.18.0"
      "react@>=18.0.0 <19.0.0"
      "typescript@5.3.0"
      "axios@*"
      "debug@latest"
    }
  }
}
```

### NPM Scripts

NPM scripts are extracted and documented as comments:

```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview",
    "test": "vitest run",
    "test:watch": "vitest",
    "lint": "eslint src --ext ts,tsx",
    "format": "prettier --write src"
  }
}
```

**Generated PKL:**
```pkl
// NPM scripts detected (manual conversion may be needed):
//   dev: vite
//   build: tsc && vite build
//   preview: vite preview
//   test: vitest run
//   test:watch: vitest
//   lint: eslint src --ext ts,tsx
//   format: prettier --write src
```

You'll need to manually convert these to Elide task definitions if needed.

## Monorepo/Workspace Support

The adopter automatically detects and handles NPM/Yarn/PNPM workspaces.

### Example: Monorepo Structure

```
my-monorepo/
├── package.json          # Root package with workspaces
├── packages/
│   ├── web/
│   │   └── package.json
│   └── api/
│       └── package.json
└── shared/
    └── package.json
```

**Root package.json:**
```json
{
  "name": "my-monorepo",
  "version": "1.0.0",
  "private": true,
  "workspaces": [
    "packages/*",
    "shared"
  ]
}
```

### Running the Adopter

```bash
# Auto-detects workspace structure
elide adopt node

# Skip workspaces (root only)
elide adopt node --skip-workspaces
```

### Generated Output

**elide.pkl (root):**
```pkl
amends "elide:project.pkl"

name = "my-monorepo"
version = "1.0.0"

workspaces {
  "packages/web"
  "packages/api"
  "shared"
}

dependencies {
  npm {
    packages {
      "express@^4.18.2"
      "react@^18.2.0"
      "lodash@^4.17.21"
    }
    devPackages {
      "typescript@^5.3.0"
      "vitest@^1.0.0"
      "@types/node@^20.10.0"
    }
  }
}

// Note: This is a workspace/monorepo with 3 package(s).
// Dependencies are aggregated from all workspace packages.
// Workspace-local dependencies are excluded.
```

### Workspace Configuration Formats

The adopter supports both array and object workspace formats:

**Array format:**
```json
{
  "workspaces": [
    "packages/*",
    "apps/*"
  ]
}
```

**Object format (Yarn/PNPM):**
```json
{
  "workspaces": {
    "packages": [
      "packages/*",
      "apps/*"
    ]
  }
}
```

Both formats are handled automatically.

## Common Scenarios

### Scenario 1: React Application

**package.json:**
```json
{
  "name": "my-react-app",
  "version": "1.0.0",
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "typescript": "^5.3.0",
    "vite": "^5.0.0"
  }
}
```

**Convert:**
```bash
$ elide adopt node
📦 Parsing package.json...
  File: package.json
  Path: /Users/dev/my-react-app

✓ Parsed package.json successfully
  Package: my-react-app
  Version: 1.0.0

✓ Successfully generated elide.pkl!
  Output: /Users/dev/my-react-app/elide.pkl

Next steps:
  1. Review the generated elide.pkl file
  2. Adjust source mappings if needed
  3. Convert NPM scripts (currently documented as comments)
```

### Scenario 2: TypeScript Library

**package.json:**
```json
{
  "name": "@myorg/utils",
  "version": "2.1.0",
  "description": "Utility functions",
  "main": "dist/index.js",
  "types": "dist/index.d.ts",
  "dependencies": {
    "lodash": "^4.17.21"
  },
  "devDependencies": {
    "@types/lodash": "^4.14.200",
    "typescript": "^5.3.0",
    "vitest": "^1.0.0"
  },
  "peerDependencies": {
    "typescript": ">=4.5.0"
  }
}
```

**Generated PKL:**
```pkl
amends "elide:project.pkl"

name = "@myorg/utils"
version = "2.1.0"
description = "Utility functions"

dependencies {
  npm {
    packages {
      "lodash@^4.17.21"
    }
    devPackages {
      "@types/lodash@^4.14.200"
      "typescript@^5.3.0"
      "vitest@^1.0.0"
    }
  }
}

// Note: peerDependencies detected:
//   - typescript@>=4.5.0

sources {
  ["main"] = "src/**/*.ts"
  ["test"] = "test/**/*.ts"
}
```

### Scenario 3: Full-Stack Monorepo (React + API)

**Root package.json:**
```json
{
  "name": "fullstack-app",
  "private": true,
  "workspaces": [
    "web",
    "api"
  ]
}
```

**web/package.json:**
```json
{
  "name": "web",
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0"
  }
}
```

**api/package.json:**
```json
{
  "name": "api",
  "dependencies": {
    "express": "^4.18.2",
    "cors": "^2.8.5"
  }
}
```

**Convert:**
```bash
$ elide adopt node
📦 Parsing package.json...
  File: package.json
  Path: /Users/dev/fullstack-app

✓ Parsed package.json successfully
  Package: fullstack-app
  Workspaces: 2 package(s)

📁 Processing workspace packages...
✓ Parsed 2 workspace package(s)

✓ Successfully generated elide.pkl!
```

**Generated elide.pkl:**
```pkl
amends "elide:project.pkl"

name = "fullstack-app"

workspaces {
  "web"
  "api"
}

dependencies {
  npm {
    packages {
      "react@^18.2.0"
      "react-dom@^18.2.0"
      "express@^4.18.2"
      "cors@^2.8.5"
    }
  }
}
```

## Limitations

### Not Supported

1. **NPM Scripts**: Scripts are only documented as comments, not converted to Elide tasks
2. **Engines**: Node.js engine version requirements not converted
3. **Binary Fields**: `bin` field for CLI tools not supported
4. **Publish Configuration**: `publishConfig` not converted
5. **Custom Registry**: Non-standard NPM registries may need manual configuration

### Partially Supported

1. **Peer Dependencies**: Documented in comments but not enforced
2. **Optional Dependencies**: Documented in comments but not installed by default
3. **Bundled Dependencies**: Not distinguished from regular dependencies
4. **Overrides/Resolutions**: Yarn/PNPM dependency overrides not supported

### Known Issues

1. **Scoped Packages**: Scoped packages (e.g., `@types/node`) are supported but may need quotes in some contexts
2. **Git Dependencies**: Dependencies from git URLs may not work
3. **File/Link Dependencies**: Local `file:` and `link:` dependencies require manual configuration

## Troubleshooting

### Issue: "package.json file not found"

**Problem**: The adopter can't find package.json

**Solution**:
```bash
# Specify the path explicitly
elide adopt node path/to/package.json

# Or navigate to the directory first
cd path/to/project
elide adopt node
```

### Issue: "Failed to parse package.json file"

**Problem**: Invalid JSON syntax in package.json

**Solution**:
```bash
# Validate your package.json
cat package.json | json_pp

# Or use a JSON linter
npx jsonlint package.json
```

### Issue: Workspace packages not detected

**Problem**: Monorepo workspaces aren't being processed

**Solution**:
1. Ensure `workspaces` field is defined in root package.json
2. Check that workspace paths are correct
3. Verify each workspace has a package.json file

```bash
# Debug: Check what workspaces are defined
cat package.json | grep -A 5 "workspaces"

# See what the adopter finds
elide adopt node --dry-run
```

## Next Steps

After conversion:

1. **Review Generated PKL**: Check that all dependencies were captured correctly
   ```bash
   cat elide.pkl
   ```

2. **Adjust Source Mappings**: Update source paths if your project uses a non-standard structure
   ```pkl
   sources {
     ["main"] = "lib/**/*.js"     // Custom source directory
     ["test"] = "spec/**/*.test.js"
   }
   ```

3. **Convert NPM Scripts**: Manually create Elide tasks for important scripts
   ```pkl
   tasks {
     ["build"] {
       command = "tsc"
       description = "Compile TypeScript"
     }
     ["test"] {
       command = "vitest run"
       description = "Run tests"
     }
   }
   ```

4. **Test Build**: Verify the project builds with Elide
   ```bash
   elide build
   ```

5. **Commit**: Add the new `elide.pkl` to version control
   ```bash
   git add elide.pkl
   git commit -m "feat: adopt Elide build system"
   ```

6. **Optional: Keep package.json**: You can keep `package.json` for compatibility during transition
   ```bash
   # Use Elide for development
   elide build

   # Keep NPM for CI/CD during migration
   npm run build
   ```

## Examples

### Example 1: Simple Express API

**Before:**
```bash
$ ls
package.json  src/  test/

$ cat package.json
{
  "name": "my-api",
  "version": "1.0.0",
  "dependencies": {
    "express": "^4.18.2",
    "cors": "^2.8.5"
  },
  "devDependencies": {
    "@types/express": "^4.17.21",
    "typescript": "^5.3.0"
  }
}
```

**Convert:**
```bash
$ elide adopt node
📦 Parsing package.json...
  File: package.json
  Path: /Users/dev/my-api

✓ Parsed package.json successfully
  Package: my-api
  Version: 1.0.0

✓ Successfully generated elide.pkl!
  Output: /Users/dev/my-api/elide.pkl

Next steps:
  1. Review the generated elide.pkl file
  2. Adjust source mappings if needed
```

**After:**
```pkl
amends "elide:project.pkl"

name = "my-api"
version = "1.0.0"

dependencies {
  npm {
    packages {
      "express@^4.18.2"
      "cors@^2.8.5"
    }
    devPackages {
      "@types/express@^4.17.21"
      "typescript@^5.3.0"
    }
  }
}

sources {
  ["main"] = "src/**/*.ts"
  ["test"] = "test/**/*.ts"
}
```

### Example 2: React + Vite Application

**Before:**
```json
{
  "name": "vite-react-app",
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "@vitejs/plugin-react": "^4.2.0",
    "typescript": "^5.3.0",
    "vite": "^5.0.0"
  }
}
```

**Convert:**
```bash
$ elide adopt node
📦 Parsing package.json...

✓ Parsed package.json successfully
  Package: vite-react-app
  Version: 0.1.0

✓ Successfully generated elide.pkl!

Next steps:
  1. Review the generated elide.pkl file
  2. Adjust source mappings if needed
  3. Convert NPM scripts (currently documented as comments)
```

**After:**
```pkl
amends "elide:project.pkl"

name = "vite-react-app"
version = "0.1.0"

dependencies {
  npm {
    packages {
      "react@^18.2.0"
      "react-dom@^18.2.0"
    }
    devPackages {
      "@types/react@^18.2.0"
      "@types/react-dom@^18.2.0"
      "@vitejs/plugin-react@^4.2.0"
      "typescript@^5.3.0"
      "vite@^5.0.0"
    }
  }
}

// NPM scripts detected (manual conversion may be needed):
//   dev: vite
//   build: tsc && vite build
//   preview: vite preview

sources {
  ["main"] = "src/**/*.tsx"
  ["test"] = "src/**/*.test.tsx"
}
```

## Comparison: Node.js vs Elide

| Feature | Node.js (package.json) | Elide (elide.pkl) |
|---------|------------------------|-------------------|
| **Configuration Language** | JSON | PKL |
| **Dependencies** | `dependencies` | `packages { }` |
| **Dev Dependencies** | `devDependencies` | `devPackages { }` |
| **Workspaces** | `workspaces` | `workspaces { }` |
| **Scripts** | `scripts` | Manual task definition |
| **Version Ranges** | Semver (^, ~, etc.) | Preserved as-is |
| **Scoped Packages** | `@org/package` | Supported |

## Related Documentation

- [Migrating from Maven](./migrating-from-maven.md)
- [Migrating from Gradle](./migrating-from-gradle.md)
- [Migrating from Python](./migrating-from-python.md)
- [Troubleshooting Guide](./adopt-troubleshooting.md)
- [Elide PKL Reference](../elide-pkl-reference.md) _(coming soon)_
- [Elide CLI Guide](../cli-guide.md) _(coming soon)_

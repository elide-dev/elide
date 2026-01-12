# Elide Build Adopter Guides

Welcome to the Elide Build Adopter documentation. These guides help you migrate from other build systems to Elide.

## Migration Guides

### [Migrating from Maven](./migrating-from-maven.md)

Complete guide to converting Maven projects to Elide using `elide adopt maven`.

**Topics covered:**
- Quick start and basic usage
- Parent POM resolution
- Multi-module projects
- BOM (Bill of Materials) support
- Maven profiles
- Real-world examples (Spring Boot, Apache Commons)

**Best for:**
- Maven users looking to try Elide
- Projects with complex parent hierarchies
- Multi-module Maven projects
- Spring Boot applications

---

### [Migrating from Gradle](./migrating-from-gradle.md)

Complete guide to converting Gradle projects to Elide using `elide adopt gradle`.

**Topics covered:**
- Groovy and Kotlin DSL support
- Multi-project builds
- Dependency configuration mapping
- Repository detection
- Plugin handling
- Real-world examples (Kotlin apps, Spring Boot)

**Best for:**
- Gradle users looking to try Elide
- Kotlin/JVM projects
- Multi-project Gradle builds
- Android projects (with limitations)

---

### [Migrating from Bazel](./migrating-from-bazel.md) ⚠️ *Planned*

Design document for the planned Bazel adopter (`elide adopt bazel`).

**Topics covered:**
- BUILD and WORKSPACE parsing
- maven_install dependency extraction
- Multi-package workspaces
- Target type mapping (java_library, java_binary)
- Implementation strategy and roadmap

**Best for:**
- Understanding planned Bazel support
- Contributing to Bazel adopter implementation
- Evaluating if Elide is right for Bazel projects
- Providing feedback on the design

**Status:** Not yet implemented - this is a planning document.

---

### [Migrating from Node.js](./migrating-from-nodejs.md)

Complete guide to converting Node.js projects to Elide using `elide adopt node`.

**Topics covered:**
- package.json parsing
- Dependency type mapping (dependencies, devDependencies)
- NPM/Yarn/PNPM workspaces
- Monorepo support
- Version range handling
- Real-world examples (React, Express, TypeScript)

**Best for:**
- Node.js/NPM users looking to try Elide
- React and frontend projects
- Full-stack JavaScript/TypeScript applications
- NPM workspace monorepos

---

### [Migrating from Python](./migrating-from-python.md) ⚠️ *Planned*

Complete guide to converting Python projects to Elide using `elide adopt python`.

**Topics covered:**
- pyproject.toml (PEP 621) support
- requirements.txt parsing
- Pipfile support
- Virtual environment configuration
- Dependency extras and version specifiers
- Real-world examples (FastAPI, Django, Data Science)

**Best for:**
- Python users evaluating Elide
- FastAPI and Django projects
- Data science and ML pipelines
- React + Python polyglot applications

**Status:** Python adopter in development - this documents planned functionality.

---

### [Troubleshooting Guide](./adopt-troubleshooting.md)

Solutions to common issues when using build adopters.

**Topics covered:**
- Parent POM resolution failures
- Missing dependency versions
- Multi-module detection issues
- Plugin configuration problems
- Performance optimization
- Network and proxy issues

**Best for:**
- Debugging adoption failures
- Understanding error messages
- Finding workarounds for edge cases
- Reporting issues effectively

## Quick Reference

### Command Syntax

**Maven:**
```bash
elide adopt maven [OPTIONS] [POM_FILE]

# Common options
--dry-run              # Preview output
--output FILE          # Custom output file
--skip-modules         # Parent only (multi-module)
-P PROFILE             # Activate Maven profile
```

**Gradle:**
```bash
elide adopt gradle [OPTIONS] [BUILD_FILE]

# Common options
--dry-run              # Preview output
--output FILE          # Custom output file
--skip-subprojects     # Root only (multi-project)
```

**Node.js:**
```bash
elide adopt node [OPTIONS] [PACKAGE_JSON]

# Common options
--dry-run              # Preview output
--output FILE          # Custom output file
--skip-workspaces      # Root only (workspaces)
--force                # Overwrite existing file
```

**Python:**
```bash
elide adopt python [OPTIONS] [CONFIG_FILE]

# Common options
--dry-run              # Preview output
--output FILE          # Custom output file
--python-version VER   # Specify Python version

# ⚠️ In development
```

### Common Workflows

**1. Try before converting:**
```bash
# Preview the output
elide adopt maven --dry-run

# Review what would be generated
elide adopt gradle --dry-run | less
```

**2. Convert and validate:**
```bash
# Convert
elide adopt maven

# Verify PKL
cat elide.pkl

# Test build
elide build
```

**3. Multi-module projects:**
```bash
# Auto-detect and convert
elide adopt maven

# Or convert parent only
elide adopt maven --skip-modules
```

**4. Handle errors:**
```bash
# Check error output
elide adopt maven 2>&1 | tee adoption.log

# Consult troubleshooting guide
cat project/docs/guides/adopt-troubleshooting.md
```

## Feature Comparison

| Feature | Maven | Gradle | Bazel | Node.js | Python |
|---------|-------|--------|-------|---------|--------|
| **Parent POM Resolution** | ✅ Full support | N/A | N/A | N/A | N/A |
| **BOM Import** | ✅ Full support | Partial | N/A | N/A | N/A |
| **Multi-Module/Project** | ✅ Full support | ✅ Full support | 📋 Planned | ✅ Full support | 📋 Planned |
| **Property Interpolation** | ✅ Full support | Partial | 📋 Planned | N/A | N/A |
| **Profiles** | ✅ Full support | N/A | N/A | N/A | 📋 Planned |
| **Custom Repositories** | ✅ Full support | ✅ Full support | 📋 Planned | ✅ Default (npm) | 📋 Planned |
| **Plugin Detection** | ✅ Listed in comments | ✅ Listed in comments | 📋 Planned | N/A | N/A |
| **Remote Resolution** | ✅ Maven Central | N/A | N/A | N/A | N/A |
| **Version Catalogs** | N/A | ❌ Not yet | N/A | N/A | N/A |
| **Composite Builds** | N/A | ❌ Not yet | N/A | N/A | N/A |
| **Workspaces** | N/A | N/A | N/A | ✅ Full support | 📋 Planned |
| **Dev Dependencies** | N/A | N/A | N/A | ✅ Full support | 📋 Planned |
| **Scripts/Tasks** | N/A | N/A | N/A | ✅ Documented | 📋 Planned |
| **maven_install** | N/A | N/A | 📋 Planned | N/A | N/A |
| **Bazel Query API** | N/A | N/A | 📋 Planned | N/A | N/A |

## Examples by Project Type

### Spring Boot

**Maven:**
```bash
# Automatically resolves spring-boot-starter-parent
elide adopt maven
```

**Gradle:**
```bash
# Handles Spring Boot plugin and dependencies
elide adopt gradle
```

### Android

**Gradle only:**
```bash
# Extracts dependencies, but Android config needs manual work
elide adopt gradle
# See gradle guide for limitations
```

### Kotlin Multiplatform

**Gradle only:**
```bash
# JVM source set is extracted, other platforms may need manual work
elide adopt gradle
```

### Multi-Module Microservices

**Maven:**
```bash
# Aggregates all modules into single elide.pkl
elide adopt maven
```

**Gradle:**
```bash
# Detects multi-project via settings.gradle
elide adopt gradle
```

### React Application

**Node.js:**
```bash
# React projects with package.json
elide adopt node

# The adopter will:
# 1. Parse package.json
# 2. Convert React dependencies
# 3. Document NPM scripts
# 4. Handle workspaces if present
```

### Full-Stack (React + Python)

**Polyglot:**
```bash
my-fullstack-app/
├── web/
│   └── package.json     # React frontend
└── api/
    └── pyproject.toml   # Python backend

# Convert frontend
cd web
elide adopt node

# Convert backend (when available)
cd ../api
elide adopt python

# Result: Polyglot elide.pkl with both npm and pypi dependencies
```

## Known Limitations

### Maven
- Build plugins not converted (only documented)
- Dependency exclusions not supported
- `provided` and `system` scopes not fully supported

### Gradle
- Android configuration blocks not converted
- Kotlin Multiplatform limited support
- Version catalogs (libs.versions.toml) not supported
- Custom Gradle tasks not converted
- `compileOnly` dependencies listed in comments only

### Node.js
- NPM scripts not converted to tasks (only documented)
- Git/file/link dependencies need manual configuration
- Peer/optional dependencies documented but not enforced
- Engine requirements not converted

### Python
- ⚠️ **In Development**: Python adopter is currently being implemented
- Setup.py with complex logic not fully supported
- Conda-specific packages require manual configuration
- Platform-specific markers limited support

## Getting Help

1. **Check the troubleshooting guide** - Most common issues are documented
2. **Review examples** - Each guide has multiple real-world examples
3. **Use --dry-run** - Preview before making changes
4. **Keep original build file** - You can use both systems during transition

## Related Documentation

- [Elide PKL Reference](../elide-pkl-reference.md) _(coming soon)_
- [Elide CLI Guide](../cli-guide.md) _(coming soon)_
- [Build Configuration](../build-configuration.md) _(coming soon)_

## Contributing

Found an issue or have a suggestion? Please file an issue with:
- Your Elide version (`elide --version`)
- Command used
- Error output
- Minimal reproduction (pom.xml or build.gradle)

---

**Last updated:** November 2024
**Elide Version:** 1.0.0-alpha

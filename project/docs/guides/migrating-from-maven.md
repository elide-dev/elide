# Migrating from Maven to Elide

This guide walks you through converting a Maven project to Elide using the `elide adopt maven` command.

## Table of Contents

- [Quick Start](#quick-start)
- [Basic Usage](#basic-usage)
- [Understanding the Output](#understanding-the-output)
- [Advanced Features](#advanced-features)
- [Multi-Module Projects](#multi-module-projects)
- [Common Scenarios](#common-scenarios)
- [Limitations](#limitations)
- [Next Steps](#next-steps)

## Quick Start

Convert your Maven project to Elide in seconds:

```bash
# Navigate to your Maven project
cd my-maven-project

# Convert to Elide format
elide adopt maven

# Review the generated elide.pkl
cat elide.pkl

# Build with Elide
elide build
```

That's it! The adopter automatically:
- Parses your `pom.xml`
- Resolves parent POMs and dependency management
- Converts dependencies to PKL format
- Handles multi-module projects
- Downloads remote parent POMs if needed

## Basic Usage

### Convert a Single-Module Project

```bash
# Use default pom.xml in current directory
elide adopt maven

# Specify a specific POM file
elide adopt maven path/to/pom.xml

# Preview without writing (dry run)
elide adopt maven --dry-run
```

### Command Options

```bash
elide adopt maven [OPTIONS] [POM_FILE]

Options:
  --dry-run              Preview PKL output without writing to file
  --output, -o FILE      Write output to specific file (default: elide.pkl)
  --skip-modules         For multi-module projects, only convert parent
  --activate-profile, -P PROFILE
                         Activate Maven profile(s) during conversion
  --help                 Show help message
```

## Understanding the Output

The adopter generates an `elide.pkl` file that mirrors your Maven configuration.

### Example: Before and After

**Before (pom.xml):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.example</groupId>
  <artifactId>my-app</artifactId>
  <version>1.0.0</version>
  <name>My Application</name>
  <description>A sample application</description>

  <dependencies>
    <dependency>
      <groupId>com.google.guava</groupId>
      <artifactId>guava</artifactId>
      <version>32.1.3-jre</version>
    </dependency>

    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>5.10.1</version>
      <scope>test</scope>
    </dependency>
  </dependencies>
</project>
```

**After (elide.pkl):**
```pkl
amends "elide:project.pkl"

name = "my-app"
description = "A sample application"

dependencies {
  maven {
    repositories {
      ["central"] = "https://repo.maven.apache.org/maven2"  // Maven Central (Super POM default)
    }
    packages {
      "com.google.guava:guava:32.1.3-jre"
    }
    testPackages {
      "org.junit.jupiter:junit-jupiter:5.10.1"
    }
  }
}

sources {
  ["main"] = "src/main/java/**/*.java"
  ["test"] = "src/test/java/**/*.java"
}
```

### Output Structure

- **Project Metadata**: `name`, `description`, `version`
- **Dependencies**: Split into `packages` (compile/runtime) and `testPackages` (test scope)
- **Repositories**: Maven Central is included by default, plus any custom repositories
- **Sources**: Standard Maven directory layout

## Advanced Features

### Parent POM Resolution

The adopter automatically resolves parent POMs through multiple strategies:

1. **Filesystem**: Looks for parent POM using `<relativePath>` (default: `../pom.xml`)
2. **Local Maven Repository**: Checks `~/.m2/repository`
3. **Maven Central**: Downloads parent POM if not found locally

**Example with Parent POM:**

```xml
<!-- pom.xml -->
<parent>
  <groupId>org.apache.commons</groupId>
  <artifactId>commons-parent</artifactId>
  <version>52</version>
</parent>

<artifactId>commons-lang3</artifactId>
<version>3.14.0</version>

<dependencies>
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <!-- Version inherited from parent's dependencyManagement -->
  </dependency>
</dependencies>
```

The adopter will:
1. Download `commons-parent:52` from Maven Central
2. Extract `dependencyManagement` versions
3. Resolve JUnit version from parent
4. Inherit properties and repositories

### Dependency Management

The adopter handles `<dependencyManagement>` sections automatically:

```xml
<!-- Parent POM -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>5.10.1</version>
    </dependency>
  </dependencies>
</dependencyManagement>

<!-- Child POM -->
<dependencies>
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <!-- Version comes from parent's dependencyManagement -->
    <scope>test</scope>
  </dependency>
</dependencies>
```

**Generated PKL:**
```pkl
dependencies {
  maven {
    testPackages {
      "org.junit.jupiter:junit-jupiter:5.10.1"  // Version resolved from parent
    }
  }
}
```

### BOM (Bill of Materials) Support

The adopter resolves BOMs with `<scope>import</scope>`:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>3.2.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <!-- Version comes from imported BOM -->
  </dependency>
</dependencies>
```

The adopter will:
1. Download the Spring Boot BOM from Maven Central
2. Parse all managed versions
3. Resolve `spring-boot-starter-web` version from BOM

### Property Interpolation

The adopter supports Maven property interpolation:

```xml
<properties>
  <guava.version>32.1.3-jre</guava.version>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencies>
  <dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>${guava.version}</version>
  </dependency>
</dependencies>
```

**Generated PKL:**
```pkl
dependencies {
  maven {
    packages {
      "com.google.guava:guava:32.1.3-jre"  // Property interpolated
    }
  }
}
```

### Custom Repositories

Custom Maven repositories are preserved:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <name>JitPack Repository</name>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```

**Generated PKL:**
```pkl
dependencies {
  maven {
    repositories {
      ["central"] = "https://repo.maven.apache.org/maven2"  // Maven Central (Super POM default)
      ["jitpack.io"] = "https://jitpack.io"  // JitPack Repository
    }
  }
}
```

### Maven Profiles

Activate specific profiles during conversion:

```bash
elide adopt maven --activate-profile production
elide adopt maven -P production,docker
```

**Example profile:**
```xml
<profiles>
  <profile>
    <id>production</id>
    <properties>
      <environment>prod</environment>
    </properties>
    <dependencies>
      <dependency>
        <groupId>com.example</groupId>
        <artifactId>prod-logging</artifactId>
        <version>1.0</version>
      </dependency>
    </dependencies>
  </profile>
</profiles>
```

## Multi-Module Projects

The adopter automatically detects multi-module projects and generates a single root `elide.pkl` with workspace configuration.

### Example: Multi-Module Structure

```
my-project/
├── pom.xml              # Parent/aggregator POM
├── module-a/
│   └── pom.xml
└── module-b/
    └── pom.xml
```

**Parent pom.xml:**
```xml
<packaging>pom</packaging>
<modules>
  <module>module-a</module>
  <module>module-b</module>
</modules>
```

### Running the Adopter

```bash
# Auto-detects multi-module structure
elide adopt maven

# Skip child modules (parent only)
elide adopt maven --skip-modules
```

### Generated Output

**elide.pkl (root):**
```pkl
amends "elide:project.pkl"

name = "my-project"

workspaces {
  "module-a"
  "module-b"
}

dependencies {
  maven {
    repositories {
      ["central"] = "https://repo.maven.apache.org/maven2"
    }
    packages {
      "com.google.guava:guava:32.1.3-jre"
      "org.apache.commons:commons-lang3:3.14.0"
    }
    testPackages {
      "org.junit.jupiter:junit-jupiter:5.10.1"
    }
  }
}

// Note: This is a multi-module Maven project with 2 module(s).
// Dependencies are aggregated from all modules. Inter-module dependencies are excluded.
```

### Inter-Module Dependencies

The adopter automatically filters out inter-module dependencies to avoid duplication:

```xml
<!-- module-b/pom.xml -->
<dependencies>
  <dependency>
    <groupId>com.example</groupId>
    <artifactId>module-a</artifactId>  <!-- Internal dependency -->
    <version>${project.version}</version>
  </dependency>
  <dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>  <!-- External dependency - included -->
    <version>32.1.3-jre</version>
  </dependency>
</dependencies>
```

Only external dependencies appear in the generated PKL.

## Common Scenarios

### Scenario 1: Spring Boot Application

```bash
# Spring Boot projects work out of the box
elide adopt maven

# The adopter will:
# 1. Resolve spring-boot-starter-parent
# 2. Download spring-boot-dependencies BOM
# 3. Resolve all Spring dependency versions
# 4. Preserve Spring repositories if any
```

### Scenario 2: Apache Commons Library

```bash
# Apache projects with commons-parent
elide adopt maven

# The adopter will:
# 1. Download commons-parent from Maven Central
# 2. Inherit dependency management
# 3. Resolve versions like JUnit from parent
```

### Scenario 3: Multi-Module Microservices

```bash
my-microservices/
├── pom.xml                 # Parent
├── common-lib/
│   └── pom.xml
├── auth-service/
│   └── pom.xml
└── api-gateway/
    └── pom.xml

# Convert the entire project
cd my-microservices
elide adopt maven

# Result: Single elide.pkl with all external dependencies
```

## Build Plugin Warnings

Maven build plugins have no direct Elide equivalent. The adopter will list them in comments:

```pkl
// Build plugins detected (manual conversion may be needed):
//   - org.apache.maven.plugins:maven-compiler-plugin:3.11.0
//   - org.apache.maven.plugins:maven-surefire-plugin:3.0.0
//   - org.springframework.boot:spring-boot-maven-plugin:3.2.0
```

You'll need to manually configure these features in Elide if needed.

## Limitations

### Not Supported

1. **Build Plugins**: Maven plugins (compiler, surefire, etc.) are not converted - only listed as comments
2. **Exclusions**: Dependency exclusions (`<exclusions>`) are not yet supported
3. **Classifiers**: Dependencies with classifiers are not fully supported
4. **Complex Profile Activation**: Only explicit profile activation via `-P` flag

### Partially Supported

1. **Properties**: Most property interpolation works, except:
   - Maven built-in properties like `${maven.build.timestamp}`
   - Expression evaluation (e.g., `${property.exists ? 'a' : 'b'}`)

2. **Scopes**: `compile`, `runtime`, and `test` scopes are supported. Others (`provided`, `system`) may not convert correctly.

## Troubleshooting

See the [Troubleshooting Guide](./adopt-troubleshooting.md) for common issues and solutions.

## Next Steps

After conversion:

1. **Review Generated PKL**: Check that all dependencies were captured correctly
   ```bash
   cat elide.pkl
   ```

2. **Test Build**: Verify the project builds with Elide
   ```bash
   elide build
   ```

3. **Manual Adjustments**: Add any custom build configuration that couldn't be auto-converted

4. **Commit**: Add the new `elide.pkl` to version control
   ```bash
   git add elide.pkl
   git commit -m "feat: adopt Elide build system"
   ```

5. **Optional: Keep Maven**: You can keep `pom.xml` for compatibility during transition
   ```bash
   # Use Elide for development
   elide build

   # Keep Maven for CI/CD during migration
   mvn clean install
   ```

## Examples

### Example 1: Simple Java Library

**Before:**
```bash
$ ls
pom.xml  src/

$ cat pom.xml
<?xml version="1.0"?>
<project>
  <groupId>com.example</groupId>
  <artifactId>math-utils</artifactId>
  <version>1.0.0</version>

  <dependencies>
    <dependency>
      <groupId>org.apache.commons</groupId>
      <artifactId>commons-math3</artifactId>
      <version>3.6.1</version>
    </dependency>
  </dependencies>
</project>
```

**Convert:**
```bash
$ elide adopt maven
📋 Parsing Maven POM...
  File: pom.xml
  Path: /Users/dev/math-utils

✓ Project parsed successfully
  Name: math-utils
  Coordinates: com.example:math-utils:1.0.0
  Description: (none)

📦 Dependencies
  Compile: 1
  Test: 0

📚 Repositories
  central (Maven Central)

✓ Generated elide.pkl (18 lines)

💡 Next steps:
  1. Review the generated elide.pkl file
  2. Run elide build to build your project
```

**After:**
```bash
$ cat elide.pkl
amends "elide:project.pkl"

name = "math-utils"

dependencies {
  maven {
    repositories {
      ["central"] = "https://repo.maven.apache.org/maven2"
    }
    packages {
      "org.apache.commons:commons-math3:3.6.1"
    }
  }
}

sources {
  ["main"] = "src/main/java/**/*.java"
  ["test"] = "src/test/java/**/*.java"
}
```

### Example 2: Spring Boot Web App

**Before:**
```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.2.0</version>
</parent>

<artifactId>my-web-app</artifactId>

<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

**Convert:**
```bash
$ elide adopt maven
📋 Parsing Maven POM...
🔍 Resolving parent POM: org.springframework.boot:spring-boot-starter-parent:3.2.0
  ✓ Resolved from Maven Central

✓ Project parsed successfully
  Parent: spring-boot-starter-parent:3.2.0

📦 Dependencies
  Compile: 1 (versions resolved from parent)
  Test: 1

✓ Generated elide.pkl
```

**After:**
```pkl
amends "elide:project.pkl"

name = "my-web-app"

dependencies {
  maven {
    repositories {
      ["central"] = "https://repo.maven.apache.org/maven2"
    }
    packages {
      "org.springframework.boot:spring-boot-starter-web:3.2.0"
    }
    testPackages {
      "org.springframework.boot:spring-boot-starter-test:3.2.0"
    }
  }
}

sources {
  ["main"] = "src/main/java/**/*.java"
  ["test"] = "src/test/java/**/*.java"
}
```

## Related Documentation

- [Migrating from Gradle](./migrating-from-gradle.md)
- [Troubleshooting Guide](./adopt-troubleshooting.md)
- [Elide PKL Reference](../elide-pkl-reference.md) _(coming soon)_
- [Elide CLI Guide](../cli-guide.md) _(coming soon)_

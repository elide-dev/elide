# Migrating from Gradle to Elide

This guide walks you through converting a Gradle project to Elide using the `elide adopt gradle` command.

## Table of Contents

- [Quick Start](#quick-start)
- [Basic Usage](#basic-usage)
- [Understanding the Output](#understanding-the-output)
- [Advanced Features](#advanced-features)
- [Multi-Project Builds](#multi-project-builds)
- [Common Scenarios](#common-scenarios)
- [Limitations](#limitations)
- [Next Steps](#next-steps)

## Quick Start

Convert your Gradle project to Elide in seconds:

```bash
# Navigate to your Gradle project
cd my-gradle-project

# Convert to Elide format
elide adopt gradle

# Review the generated elide.pkl
cat elide.pkl

# Build with Elide
elide build
```

The adopter automatically:
- Parses both Groovy DSL (`build.gradle`) and Kotlin DSL (`build.gradle.kts`)
- Extracts dependencies and their configurations
- Detects multi-project builds via `settings.gradle[.kts]`
- Maps Gradle dependency configurations to Elide equivalents
- Preserves repository declarations

## Basic Usage

### Convert a Single-Project Build

```bash
# Auto-detect build file in current directory
elide adopt gradle

# Specify a specific build file
elide adopt gradle build.gradle.kts

# Preview without writing (dry run)
elide adopt gradle --dry-run
```

### Command Options

```bash
elide adopt gradle [OPTIONS] [BUILD_FILE]

Options:
  --dry-run              Preview PKL output without writing to file
  --output, -o FILE      Write output to specific file (default: elide.pkl)
  --skip-subprojects     For multi-project builds, only convert root
  --help                 Show help message
```

## Understanding the Output

The adopter generates an `elide.pkl` file that mirrors your Gradle configuration.

### Example: Before and After

**Before (build.gradle.kts):**
```kotlin
plugins {
    id("java")
    id("application")
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("com.google.guava:guava:32.1.3-jre")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.mockito:mockito-core:5.7.0")
}
```

**After (elide.pkl):**
```pkl
amends "elide:project.pkl"

name = "my-app"
version = "1.0.0"

dependencies {
  maven {
    repositories {
      ["central"] = "https://repo.maven.apache.org/maven2"
      ["google"] = "https://maven.google.com"
    }
    packages {
      "com.google.guava:guava:32.1.3-jre"
      "com.squareup.okhttp3:okhttp:4.12.0"
    }
    testPackages {
      "org.junit.jupiter:junit-jupiter:5.10.1"
      "org.mockito:mockito-core:5.7.0"
    }
  }
}

// Build plugins detected (manual conversion may be needed):
//   - java
//   - application

sources {
  ["main"] = "src/main/java/**/*.java"
  ["test"] = "src/test/java/**/*.java"
}
```

### Output Structure

- **Project Metadata**: `name`, `group`, `version`
- **Dependencies**: Mapped from Gradle configurations to PKL sections
- **Repositories**: Common repositories (mavenCentral, google) are recognized
- **Plugins**: Listed in comments for manual review
- **Sources**: Standard Gradle source set layout

## Advanced Features

### Kotlin DSL Support

The adopter fully supports Kotlin DSL (`.kts`) files:

**build.gradle.kts:**
```kotlin
plugins {
    kotlin("jvm") version "1.9.21"
}

val ktorVersion = "2.3.6"

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    testImplementation(kotlin("test"))
}
```

**Generated PKL:**
```pkl
dependencies {
  maven {
    packages {
      "io.ktor:ktor-server-core:2.3.6"
      "io.ktor:ktor-server-netty:2.3.6"
    }
    testPackages {
      "org.jetbrains.kotlin:kotlin-test:1.9.21"
    }
  }
}

// Build plugins detected (manual conversion may be needed):
//   - org.jetbrains.kotlin.jvm:1.9.21
```

### Dependency Configuration Mapping

Gradle dependency configurations are mapped to Elide equivalents:

| Gradle Configuration | Elide PKL Section | Description |
|---------------------|------------------|-------------|
| `implementation` | `packages` | Compile + runtime dependencies |
| `api` | `packages` | Public API dependencies |
| `compileOnly` | _(comment)_ | Compile-time only (not yet supported) |
| `runtimeOnly` | `packages` | Runtime dependencies |
| `testImplementation` | `testPackages` | Test dependencies |
| `testCompileOnly` | _(comment)_ | Test compile-only |
| `testRuntimeOnly` | `testPackages` | Test runtime dependencies |

**Example:**
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.google.guava:guava:32.1.3-jre")
    api("org.slf4j:slf4j-api:2.0.9")
    compileOnly("org.projectlombok:lombok:1.18.30")
    runtimeOnly("com.h2database:h2:2.2.224")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")
}
```

**Generated PKL:**
```pkl
dependencies {
  maven {
    packages {
      "com.google.guava:guava:32.1.3-jre"
      "org.slf4j:slf4j-api:2.0.9"
      "com.h2database:h2:2.2.224"
    }
    testPackages {
      "org.junit.jupiter:junit-jupiter:5.10.1"
      "org.junit.platform:junit-platform-launcher:1.10.1"
    }
  }
}

// Note: compileOnly dependencies detected:
//   - org.projectlombok:lombok:1.18.30
```

### Repository Detection

Common Gradle repositories are automatically detected:

```kotlin
repositories {
    mavenCentral()
    google()
    maven("https://jitpack.io")
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/owner/repo")
    }
}
```

**Generated PKL:**
```pkl
dependencies {
  maven {
    repositories {
      ["central"] = "https://repo.maven.apache.org/maven2"
      ["google"] = "https://maven.google.com"
      ["maven"] = "https://jitpack.io"
      ["GitHubPackages"] = "https://maven.pkg.github.com/owner/repo"
    }
  }
}
```

### Plugin Detection

Gradle plugins are detected and listed for manual review:

```kotlin
plugins {
    java
    `java-library`
    application
    id("org.springframework.boot") version "3.2.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
```

**Generated PKL:**
```pkl
// Build plugins detected (manual conversion may be needed):
//   - java
//   - java-library
//   - application
//   - org.springframework.boot:3.2.0
//   - com.github.johnrengelman.shadow:8.1.1
```

## Multi-Project Builds

The adopter automatically detects multi-project builds via `settings.gradle[.kts]`.

### Example: Multi-Project Structure

```
my-project/
├── settings.gradle.kts
├── build.gradle.kts       # Root build file
├── module-a/
│   └── build.gradle.kts
└── module-b/
    └── build.gradle.kts
```

**settings.gradle.kts:**
```kotlin
rootProject.name = "my-project"

include("module-a", "module-b")
```

### Running the Adopter

```bash
# Auto-detects multi-project structure
elide adopt gradle

# Skip subprojects (root only)
elide adopt gradle --skip-subprojects
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
      "com.squareup.okhttp3:okhttp:4.12.0"
    }
    testPackages {
      "org.junit.jupiter:junit-jupiter:5.10.1"
    }
  }
}

// Build plugins detected (manual conversion may be needed):
//   - java
//   - org.jetbrains.kotlin.jvm:1.9.21
```

### Project Dependencies

Dependencies between subprojects are handled automatically:

**build.gradle.kts (module-b):**
```kotlin
dependencies {
    implementation(project(":module-a"))  // Internal dependency
    implementation("com.google.guava:guava:32.1.3-jre")  // External dependency
}
```

The generated PKL only includes external dependencies - project dependencies are handled by the workspace structure.

## Common Scenarios

### Scenario 1: Spring Boot Application

**build.gradle.kts:**
```kotlin
plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.spring") version "1.9.21"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
```

**Generated PKL:**
```pkl
name = "my-spring-app"

dependencies {
  maven {
    repositories {
      ["central"] = "https://repo.maven.apache.org/maven2"
    }
    packages {
      "org.springframework.boot:spring-boot-starter-web:3.2.0"
      "com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3"
      "org.jetbrains.kotlin:kotlin-reflect:1.9.21"
    }
    testPackages {
      "org.springframework.boot:spring-boot-starter-test:3.2.0"
    }
  }
}

// Build plugins detected (manual conversion may be needed):
//   - org.springframework.boot:3.2.0
//   - io.spring.dependency-management:1.1.4
//   - org.jetbrains.kotlin.jvm:1.9.21
//   - org.jetbrains.kotlin.plugin.spring:1.9.21
```

### Scenario 2: Android Application

**build.gradle.kts:**
```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.myapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myapp"
        minSdk = 24
        targetSdk = 34
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
```

**Note**: Android-specific configuration (`android {}` block) cannot be automatically converted. The adopter will extract dependencies but warn about manual configuration needed.

### Scenario 3: Kotlin Multiplatform

**build.gradle.kts:**
```kotlin
plugins {
    kotlin("multiplatform") version "1.9.21"
}

kotlin {
    jvm()
    js(IR) { browser() }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("ch.qos.logback:logback-classic:1.4.14")
            }
        }
    }
}
```

**Note**: Multiplatform projects have complex source set dependencies. The adopter will extract JVM dependencies but may not handle all platforms correctly.

## Limitations

### Not Supported

1. **Build Logic**: Custom Gradle tasks and build scripts are not converted
2. **Android Configuration**: `android {}` blocks require manual migration
3. **Kotlin Multiplatform**: Platform-specific source sets not fully supported
4. **Dependency Constraints**: `constraints {}` blocks not converted
5. **Custom Configurations**: Non-standard configurations may not map correctly
6. **Version Catalogs**: `libs.versions.toml` files not yet supported

### Partially Supported

1. **compileOnly Dependencies**: Listed in comments but not in PKL dependencies
2. **Platform Dependencies**: `platform()` and `enforcedPlatform()` partially supported
3. **Plugin Configuration**: Plugins are detected but configuration blocks ignored

### Known Issues

1. **Dynamic Versions**: Version ranges (e.g., `1.+`, `[1.0,2.0)`) are preserved but may not work as expected in Elide
2. **Gradle Properties**: Property interpolation from `gradle.properties` not supported
3. **buildSrc**: Custom code in `buildSrc/` cannot be converted

## Common Conversion Patterns

### Pattern 1: Groovy to Kotlin DSL Variables

**Groovy (build.gradle):**
```groovy
def guavaVersion = '32.1.3-jre'

dependencies {
    implementation "com.google.guava:guava:$guavaVersion"
}
```

**Kotlin DSL (build.gradle.kts):**
```kotlin
val guavaVersion = "32.1.3-jre"

dependencies {
    implementation("com.google.guava:guava:$guavaVersion")
}
```

**Generated PKL:**
```pkl
dependencies {
  maven {
    packages {
      "com.google.guava:guava:32.1.3-jre"
    }
  }
}
```

The adopter resolves variables at parse time.

### Pattern 2: BOM/Platform Dependencies

**build.gradle.kts:**
```kotlin
dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.0"))

    implementation("org.springframework.boot:spring-boot-starter-web")  // Version from BOM
}
```

**Note**: The adopter may not resolve BOM versions - you may need to specify versions explicitly in the generated PKL.

## Troubleshooting

See the [Troubleshooting Guide](./adopt-troubleshooting.md) for common issues and solutions.

## Next Steps

After conversion:

1. **Review Generated PKL**: Check that all dependencies were captured correctly
   ```bash
   cat elide.pkl
   ```

2. **Check Plugin Warnings**: Review build plugin comments
   ```bash
   grep "Build plugins" elide.pkl
   ```

3. **Test Build**: Verify the project builds with Elide
   ```bash
   elide build
   ```

4. **Manual Adjustments**: Add configuration for:
   - Build plugins (test runners, code coverage, etc.)
   - Android-specific settings
   - Custom build tasks

5. **Commit**: Add the new `elide.pkl` to version control
   ```bash
   git add elide.pkl
   git commit -m "feat: adopt Elide build system"
   ```

6. **Optional: Keep Gradle**: You can keep Gradle during transition
   ```bash
   # Use Elide for development
   elide build

   # Keep Gradle for CI/CD during migration
   ./gradlew build
   ```

## Examples

### Example 1: Simple Kotlin Application

**Before:**
```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.21"
    application
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.guava:guava:32.1.3-jre")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.example.MainKt")
}
```

**Convert:**
```bash
$ elide adopt gradle
📋 Parsing Gradle build file...
  File: build.gradle.kts (Kotlin DSL)
  Path: /Users/dev/my-app

✓ Project parsed successfully
  Name: my-app
  Group: com.example
  Version: 1.0.0

📦 Dependencies
  Implementation: 1
  Test: 1

📚 Repositories
  central

🔌 Plugins
  org.jetbrains.kotlin.jvm:1.9.21
  application

✓ Generated elide.pkl (22 lines)

💡 Next steps:
  1. Review the generated elide.pkl file
  2. Run elide build to build your project
  3. Review plugin warnings for manual configuration
```

**After:**
```pkl
amends "elide:project.pkl"

name = "my-app"
version = "1.0.0"

dependencies {
  maven {
    repositories {
      ["central"] = "https://repo.maven.apache.org/maven2"
    }
    packages {
      "com.google.guava:guava:32.1.3-jre"
    }
    testPackages {
      "org.jetbrains.kotlin:kotlin-test:1.9.21"
    }
  }
}

// Build plugins detected (manual conversion may be needed):
//   - org.jetbrains.kotlin.jvm:1.9.21
//   - application

sources {
  ["main"] = "src/main/java/**/*.java"
  ["test"] = "src/test/java/**/*.java"
}
```

### Example 2: Multi-Module Microservices

**Before:**
```
my-services/
├── settings.gradle.kts
├── build.gradle.kts
├── common/
│   └── build.gradle.kts
├── user-service/
│   └── build.gradle.kts
└── order-service/
    └── build.gradle.kts
```

**settings.gradle.kts:**
```kotlin
rootProject.name = "my-services"
include("common", "user-service", "order-service")
```

**Convert:**
```bash
$ elide adopt gradle
📋 Parsing Gradle build file...
  Multi-project build detected

📦 Found 3 subprojects:
  - common
  - user-service
  - order-service

✓ Generated multi-module elide.pkl (38 lines)
  Workspaces: 3
  Dependencies: 12 (external only)
```

**After:**
```pkl
amends "elide:project.pkl"

name = "my-services"

workspaces {
  "common"
  "user-service"
  "order-service"
}

dependencies {
  maven {
    repositories {
      ["central"] = "https://repo.maven.apache.org/maven2"
    }
    packages {
      "com.google.guava:guava:32.1.3-jre"
      "io.ktor:ktor-server-core:2.3.6"
      "io.ktor:ktor-server-netty:2.3.6"
    }
    testPackages {
      "io.ktor:ktor-server-test-host:2.3.6"
      "org.jetbrains.kotlin:kotlin-test:1.9.21"
    }
  }
}
```

## Comparison: Gradle vs Elide

| Feature | Gradle | Elide |
|---------|--------|-------|
| **Configuration Language** | Groovy/Kotlin DSL | PKL |
| **Dependency Declaration** | `implementation("...")` | `packages { "..." }` |
| **Repositories** | `repositories { }` | `repositories { }` |
| **Multi-Project** | `settings.gradle` + `include()` | `workspaces { }` |
| **Test Dependencies** | `testImplementation()` | `testPackages { }` |
| **Plugins** | `plugins { }` block | Manual configuration |
| **Build Scripts** | Kotlin/Groovy code | PKL configuration |

## Related Documentation

- [Migrating from Maven](./migrating-from-maven.md)
- [Troubleshooting Guide](./adopt-troubleshooting.md)
- [Elide PKL Reference](../elide-pkl-reference.md) _(coming soon)_
- [Elide CLI Guide](../cli-guide.md) _(coming soon)_

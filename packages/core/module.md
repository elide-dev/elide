# Module core

`elide-core` provides the widest possible platform support for Elide tooling and logic.

## Universal applicability

Utilities and declarations here are used throughout the framework and runtime.

At this level, utilities are truly universal, with support for some encodings, annotations, and other _pure-Kotlin_
logic. This is the lowest level of Elide, and it is the most portable.

Because of this strict support guarantee, `elide-core` only depends on dependencies directly from the Kotlin team,
including Kotlin `stdlib` and KotlinX.

## Installation

The `elide-core` module is provided via
[Maven Central](https://search.maven.org/search?q=g:dev.elide%20AND%20a:elide-core), and also via Elide's own
[GitHub Packages](https://github.com/orgs/elide-dev/packages?ecosystem=maven&q=core&tab=packages&ecosystem=maven&q=elide-core)
repository.

**Via Gradle (Catalog):**

```kotlin
implementation(framework.elide.core)
```

**Via Gradle (Kotlin DSL):**

```kotlin
implementation("dev.elide:elide-core")
```

**Via Gradle (Groovy DSL):**

```kotlin
implementation "dev.elide:elide-core"
```

**Via Maven:**

```xml
<dependency>
  <groupId>dev.elide</groupId>
  <artifactId>elide-core</artifactId>
</dependency>
```

# Package elide.core

Platform and global defaults.

# Package elide.core.annotations

Cross-platform annotations which are made available to all apps.

# Package elide.core.api

API utilities and markers.

# Package elide.core.crypto

Enumerations for cryptography and hashing.

# Package elide.core.encoding

Cross-platform encoding utilities and API.

# Package elide.core.encoding.base64

Pure-Kotlin base64 encoding via `elide.core.encoding`.

# Package elide.core.encoding.hex

Pure-Kotlin hex encoding via `elide.core.encoding`.

# Package elide.core.platform

Platform-specific defaults.

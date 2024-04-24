# Module base

`elide-base`  is a multi-platform Kotlin module which provides basic utilities that are typically useful for all
software applications. For example:

- **Annotations:** Common annotations used across the Elide framework and runtime
- **Codecs:** Multiplatform-capable implementations of Base64, hex, and other common encoding tools
- **Crypto:** Core crypto, hashing, and entropy primitives (for example, `UUID`)
- **Structures:** Data structures in MPP and pure Kotlin for sorted maps, sets, and lists
- **Logging:** Multiplatform-capable logging, which delegates to platform logging tools

## Platform Support

The `elide-base` module supports all Kotlin Multiplatform targets, including WASM/WASI. The `elide-base` module only
depends on Elide Core, Kotlin Stdlib, and KotlinX.

## Installation

The `elide-base` package is provided via [Maven Central][0], and also via Elide's own [GitHub Packages][1] repository.
Base only depends on KotlinX and `elide-core`.

**Via Gradle (Catalog):**

```kotlin
implementation(framework.elide.base)
```

**Via Gradle (Kotlin DSL):**

```kotlin
implementation("dev.elide:elide-base")
```

**Via Gradle (Groovy DSL):**

```kotlin
implementation "dev.elide:elide-base"
```

**Via Maven:**

```xml
<dependency>
  <groupId>dev.elide</groupId>
  <artifactId>elide-base</artifactId>
</dependency>
```

# Package elide

Globally applicable enumerations and definitions.

# Package elide.annotations

General annotations which apply to all applications.

# Package elide.annotations.base

Annotations provided as part of the `elide-base` package.

# Package elide.annotations.core

Deprecated: moved to `elide.annotations.base`.

# Package elide.annotations.data

Annotations which model and govern application data.

# Package elide.runtime

Core interfaces like logging and runtime info.

# Package elide.runtime.js

JavaScript bridge to `console`-based logging, among other JS utilities.

# Package elide.runtime.jvm

Java bridge to SLF4J-based logging, among other JVM utilities.

# Package elide.struct

Provides simple data structures like sorted maps and sets.

# Package elide.struct.api

Pure-Kotlin equivalents to JVM's base collection APIs, including `SortedList`, `SortedMap`, and `SortedSet`.

# Package elide.struct.codec

Kotlin Serialization codecs for `elide.struct` types.

# Package elide.util

Encoders, runtime flags, and UUID types; miscellaneous utilities.

[0]: https://search.maven.org/search?q=g:dev.elide%20AND%20a:elide-base
[1]: https://github.com/orgs/elide-dev/packages?ecosystem=maven&q=core&tab=packages&ecosystem=maven&q=elide-base

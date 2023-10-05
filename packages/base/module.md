# Module base

The `elide-base` module is a multi-platform Kotlin module which provides basic utilities which are typically useful for
all applications.

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

# Package elide.util

Encoders, runtime flags, and UUID types; miscellaneous utilities.

[0]: https://search.maven.org/search?q=g:dev.elide%20AND%20a:elide-base
[1]: https://github.com/orgs/elide-dev/packages?ecosystem=maven&q=core&tab=packages&ecosystem=maven&q=elide-base

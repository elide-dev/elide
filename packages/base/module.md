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

[0]: https://search.maven.org/search?q=g:dev.elide%20AND%20a:elide-base
[1]: https://github.com/orgs/elide-dev/packages?ecosystem=maven&q=core&tab=packages&ecosystem=maven&q=elide-base

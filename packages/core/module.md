# Module core

The `elide-core` module is a multi-platform Kotlin module which provides the widest possible platform support for Elide
tooling and logic; utilities which reside here are supported for all [Kotlin platforms][2].

## Universal applicability

At this level, utilities are truly universal, with support for some encodings, annotations, and other *pure-Kotlin*
logic. This is the lowest level of Elide, and it is the most portable.

Because of this strict support guarantee, `elide-core` only depends on dependencies directly from the Kotlin team,
including Kotlin `stdlib` and KotlinX.

## Installation

The `elide-core` package is provided via [Maven Central][0], and also via Elide's own [GitHub Packages][1] repository.

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

[0]: https://search.maven.org/search?q=g:dev.elide%20AND%20a:elide-core
[1]: https://github.com/orgs/elide-dev/packages?ecosystem=maven&q=core&tab=packages&ecosystem=maven&q=elide-core
[2]: https://kotlinlang.org/docs/multiplatform.html

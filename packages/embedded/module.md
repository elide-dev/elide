# Module embedded

`elide-embedded` implements native embedded dispatch of Elide applications.

## Usage

Embedded Elide dispatch is meant to be implemented from within a [GraalVM](https://www.graalvm.org/) native image. The
interface accepts native structures for request dispatch, and handles routing, marshaling, and error handling.

## Installation

The `elide-embedded` module is provided via
[Maven Central](https://search.maven.org/search?q=g:dev.elide%20AND%20a:elide-embedded), and also via Elide's own
[GitHub Packages](https://github.com/orgs/elide-dev/packages?ecosystem=maven&q=core&tab=packages&ecosystem=maven&q=elide-embedded)
repository.

**Via Gradle (Catalog):**

```kotlin
implementation(framework.elide.embedded)
```

**Via Gradle (Kotlin DSL):**

```kotlin
implementation("dev.elide:elide-embedded")
```

**Via Gradle (Groovy DSL):**

```kotlin
implementation "dev.elide:elide-embedded"
```

**Via Maven:**

```xml

<dependency>
  <groupId>dev.elide</groupId>
  <artifactId>elide-embedded</artifactId>
</dependency>
```

# Package elide.embedded

Public API used to interact with the embedded runtime, including configuration, initialization, and dispatch.
Implementations for most interfaces are kept private and are meant to be accessed only by the `ElideEmbedded` class,
as a singleton representing a runtime instance.

# Package elide.embedded.http

Public types used during dispatch, as a bridge between guest application code and runtime logic.

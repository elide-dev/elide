# Module test

`elide-test` is a multi-platform Kotlin module which provides cross-platform test and assertion utilities and
annotations.

### Platform support

Elide's testing module uses [JUnit 5](https://junit.org/junit5/docs/current/user-guide/) via
[Micronaut Test](https://micronaut-projects.github.io/micronaut-test/latest/guide/index.html) when running on JVM.

## Installation

The `elide-test` package is provided via
[Maven Central](https://search.maven.org/search?q=g:dev.elide%20AND%20a:elide-test), and also via Elide's own
[GitHub Packages](https://github.com/orgs/elide-dev/packages?ecosystem=maven&q=core&tab=packages&ecosystem=maven&q=elide-test) repository.

**Via Gradle (Catalog):**

```kotlin
implementation(framework.elide.test)
```

**Via Gradle (Kotlin DSL):**

```kotlin
implementation("dev.elide:elide-test")
```

**Via Gradle (Groovy DSL):**

```kotlin
implementation "dev.elide:elide-test"
```

**Via Maven:**

```xml
<dependency>
  <groupId>dev.elide</groupId>
  <artifactId>elide-test</artifactId>
</dependency>
```

# Package elide.server

Test stub for Elide Server API tests.

# Package elide.server.runtime

Test-oriented runtime stub for Elide Server API tests.

# Package elide.testing

Provides testing and assertion utilities.

# Package elide.testing.annotations

Provides cross-platform annotations for use in tests.

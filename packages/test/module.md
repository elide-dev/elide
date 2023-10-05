# Module test

The `elide-test` module is a multi-platform Kotlin module which provides cross-platform test and assertion utilities and
annotations.

### Platform support

Elide's testing module uses [JUnit 5][2] via [Micronaut Test][3] when running on JVM.

## Installation

The `elide-test` package is provided via [Maven Central][0], and also via Elide's own [GitHub Packages][1] repository.

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

[0]: https://search.maven.org/search?q=g:dev.elide%20AND%20a:elide-test
[1]: https://github.com/orgs/elide-dev/packages?ecosystem=maven&q=core&tab=packages&ecosystem=maven&q=elide-test
[2]: https://junit.org/junit5/docs/current/user-guide/
[3]: https://micronaut-projects.github.io/micronaut-test/latest/guide/index.html

# Module graalvm-kt

`elide-graalvm-kt` provides language support for [Kotlin](https://kotlinlang.org/), on top of `graalvm-jvm`.

## How Elide uses Espresso

Elide uses Espresso to support JVM languages like Java, Kotlin, and Groovy. The Kotlin plugin, implemented by this
module, depends on the `elide-jvm` package.

## Installation

This package is made available via Maven Central and Elide's package repository, but is not designed for end-user
consumption. Language support is built-in to the Elide runtime.

**Via Gradle (Catalog):**

```kotlin
implementation(framework.elide.graalvm.kt)
```

**Via Gradle (Kotlin DSL):**

```kotlin
implementation("dev.elide:elide-graalvm-kt")
```

**Via Gradle (Groovy DSL):**

```kotlin
implementation "dev.elide:elide-graalvm-kt"
```

**Via Maven:**

```xml
<dependency>
  <groupId>dev.elide</groupId>
  <artifactId>elide-graalvm-kt</artifactId>
</dependency>
```

# Package elide.runtime.plugins.kotlin

Implements an Elide VM plug-in for execution of Kotlin via Espresso.

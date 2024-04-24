# Module graalvm-java

`elide-graalvm-java` provides language support for [Java](https://www.java.com/), on top of `graalvm-jvm`.

## Installation

This package is made available via Maven Central and Elide's package repository, but is not designed for end-user
consumption. Language support is built-in to the Elide runtime.

**Via Gradle (Catalog):**

```kotlin
implementation(framework.elide.graalvm.java)
```

**Via Gradle (Kotlin DSL):**

```kotlin
implementation("dev.elide:elide-graalvm-java")
```

**Via Gradle (Groovy DSL):**

```kotlin
implementation "dev.elide:elide-graalvm-java"
```

**Via Maven:**

```xml
<dependency>
  <groupId>dev.elide</groupId>
  <artifactId>elide-graalvm-java</artifactId>
</dependency>
```

## How Elide uses Espresso

Elide uses Espresso to support JVM languages like Java, Kotlin, and Groovy. The Java plugin, implemented by this
module, depends on the `elide-jvm` package.

# Package elide.runtime.plugins.java

Implements an Elide VM plug-in for execution of Java via Espresso.

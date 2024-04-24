# Module graalvm-jvm

`elide-graalvm-jvm` integrates Elide and [Espresso](https://www.graalvm.org/latest/reference-manual/java-on-truffle/), a
high-performance Java runtime based on the Truffle framework.

## How Elide uses Espresso

Elide uses Espresso to support JVM languages like Java, Kotlin, and Groovy. The Java plugin, implemented by this
module, depends on the `elide-jvm` package.

## Installation

This package is made available via Maven Central and Elide's package repository, but is not designed for end-user
consumption. Language support is built-in to the Elide runtime.

**Via Gradle (Catalog):**

```kotlin
implementation(framework.elide.graalvm.jvm)
```

**Via Gradle (Kotlin DSL):**

```kotlin
implementation("dev.elide:elide-graalvm-jvm")
```

**Via Gradle (Groovy DSL):**

```kotlin
implementation "dev.elide:elide-graalvm-jvm"
```

**Via Maven:**

```xml
<dependency>
  <groupId>dev.elide</groupId>
  <artifactId>elide-graalvm-jvm</artifactId>
</dependency>
```

# Package elide.runtime.plugins.jvm

Implements an Elide VM plug-in for execution of JVM languages via Espresso.

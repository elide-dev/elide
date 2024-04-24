# Module graalvm-llvm

`elide-graalvm-llvm` integrates Elide and [Sulong](https://github.com/oracle/graal/blob/master/sulong/README.md),
GraalVM's LLVM interpreter.

## How Elide uses Sulong

Elide uses Sulong to support LLVM-implemented languages like GraalPython and TruffleRuby.

## Installation

This package is made available via Maven Central and Elide's package repository, but is not designed for end-user
consumption. Language support is built-in to the Elide runtime.

**Via Gradle (Catalog):**

```kotlin
implementation(framework.elide.graalvm.llvm)
```

**Via Gradle (Kotlin DSL):**

```kotlin
implementation("dev.elide:elide-graalvm-llvm")
```

**Via Gradle (Groovy DSL):**

```kotlin
implementation "dev.elide:elide-graalvm-llvm"
```

**Via Maven:**

```xml
<dependency>
  <groupId>dev.elide</groupId>
  <artifactId>elide-graalvm-llvm</artifactId>
</dependency>
```

# Package elide.runtime.plugins.llvm

Implements an Elide VM plug-in for execution of LLVM bitcode and related languages via Sulong.

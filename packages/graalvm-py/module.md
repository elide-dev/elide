# Module graalvm-py

`elide-graalvm-py` provides language support for [Python](https://www.python.org/) on top of
[GraalPython](https://github.com/oracle/graalpython).

## How Elide uses GraalPython

Elide supports Python `3.10.x` via [GraalPython](https://github.com/oracle/graalpython), a compliant implementation of
Python 3. Elide supplies various integration points like application environment, virtual I/O, and smooth interop for
polyglot dispatch with JavaScript.

## Installation

This package is made available via Maven Central and Elide's package repository, but is not designed for end-user
consumption. Language support is built-in to the Elide runtime.

**Via Gradle (Catalog):**

```kotlin
implementation(framework.elide.graalvm.py)
```

**Via Gradle (Kotlin DSL):**

```kotlin
implementation("dev.elide:elide-graalvm-py")
```

**Via Gradle (Groovy DSL):**

```kotlin
implementation "dev.elide:elide-graalvm-py"
```

**Via Maven:**

```xml
<dependency>
  <groupId>dev.elide</groupId>
  <artifactId>elide-graalvm-py</artifactId>
</dependency>
```

# Package elide.runtime.gvm.internals.python

# Package elide.runtime.gvm.python

# Package elide.runtime.gvm.python.cfg

# Package elide.runtime.plugins.python

# Package elide.runtime.plugins.python.features

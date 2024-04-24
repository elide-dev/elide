# Module graalvm-rb

`elide-graalvm-rb` provides language support for [Ruby](https://www.ruby-lang.org/) on top of
[TruffleRuby](https://github.com/oracle/truffleruby).

## How Elide uses TruffleRuby

Elide supports Ruby `3.2.x` via [TruffleRuby](https://github.com/oracle/truffleruby). Elide supplies various integration
points like application environment, virtual I/O, and smooth interop for polyglot dispatch with JavaScript.

## Installation

This package is made available via Maven Central and Elide's package repository, but is not designed for end-user
consumption. Language support is built-in to the Elide runtime.

**Via Gradle (Catalog):**

```kotlin
implementation(framework.elide.graalvm.rb)
```

**Via Gradle (Kotlin DSL):**

```kotlin
implementation("dev.elide:elide-graalvm-rb")
```

**Via Gradle (Groovy DSL):**

```kotlin
implementation "dev.elide:elide-graalvm-rb"
```

**Via Maven:**

```xml
<dependency>
  <groupId>dev.elide</groupId>
  <artifactId>elide-graalvm-rb</artifactId>
</dependency>
```

# Package elide.runtime.gvm.internals.ruby

# Package elide.runtime.gvm.ruby

# Package elide.runtime.gvm.ruby.cfg

# Package elide.runtime.plugins.ruby

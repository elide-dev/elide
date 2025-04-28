# Java and Kotlin

This guide explains how Elide supports JVM technologies like Java and Kotlin.

- Use Maven dependencies from Central or elsewhere (see `elide help projects`)
- Compile Java code with `javac`, which Elide embeds
- Compile Kotlin code with `kotlinc`, which Elide embeds
- Create JARs, generate Javadoc, and so on, with `elide <javadoc|jar>`

## Java

Elide supports Java up to JDK 24. Elide's program root is usable as a `JAVA_HOME` root; just point your `JAVA_HOME` to
the folder containing `elide`. Gradle and Maven should recognize Elide as an instance of Oracle GraalVM at JDK 24.

Java 24 is the latest major version as of April 2024. Supported tools include:

- `javac` (Java compiler)
- `javadoc` (Java documentation generator)
- `jar` (Java archive tool)

Several core libraries are included with Elide:

- Jacoco, supporting JVM code coverage

## Kotlin

Elide supports Kotlin up to K2 (v2.x). Elide's program root is usable as a `KOTLIN_HOME` root; just point `KOTLIN_HOME`
to `<elide's root>/resources/kotlin/<desired version>`.

Kotlin K2 is the latest major version as of April 2024. Supported tools include:

- `kotlinc` (Kotlin compiler)
- `kapt` (Kotlin annotation processor)
- KSP (Kotlin Symbol Processing)
- Kotlin Scripting compiler

### KotlinX

In addition to shipping the Kotlin standard library and compiler, Elide ships a full up-to-date copy of the "KotlinX"
(Kotlin Extension) libraries. Included within the distribution are:

- `coroutines` (coroutines/concurrency)
- `datetime` (date and time)
- `html` (HTML rendering)
- `serialization` (serialization)
- `serialization.json` (JSON support)

You don't need to do anything to use these; they are included in your classpath automatically when you use Kotlin,
unless you opt out with:

```pkl
kotlin {
  features {
    kotlinx = false  // defaults to true
  }
}
```

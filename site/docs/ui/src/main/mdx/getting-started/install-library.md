
#### Using Elide as a Library

Using Elide as a framework or library is supported for **any language on the JVM**. Elide's components can be installed
like any other Maven dependencies. See the full list of available Maven coordinates on the [Packages](/library/packages)
page.

##### Gradle

> See docs for the [Elide Plugin for Gradle](/tools/gradle).

In **Groovy**:

```groovy
  dependencies {
    // for the `core` package:
    implementation "dev.elide:elide-core:<latest-version>"
  }
```

Or, for the **Kotlin DSL**:

```kotlin
  dependencies {
    // for the `core` package:
    implementation("dev.elide:elide-core:<latest-version>")
  }
```

##### Maven

Elide doesn't yet have a Maven plugin. However, all [Elide packages](/library/packages) are available on Maven Central
and are usable in a Maven project:

```xml
   <!-- for the `core` package`: -->
   <dependency>
       <groupId>dev.elide</groupId>
       <artifactId>elide-core</artifactId>
       <version><!-- latest version here --></version>
   </dependency>
```

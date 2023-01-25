
Elide provides first-class support for [Gradle][1] via the [Gradle Plugin for Elide][2].

<br />

<a id="install"></a>

#### Installing the plug-in

The plug-in is available via the Gradle plug-in portal. Make sure to check for the latest version upon installation:

To use the plug-in from the **Kotlin DSL**:

```kotlin
plugins {
  id("dev.elide.buildtools.plugin") version "<version here>"
}
```

To use the plug-in from the **Groovy DSL**:

```groovy
plugins {
  id "dev.elide.buildtools.plugin" version "<version here>"
}
```

<br />

<a id="features"></a>

#### Supported features

The Gradle plug-in helps wire together Elide-based projects which use Gradle as a build-tool. Depending on the structure
of your project, you can use the plug-in to:

- **Build server targets.** Use the plug-in to pull in and reference static assets, embed SSR scripts, and more.
- **Frontend target.** Use the plug-in to wire together a Kotlin JS target or Node JS build.
- **SSR scripts.** Compile and bundle SSR scripts for use embedded in your server targets.

[1]: https://gradle.org/
[2]: https://plugins.gradle.org/plugin/dev.elide.buildtools.plugin

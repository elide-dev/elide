
# Elide Plugin for Gradle

![alpha](https://img.shields.io/badge/status-alpha-yellow.svg)
[![CI](https://github.com/elide-dev/buildtools/actions/workflows/pre-merge.yaml/badge.svg)](https://github.com/elide-dev/buildtools/actions/workflows/pre-merge.yaml)
[![Kotlin](https://img.shields.io/badge/kotlin-1.7.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/gradle-7.x-blue.svg?logo=gradle)](http://gradle.org)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_buildtools&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=elide-dev_buildtools)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_buildtools&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=elide-dev_buildtools)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_buildtools&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=elide-dev_buildtools)

_**Elide is under construction.**_

<hr />

This plugin for Gradle enables [Elide](https://github.com/elide-dev)-based apps with additional build tooling and tasks.
In particular, this plugin is responsible for configuring and running [`esbuild`](https://esbuild.github.io) and
[`webpack`](https://webpack.js.org) on behalf of the developer.

### Installing the plugin

It's a standard Gradle plugin, so you should be able to check the latest version at the
[Gradle Plugins Portal](https://plugins.gradle.org). See below for specific installation instructions.

#### From a single-module Gradle project

**Kotlin DSL**
```kotlin
plugins {
  id("dev.elide.buildtools.plugin") version "(latest version here)"
}
```

**Groovy DSL**
```groovy
plugins {
    id "dev.elide.buildtools.plugin" version "(latest version here)"
}
```

#### From a multi-module Gradle project

**Kotlin DSL** (via `buildSrc`)
```kotlin
repositories {
    gradlePluginPortal()
}

dependencies {
  implementation("dev.elide.buildtools:elide-gradle-plugin:(latest version here)")
}
```

**Groovy DSL** (via `buildSrc`)
```groovy
repositories {
    gradlePluginPortal()
}

dependencies {
  implementation "dev.elide.buildtools:elide-gradle-plugin:(latest version here)"
}
```

**Kotlin DSL** (via root `build.gradle.kts`)
```kotlin
dependencies {
  classpath("dev.elide.buildtools:elide-gradle-plugin:(latest version here)")
}
```

**Groovy DSL** (via root `build.gradle`)
```groovy
dependencies {
  classpath "dev.elide.buildtools:elide-gradle-plugin:(latest version here)"
}
```


### Using the plugin

Once you've installed the plugin (see above), you can configure your build with:

**Kotlin DSL** (module `build.gradle.kts`)
```kotlin
elide {
  /* docs coming soon */
}
```

**Groovy DSL** (module `build.gradle`)
```groovy
elide {
  /* docs coming soon */
}
```

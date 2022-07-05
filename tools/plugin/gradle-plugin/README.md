
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

### Features
- [x] **Embedded SSR:** Build JS apps, including Kotlin JS, into optimized embedded SSR bundles
- [x] **Asset compiler:** Optimize and compile frontend assets, and package them for use in Elide

### Installing the plugin

It's a standard Gradle plugin, designed to be applied to **multi-module Gradle projects**. You should be able to check
the latest version at the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/dev.elide.buildtools.plugin). See
below for specific installation and use instructions.

**Kotlin DSL**
```kotlin
plugins {
  id("dev.elide.buildtools.plugin") version "1.0.0-alpha4"
}
```

**Groovy DSL**
```groovy
plugins {
    id "dev.elide.buildtools.plugin" version "1.0.0-alpha4"
}
```

#### Plugin coordinates

- **Version:** `1.0.0-alpha4`
- **Plugin ID:** `dev.elide.buildtools.plugin`
- **Classpath Coordinate:** `dev.elide.buildtools:plugin`


# Building Elide apps with Gradle

The plugin supports two build styles with Elide: **(1)** packaging frontend assets to be served by your JVM app, and
**(2)** packaging scripting languages to be executed server-side, for instance, React, Vue, or Angular apps. JavaScript
is the only supported language at this time, but more languages may be supported in the future.

## Using the plugin

The Elide plugin is built to work with the [Kotlin Plugin for Gradle][1], specifically the [Kotlin/JS plugin][2] and the
[Kotlin/JVM plugin][3] (multi-platform support is on the roadmap). In your Elide app project, you should have modules
for your `frontend` and `server` (or whatever you chose to call them).

Apply the plugin to both, depending on your needs.

---

### SSR Example

First up is your frontend module:

**Client** (module `frontend`)
```kotlin
plugins {
    kotlin("js")
    id("com.github.node-gradle.node")
    id("dev.elide.buildtools.plugin")
}
```

Optionally, you can configure the plugin. Below are some example configuration options:

```kotlin
plugins {
    kotlin("js")
    id("com.github.node-gradle.node")
    id("dev.elide.buildtools.plugin")
}

elide {
    mode = BuildMode.DEVELOPMENT

    js {
        tool(BundleTool.ESBUILD)
        target(BundleTarget.EMBEDDED)

        runtime {
            languageLevel(JsLanguageLevel.ES2020)
        }
    }
}
```

This will bundle your SSR app with `esbuild`, using `ES2020` as the language standard. More docs are coming soon for
frontend SSR builds, but in the meantime, code completion via the Gradle Kotlin DSL works great.

Next, we'll configure the server module, which needs to know where to find your frontend app:

**Server** (module `server`)
```kotlin
plugins {
  kotlin("jvm")
  id("dev.elide.buildtools.plugin")
}

elide {
  mode = BuildMode.DEVELOPMENT

  server {
    ssr(EmbeddedScriptLanguage.JS) {
      bundle(project(":frontend"))
    }
  }
}
```

Here, we're configuring the server to consume an **SSR bundle**, in **JavaScript**, from your `frontend` module. That's
all you need to do for SSR.

---

### Assets Example

Server assets can be packaged and embedded via this plugin, in concert with the Kotlin JVM plugin. See below:

**Server** (module `server`)
```kotlin
plugins {
    kotlin("jvm")
    id("dev.elide.buildtools.plugin")
}

elide {
    server {
        assets {
            bundler {
                compression {
                    modes(CompressionMode.GZIP)
                }
            }

            // stylesheet: `main.base`
            stylesheet("main.base") {
                sourceFile("src/main/assets/basestyles.css")
            }

            // stylesheet: `main.styles`
            stylesheet("main.styles") {
                sourceFile("src/main/assets/coolstyles.css")
                dependsOn("main.base")
            }

            // script: `main.js`
            script("main.js") {
                sourceFile("src/main/assets/some-script.js")
            }

            // text: `util.humans`
            text("util.humans") {
                sourceFile("src/main/assets/humans.txt")
            }
        }
    }
}
```

This will bundle the configured assets in Elide's asset format, and will pre-compress them with Gzip. Dependencies are
expressed in the asset manifest, allowing for efficient multi-file loading in your frontend.

---

### Combined Example

The frontend assets task can be combined with an SSR task:

**Client** (module `frontend`)
```kotlin
plugins {
    kotlin("js")
    id("com.github.node-gradle.node")
    id("dev.elide.buildtools.plugin")
}

elide {
    mode = BuildMode.DEVELOPMENT

    js {
        tool(BundleTool.ESBUILD)
        target(BundleTarget.EMBEDDED)

        runtime {
            languageLevel(JsLanguageLevel.ES2020)
        }
    }
}
```

**Server** (module `server`)
```kotlin
plugins {
    kotlin("jvm")
    id("dev.elide.buildtools.plugin")
}

elide {
    server {
        assets {
            // settings for the asset bundler
            bundler {
                compression {
                    modes(CompressionMode.GZIP)
                }
            }

            // example: adding a stylesheet module
            stylesheet("main.base") {
                sourceFile("src/main/assets/basestyles.css")
            }

            // example: adding a stylesheet module with a dependency
            stylesheet("main.styles") {
                sourceFile("src/main/assets/coolstyles.css")
                dependsOn("main.base")
            }

            // example: adding a script module
            script("main.js") {
                sourceFile("src/main/assets/some-script.js")
            }

            // example: adding a plain-text asset module
            text("util.humans") {
                sourceFile("src/main/assets/humans.txt")
            }
        }
    }
}
```


[1]: https://kotlinlang.org/docs/gradle.html
[2]: https://kotlinlang.org/docs/js-project-setup.html
[3]: https://kotlinlang.org/docs/jvm-get-started.html

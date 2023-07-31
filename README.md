<p align="center">
  <a href="https://github.com/elide-dev/v3">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="./creative/logo/logo-wide-1200-w-r2.png">
      <img src="./creative/logo/logo-wide-1200-w-r2.png" height="300">
    </picture>
  </a>
</p>

<p align="center">
<i>elide: verb. to omit (a sound or syllable) when speaking. to join together; to merge.</i>
</p>

<hr />

[![Build](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml/badge.svg)](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml)
[![codecov](https://codecov.io/gh/elide-dev/v3/branch/v3/graph/badge.svg?token=FXxhJlpKG3)](https://codecov.io/gh/elide-dev/v3)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=reliability_rating&token=7e7d03a5cb8a12b7297eb6eedf5fe9b93ade6d75)](https://sonarcloud.io/summary/new_code?id=elide-dev_v3)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=security_rating&token=7e7d03a5cb8a12b7297eb6eedf5fe9b93ade6d75)](https://sonarcloud.io/summary/new_code?id=elide-dev_v3)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=sqale_rating&token=7e7d03a5cb8a12b7297eb6eedf5fe9b93ade6d75)](https://sonarcloud.io/summary/new_code?id=elide-dev_v3)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Felide-dev%2Fv3.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Felide-dev%2Fv3?ref=badge_shield)
[![Java 20](https://img.shields.io/badge/Java-20-blue.svg?logo=oracle)](https://openjdk.org/projects/jdk/19/)
[![GraalVM](https://img.shields.io/badge/GraalVM-22.3.x-blue.svg?logo=oracle)](https://www.graalvm.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Kotlin/JS. IR supported](https://img.shields.io/badge/kotlin-IR-yellow?logo=kotlin&logoColor=yellow)](https://kotl.in/jsirsupported)
[![ECMA](https://img.shields.io/badge/ECMA-2020-blue.svg?logo=javascript)](https://262.ecma-international.org/11.0/)

Latest version: `1.0-v3-alpha4-b9`

<hr />

Elide is a **Kotlin/Multiplatform meta-framework** for **rapid, cross-platform application development**. Write once in
Kotlin and deploy everywhere: your server, the browser, and native app targets.

## Using Elide as a runtime

First and foremost, Elide is a runtime. The runtime is **still in alpha**. You can try it with:
```bash
curl -sSL --tlsv1.2 "dl.elide.dev/cli/install.sh" | bash -s -
```

Or you can try it via NPM:
```bash
npx @elide-dev/elide@alpha --help
```


## Using Elide as a library

Elide is available on [Maven Central](https://search.maven.org/artifact/dev.elide). To use it, pick the package you want
to use (typically `server`) and add it via Gradle or Maven:

### Gradle

```groovy
dependencies {
    implementation 'dev.elide:elide-server:1.0-v3-alpha4-b9'
}
```

```kotlin
dependencies {
    implementation("dev.elide:elide-server:1.0-v3-alpha4-b9")
}
```

### Maven

```xml
<dependency>
    <groupId>dev.elide</groupId>
    <artifactId>elide-server</artifactId>
    <version>1.0-v3-alpha4-b9</version>
</dependency>
```

## Distinguishing features

- **Full-stack development.** Share code across platforms with Kotlin Multiplatform. Quickly develop performant UIs in
  Kotlin, TypeScript, or JavaScript, and execute them server-side (streaming SSR) or client-side (SPA).

- **Countless ways to run.** Use SSR (server rendering), CSR (client rendering), or an isomorphic approach. Compile your
  app to HTML via SSG. Run your app on a JVM, Node.js, JS runtime, or compile it to a native binary and run it without
  a runtime at all.

- **Pure Kotlin when you want it.** Write your core application logic and models once, and share them across platforms
  transparently. Leverage [Kotest](https://kotest.io) for cross-platform, write-once-run-native testing. Enjoy
  first-class support for the full suite of KotlinX libraries, including
  [`serialization`](https://kotlinlang.org/docs/serialization.html),
  [`atomicfu`](https://github.com/Kotlin/kotlinx.atomicfu), [`coroutines`](https://github.com/Kotlin/kotlinx.coroutines)
  and [`datetime`](https://github.com/Kotlin/kotlinx-datetime).

- **TypeScript/JavaScript when you need it.** Plug your Kotlin code into the JavaScript ecosystem with embedded guest VM
  support for ES2022.

- **Isomorphic SSR with React.** Write your UI in React, using JavaScript, TypeScript, or Kotlin. Package it for serving
  via [client-side rendering or hybrid isomorphic rendering](https://web.dev/rendering-on-the-web/) directly from your
  Kotlin server.

- **Model-driven development.** Write your models once, and use them everywhere, across platforms, **without copying**,
  **without glue-code**, and **without DTOs**. Via [Protobuf][5] and
  [Kotlin data classes](https://kotlinlang.org/docs/data-classes.html), the same Elide model is code-generated for use
  with your database, API, and UI.

- **Extreme performance.** Enjoy fast development locally with Kotlin and Gradle, and insanely fast runtime performance
  thanks to GraalVM Native, Netty, and Micronaut. Deploy to bare metal, Docker `scratch` images, or JARs.

## Code samples

A full suite of [code samples](./samples) demo various functionality. The samples are runnable locally or via pre-built
Docker images. Click through to each one for a short tour and getting started guide.

"Blah blah blah, okay, show me some code." Certainly:

**`App.kt`** (for the server)

```kotlin
/** GET `/`: Controller for index page. */
@Controller class Index {
  // Serve an HTML page with isomorphic React SSR.
  @Get("/") fun index() = ssr {
    head {
      title { +"Hello, Elide!" }
      stylesheet("/styles/main.css")
      script("/scripts/ui.js", defer = true)
    }
    body {
      injectSSR()
    }
  }

  // Serve styles for the page.
  @Get("/styles/main.css") fun styles() = css {
    rule("body") {
      backgroundColor = Color("#bada55")
    }
    rule("strong") {
      fontFamily = "-apple-system, BlinkMacSystemFont, sans-serif"
    }
  }

  // Serve the built & embedded JavaScript.
  @Get("/scripts/ui.js") suspend fun js(request: HttpRequest<*>) = script(request) {
    module("scripts.ui")
  }
}
```

**`main.kt`** (for the browser)

```kotlin
// React props for a component
external interface HelloProps: Props {
  var name: String
}

// React component which says hello
val HelloApp = FC<HelloProps> { props ->
  div {
    strong {
      +props.name
    }
  }
}

// Entrypoint for the browser
fun main() {
  hydrateRoot(
    document.getElementById("root"),
    Fragment.create() {
      HelloApp {
        name = "Elide"
      }
    }
  )
}
```

That's it. That's the entire app. In fact, it's just the [`fullstack/react-ssr`](./samples/fullstack/react-ssr) sample
pasted into the README. What we get out of this is surprising:

- **Server:** JVM or Native server (via GraalVM) that serves our root page, our CSS, and our JS
- **Client:** Embedded JS VM that executes a Node copy of our React UI and splices it into the page for isomorphic rendering
- **For us** (the developer):
  - Completely type-checked calls across platforms, with almost no boilerplate
  - Single build graph, with aggressive caching and tool support
  - Build & test virtualized on any platform
  - Ship & perform natively on any platform

What's going on here? Elide has helped us wire together code from **Micronaut** (that's where `@Controller` comes from),
**Kotlin/JS**, **GraalVM**, and **esbuild** to make the above code make sense on both platforms. The React code builds
for the browser _and_ a pure server environment; both are embedded into the JAR and served through a thin runtime layer
provided by the framework.

#### Why is this useful?

If you're participating in the React and Java ecosystems, this gives you a fantastic best-of-both-worlds runtime option:
superb tooling support for React and Kotlin and an ideal serving and rendering mode, all handled for you.

You can do the same thing with these same tools in a custom codebase, but setting up the build environment for this kind
of app is challenging and error-prone. Elide _intentionally_ leverages existing frameworks with rich ecosystems and
docs, _instead of_ re-providing existing functionality so that you always have an escape hatch up to a more industrial
toolset if you need it.

### Trying it out

> **Note**
> Elide is early. This guide will soon be usable without cloning the source.

There are currently two ways to try out Elide. You can build a sample from source, or run the pre-built Docker images.
Native images are not yet available via Docker, but you can build and test them locally.

The `react-ssr` sample is recommended, because it demoes the broadest set of functionality currently available. Source
code for each sample is in the [`samples/`](./samples) directory. If you're going to build from source, make sure to see
the _Requirements to build_ section.

**Run the `helloworld` sample via Docker (JVM):**

```
docker run --rm -it -p 8080:8080 ghcr.io/elide-dev/samples-server-helloworld-jvm
```

**Run the `react-ssr` sample via Docker (JVM):**

```
docker run --rm -it -p 8080:8080 ghcr.io/elide-dev/samples-fullstack-react-ssr-jvm:latest
```

**Run the `react-ssr` sample via Gradle (JVM):**

```
git clone git@github.com:elide-dev/v3.git && cd v3
./gradlew :samples:fullstack:react-ssr:server:run
```

**Run the `react-ssr` sample via Gradle (Native):**

```
git clone git@github.com:elide-dev/v3.git && cd v3
./gradlew :samples:fullstack:react-ssr:server:runNative
```

#### Requirements to build

To build the JVM or JS samples in Kotlin, you will need **JDK 11 or later**. [Zulu](https://www.azul.com/downloads/) is
a good option if you don't have a preferred JVM.

To build native code, you'll need a recent version of [GraalVM](https://www.graalvm.org/downloads/). Make sure to
install the `native-image` tool after initially downloading, which you can do with:

```
gu install native-image espresso
gu rebuild-images
```

Finally, you'll need a recent [Node.js](https://nodejs.org/) runtime if you want to build JS or frontend code. That's
it!

To summarize:

- **For building via Gradle:** JDK11+, any reasonable JVM should work.
- **For building native:** GraalVM (consult compat table for version advice).
- **For building browser/embedded JS:** Recent Node.js toolchain. 16.x+ is recommended.

### Powered-by

Elide is modular. You can mix and match the following technologies in **server**, **client**, or **hybrid/fullstack**
development scenarios:

- [**Kotlin**][1]. Elide is written from the inside out with support for Kotlin/Multiplatform, including native
  platforms. Write once and use the same consistent code across server-side and client-side(s) platforms.

- [**GraalVM**][2]. GraalVM is a JVM and toolchain from Oracle which includes modernized JIT support, cross-language
  [polyglot][10] development in JS, Ruby, Python, and LLVM, and the ability to build native binaries from JVM apps.

- [**Micronaut**][3]. Micronaut is a new JVM-based framework for building server-side applications. Elide leverages
  [Micronaut's](https://docs.micronaut.io/latest/guide/#ioc)
  [dependency injection](https://en.wikipedia.org/wiki/Dependency_injection) and
  [AOP](https://docs.micronaut.io/latest/guide/#aop) features, and transparently works with most add-ons.

- [**React**][4]. React is a popular UI library written for browser and server environments. Elide leverages
  [Kotlin/JS support for React](https://kotlinlang.org/docs/js-get-started.html) for isomorphic UI rendering. CSR and
  SSR modes are supported natively.

- [**Protobuf**][5] / [**gRPC**][6]. Elide leverages cross-platform serialization through KotlinX's
  [`protobuf`](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/formats.md#protobuf-experimental)
  module, and includes **native support for [gRPC Web](https://github.com/grpc/grpc-web)** without running a proxy.

- [**Gradle**][9]. Early support for building multi-platform Kotlin applications via Gradle, including integrated
  support for [Webpack](https://webpack.js.org/)-based frontend builds and [esbuild](https://esbuild.github.io/)-based
  embedded SSR builds.

- [**`esbuild`**][14]. Elide leverages ESBuild when compiling and minifying JS code for the server or browser. ESBuild
  sports extremely fast build times, modern language support, and tunable minification/DCE.

## Version compatibility

The following version matrix indicates tested support across tool and platform versions, including **Java**,
**Kotlin**, **GraalVM**, **Micronaut**, and **React**.

Following this guide is recommended but optional. Depending on the style of development you're doing with Elide, you may
not need some of these components:

| Status                                                          | **Java**    | **Kotlin**   | **GraalVM** | **Micronaut** | **React** | **Protobuf/gRPC**  |
| --------------------------------------------------------------- |-------------|--------------|-------------|---------------| --------- |--------------------|
| ![Status](https://img.shields.io/badge/-experimental-important) | `Java 20`   | `1.9.0-wasm` | `23.0.x`    | `4.0.x`       | `18.x`    | `3.21.11`/`1.56.1` |
| ![Status](https://img.shields.io/badge/-tested-success)         | `Java 20`   | `1.9.0`      | `23.0.x`    | `3.9.x`       | `18.x`    | `3.21.11`/`1.56.1` |
| ![Status](https://img.shields.io/badge/-tested-success)         | `Java 17`   | `1.8.20`     | `22.3.x`    | `3.9.x`       | `18.x`    | `3.21.11`/`1.42.0` |
| ![Status](https://img.shields.io/badge/-tested-success)         | `Java 11`   | `1.7.22`     | `22.3.x`    | `3.5.x`       | `18.x`    | `3.20.1`/`1.46.0`  |
| ![Status](https://img.shields.io/badge/-no%20support-yellow)    | `Java 8-10` | --           | --          | --            | --        | --                 |

If you aren't using certain components on this list, for example, gRPC/Protobuf, you can ignore that column entirely.

## Contributing

Elide is structured as a Gradle codebase, with additional support for Make and Node. Bazel is also coming soon. After
cloning the project, you can run `make help` to get familiar with some standard local dev tasks.

1) **Clone the repo.**
   ```
   git clone git@github.com:elide-dev/v3.git
   ```
2) **Install GraalVM.** You can download CE [here](https://www.graalvm.org/downloads/). Make sure to install the
   `native-image`, `espresso`, and `js` tools after initially downloading, which you can do with:
   ```
   gu install native-image js espresso
   gu rebuild-images
   ```
3) **Explore the Makefile.** The `Makefile` is self-describing. Run `make help` to see what it can do for you:
  ```
  Elide:
  api-check                      Check API/ABI compatibility with current changes.
  build                          Build the main library, and code-samples if SAMPLES=yes.
  clean-cli                      Clean built CLI targets.
  clean-docs                     Clean documentation targets.
  clean-site                     Clean site targets.
  clean                          Clean build outputs and caches.
  cli-local                      Build the Elide command line tool and install it locally (into ~/bin, or LOCAL_CLI_INSTALL_DIR).
  cli-release                    Build an Elide command-line release.
  cli                            Build the Elide command-line tool (native target).
  distclean                      DANGER: Clean and remove any persistent caches. Drops changes.
  docs                           Generate docs for all library modules.
  forceclean                     DANGER: Clean, distclean, and clear untracked files.
  help                           Show this help text ('make help').
  image-base-alpine              Build base Alpine image.
  image-base                     Build base Ubuntu image.
  image-gvm17                    Build GVM17 builder image.
  image-jdk17                    Build JDK17 builder image.
  image-native-alpine            Build native Alpine base image.
  image-native                   Build native Ubuntu base image.
  image-runtime-jvm17            Build runtime GVM17 builder image.
  images                         Build all Docker images.
  publish                        Publish a new version of all Elide packages.
  relock-deps                    Update dependency locks and hashes across Yarn and Gradle.
  reports                        Generate reports for tests, coverage, etc.
  serve-docs                     Serve documentation locally.
  serve-site                     Serve Elide site locally.
  site                           Generate the static Elide website.
  test                           Run the library testsuite, and code-sample tests if SAMPLES=yes.
  update-deps                    Perform interactive dependency upgrades across Yarn and Gradle.
  update-jdeps                   Interactively update Gradle dependencies.
  update-jsdeps                  Interactively update Yarn dependencies.
  ```
4) **Take a look at the Makefile flags.** The `Makefile` defines flags at the top of the source code:
  ```
  # Flags that control this makefile, along with their defaults:
  #
  # DEBUG ?= no
  # STRICT ?= yes
  # RELEASE ?= no
  # JVMDEBUG ?= no
  # NATIVE ?= no
  # CI ?= no
  # DRY ?= no
  # SCAN ?= no
  # IGNORE_ERRORS ?= no
  # RELOCK ?= no
  ```

When committing to Elide, make sure to follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)
standard. This helps us keep changelogs clean and obvious.

## Reports

### Licensing

Elide itself is licensed [under MIT](LICENSE) as of November 2022. Dependencies are scanned for license compatibility;
the report is available via FOSSA:

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Felide-dev%2Fv3.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Felide-dev%2Fv3?ref=badge_large)

### Coverage

Code coverage is continuously reported to [Codecov](https://app.codecov.io/gh/elide-dev/v3) and
[SonarCloud](https://sonarcloud.io/project/overview?id=elide-dev_v3):

[![Coverage grid](https://codecov.io/gh/elide-dev/v3/branch/v3/graphs/tree.svg?token=FXxhJlpKG3)](https://codecov.io/gh/elide-dev/v3)


[1]: https://kotlinlang.org/
[2]: https://graalvm.org/
[3]: https://micronaut.io/
[4]: https://reactjs.org/
[5]: https://developers.google.com/protocol-buffers
[6]: https://grpc.io/
[7]: https://developers.google.com/closure
[8]: https://bazel.build/
[9]: https://gradle.org/
[10]: https://developers.google.com/speed/pagespeed/module
[11]: https://github.com/sgammon/elide/tree/master
[12]: https://github.com/sgammon/elide
[13]: https://buf.build
[14]: https://esbuild.github.io/

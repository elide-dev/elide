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

[![Build](https://github.com/elide-dev/elide/actions/workflows/build.ci.yml/badge.svg)](https://github.com/elide-dev/elide/actions/workflows/build.ci.yml)
[![codecov](https://codecov.io/gh/elide-dev/elide/branch/v3/graph/badge.svg?token=FXxhJlpKG3)](https://codecov.io/gh/elide-dev/elide)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=reliability_rating&token=7e7d03a5cb8a12b7297eb6eedf5fe9b93ade6d75)](https://sonarcloud.io/summary/new_code?id=elide-dev_v3)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=security_rating&token=7e7d03a5cb8a12b7297eb6eedf5fe9b93ade6d75)](https://sonarcloud.io/summary/new_code?id=elide-dev_v3)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=sqale_rating&token=7e7d03a5cb8a12b7297eb6eedf5fe9b93ade6d75)](https://sonarcloud.io/summary/new_code?id=elide-dev_v3)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Felide-dev%2Fv3.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Felide-dev%2Fv3?ref=badge_shield)
[![Java 20](https://img.shields.io/badge/Java-20-blue.svg?logo=oracle)](https://openjdk.org/projects/jdk/19/)
[![GraalVM](https://img.shields.io/badge/GraalVM-23.x.x-blue.svg?logo=oracle)](https://www.graalvm.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.20-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Kotlin/JS. IR supported](https://img.shields.io/badge/kotlin-IR-yellow?logo=kotlin&logoColor=yellow)](https://kotl.in/jsirsupported)
[![ECMA](https://img.shields.io/badge/ECMA-2020-blue.svg?logo=javascript)](https://262.ecma-international.org/11.0/)
[![OpenSSF Best Practices](https://bestpractices.coreinfrastructure.org/projects/7690/badge)](https://bestpractices.coreinfrastructure.org/projects/7690)

Latest version: `1.0.0-alpha7`

<hr />

Elide is a cloud-first polyglot runtime for developing fast web applications. It aims to reduce the barriers between languages and improve performance of existing code, without forcing developers to abandon their favorite APIs and libraries.

> [!IMPORTANT]
> Elide is still in alpha, some features are not fully supported and others may fail on certain environments.

## Using the runtime binaries

You can install the runtime by running:

```shell
curl -sSL --tlsv1.2 "dl.elide.dev/cli/install.sh" | bash -s -
```

After installation, you can run `elide help` or `elide info` to see more information.

## Features

The Elide CLI supports JavaScript out of the box, and includes experimental support for Python, while more languages such as Ruby are planned for future releases.
### Environment files

Elide provides `.env` files support at the runtime level, without the need for manual configuration or third-party packages. Environment variables from `.env` files will be picked up and injected into the application using a language-specific API (e.g. `process.env` in JavaScript).

### Server engine

Let's see an example, the following JavaScript application configures a built-in HTTP server and adds a route handler with path variables:

```javascript
// access the built-in HTTP server engine
const app = Elide.http

// register a route handler
app.router.handle("GET", "/hello/:name", (request, response, context) => {
  // respond using the captured path variables
  response.send(200, `Hello, ${context.params.name}`)
})

// configure the server binding options
app.config.port = 3000

// receive a callback when the server starts
app.config.onBind(() => {
  console.log(`Server listening at "http://localhost:${app.config.port}"! 🚀`)
})

// start the server
app.start()
```

The server can be started with:

```shell
> elide serve app.js
> elide 17:43:09.587 DEBUG Server listening at "http://localhost:3000"! 🚀
```

> [!IMPORTANT]
> The Elide HTTP intrinsics are under active development and provide only limited features. We are actively working to improve performance and support more use cases.

### Planned features

The following features are currently planned or under construction:

- **Secret management**: access secrets as environment variables visible only to your application, decouple your code from secret management SDKs and let the runtime handle that complexity for you.
- **Isolated I/O**: use pre-packaged archives to provide read-only, sealed Virtual File Systems, allowing access to the host File System only where needed.
- **Built-in telemetry**: managed, configurable integration with telemetry APIs.
- **Native distributions**: build your app into a truly native binary using GraalVM's [Native Image](https://www.graalvm.org/22.0/reference-manual/native-image/) technology, ship optimized applications without including a runtime in your container.

## Use with server frameworks

Elide integrates with [Micronaut]() to provide Server-Side and Hybrid rendering options with very high performance, by running JavaScript code inside your JVM server:

```kotlin
// build.gradle.kts
implementation("dev.elide:elide-server:1.0.0-alpha7")
```

or if you use Groovy:

```groovy
// build.gradle
implementation 'dev.elide:elide-server:1.0.0-alpha7'
```

See our [samples](samples) to explore the features available when integrating with server frameworks, the following code for a server application uses React with Server-Side Rendering:

```kotlin
// server/App.kt

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

By evaluating the JavaScript code built using a Kotlin/JS browser app:

```kotlin
// client/main.kt

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

> [!NOTE]
> More versatile integration with frameworks like [Micronaut](https://micronaut.io) and [Ktor](https://ktor.io/) is planned but not yet supported. The API and packages used for these integrations may change as we add more features.

## Building a custom runtime

If you are building a JVM application that runs guest code in one of the languages supported by Elide, you can use the Runtime DSL to configure your own embedded polyglot engine:

```kotlin
implementation("dev.elide:elide-graalvm:1.0.0-alpha7")
```

or for Groovy scripts:

```groovy
implementation 'dev.elide:elide-graalvm:1.0.0-alpha7'
```

The DSL is used internally by the Elide binaries and by the SSR packages and provides a simplified API to harness the power of the underlying [GraalVM](https://graalvm.org) engine:

```kotlin
// configure and create a new engine
val engine = PolyglotEngine {
  // add support for the JavaScript language
  install(JavaScript) {
    // enable ESM support
    esm = true
    
    // include custom bindings into the engine, these
    // will be available as top level symbols
    bindings {
      put("version", "1.0.0")
    }
  }
}

// obtain a context to evaluate source code
val context = engine.acquire()

// evaluate guest JavaScript code
context.javascript("""
  console.log(`Hello from custom runtime v${version}`)
""")
```

> [!WARNING]
> The Elide Runtime DSL is intended for advanced use cases, and while we try not to break compatibility between releases, there are no guarantees.

## Version compatibility

The following version matrix indicates tested support across tool and platform versions, including **Java**,
**Kotlin**, **GraalVM**, **Micronaut**, and **React**.

Following this guide is recommended but optional. Depending on the style of development you're doing with Elide, you may
not need some of these components:

| Status                                                          | **Java**    | **Kotlin**    | **GraalVM** | **Micronaut** | **React** | **Protobuf/gRPC**  |
|-----------------------------------------------------------------|-------------|---------------|-------------|---------------|-----------|--------------------|
| ![Status](https://img.shields.io/badge/-experimental-important) | `Java 20`   | `1.9.20-Beta2` | `23.0.x`    | `4.0.x`       | `18.x`    | `3.21.11`/`1.56.1` |
| ![Status](https://img.shields.io/badge/-tested-success)         | `Java 20`   | `1.9.10`      | `23.0.x`    | `3.10.x`      | `18.x`    | `3.21.11`/`1.56.1` |
| ![Status](https://img.shields.io/badge/-tested-success)         | `Java 17`   | `1.8.20`      | `22.3.x`    | `3.9.x`       | `18.x`    | `3.21.11`/`1.42.0` |
| ![Status](https://img.shields.io/badge/-tested-success)         | `Java 11`   | `1.7.22`      | `22.3.x`    | `3.5.x`       | `18.x`    | `3.20.1`/`1.46.0`  |
| ![Status](https://img.shields.io/badge/-no%20support-yellow)    | `Java 8-10` | --            | --          | --            | --        | --                 |

If you aren't using certain components on this list, for example, gRPC/Protobuf, you can ignore that column entirely.
## Reports

### Licensing

Elide itself is licensed [under MIT](LICENSE) as of November 2022. Dependencies are scanned for license compatibility;
the report is available via FOSSA:

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Felide-dev%2Fv3.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Felide-dev%2Fv3?ref=badge_large)

### Coverage

Code coverage is continuously reported to [Codecov](https://app.codecov.io/gh/elide-dev/v3) and
[SonarCloud](https://sonarcloud.io/project/overview?id=elide-dev_v3):

[![Coverage grid](https://codecov.io/gh/elide-dev/elide/branch/v3/graphs/tree.svg?token=FXxhJlpKG3)](https://codecov.io/gh/elide-dev/v3)

## Contributing

Issue reports and pull requests are welcome! See our [contribution guidelines](CONTRIBUTING.md) or join our [discord community](https://elide.dev/discord) and let us know which features you would like to see implemented, or simply participate in the discussions to help shape the future of the project.


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

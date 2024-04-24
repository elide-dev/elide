# Module server

`elide-server` provides support for server apps built with the Elide Framework on JVM.

## Built with Micronaut

By default, the server implementation used by Elide is based on [Micronaut](https://micronaut.io/), and underneath that,
[Netty](https://netty.io/). This is a very fast and lightweight combination, and it's also very easy to use.

This also means you can use most of the [Micronaut modules](https://launch.micronaut.io) with your Elide server app out
of the box, such as:

- [Micronaut Security](https://micronaut-projects.github.io/micronaut-security/latest/guide/index.html)
- [Micronaut Data](https://micronaut-projects.github.io/micronaut-data/latest/guide/index.html)
- [Micronaut gRPC](https://micronaut-projects.github.io/micronaut-grpc/latest/guide/index.html)

## Installation

The `elide-server` package is provided via
[Maven Central](https://search.maven.org/search?q=g:dev.elide%20AND%20a:elide-server), and also via Elide's own
[GitHub Packages](https://github.com/elide-dev/elide/packages/1933415) repository.

**Via Gradle (Catalog):**

```kotlin
implementation(framework.elide.server)
```

**Via Gradle (Kotlin DSL):**

```kotlin
implementation("dev.elide:elide-server")
```

**Via Gradle (Groovy DSL):**

```kotlin
implementation "dev.elide:elide-server"
```

**Via Maven:**

```xml
<dependency>
  <groupId>dev.elide</groupId>
  <artifactId>elide-server</artifactId>
</dependency>
```

## Getting Started

All of the main [Elide server code samples](https://github.com/elide-dev/elide/tree/main/samples) use the `elide-server`
package. Here is a barebones example ([full code](https://github.com/elide-dev/elide/blob/8129124d52f8b25f5baa6fe7231683afb09f4584/samples/server/helloworld/src/main/kotlin/helloworld/App.kt#L14-L32)):

```kotlin
/** Self-contained application example, which serves an HTML page that says "Hello, Elide!". */
object App {
  /** GET `/`: Controller for index page. */
  @Page(name = "index") class Index : PageController() {
    @Get("/") suspend fun index() = html {
      head {
        title { +"Hello, Elide!" }
      }
      body {
        strong { +"Hello, Elide!" }
      }
    }
  }

  /** Main entrypoint for the application. */
  @JvmStatic fun main(args: Array<String>) {
    build().args(*args).start()
  }
}
```

This particular sample is embedded within the Elide codebase, and can be run with:

```
./gradlew -PbuildSamples=true :samples:server:helloworld:run
```

## Server Features

- Implemented with [Micronaut HTTP v4](https://docs.micronaut.io/latest/guide/#httpServer) and [Netty](https://netty.io/)
- Opinionated Micronaut and Netty defaults for interactive web-facing applications
- Integration with [Elide's Gradle plugin](https://github.com/elide-dev/buildtools) for static asset management and SSR
- Supports content negotiation, ETags, conditional responses
- Intelligent build-time compression support (Brotli, Gzip, etc.)
- Execute JavaScript, Python, Ruby, and other languages via Elide's SSR tools
- Share objects between languages for SSR calls

# Package elide.server

Top-level types, services, aliases, and interfaces which define the Elide Server API.

# Package elide.server.annotations

Server-specific JVM annotations, for use with Jakarta-compliant DI containers.

# Package elide.server.assets

Logic for serving static assets embedded within Elide apps.

# Package elide.server.cfg

Configuration structures for the Elide Server API.

# Package elide.server.controller

Controller base classes for implementing server-side logic.

# Package elide.server.controller.builtin

Built-in controllers which handle tasks like static asset serving.

# Package elide.server.http

HTTP serving logic and utilities.

# Package elide.server.runtime

Server-side runtime logic for Elide apps; implements features like polyglot SSR.

# Package elide.server.runtime.jvm

JVM-specific server-side runtime logic for Elide apps.

# Package elide.server.ssr

Public SSR API types for Elide server apps. These types are usually used from controllers.

# Package kotlinx.html

Extensions to KotlinX HTML which enable suspension during rendering.

# Package kotlinx.html.tagext

Extensions to KotlinX HTML tags which enable suspension during rendering.

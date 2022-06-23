
## `@elide` (v3)

[![Build](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml/badge.svg)](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=reliability_rating&token=7e7d03a5cb8a12b7297eb6eedf5fe9b93ade6d75)](https://sonarcloud.io/summary/new_code?id=elide-dev_v3)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=security_rating&token=7e7d03a5cb8a12b7297eb6eedf5fe9b93ade6d75)](https://sonarcloud.io/summary/new_code?id=elide-dev_v3)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=sqale_rating&token=7e7d03a5cb8a12b7297eb6eedf5fe9b93ade6d75)](https://sonarcloud.io/summary/new_code?id=elide-dev_v3)


[![Kotlin](https://img.shields.io/badge/kotlin-1.7.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![GraalVM](https://img.shields.io/badge/graal-22.1.x-blue.svg?logo=oracle)](https://www.graalvm.org/)
[![Micronaut](https://img.shields.io/badge/micronaut-3.5.x-blue.svg?logo=java)](https://micronaut.io)
[![React](https://img.shields.io/badge/react-18.x-blue.svg?logo=react)](https://reactjs.org/)


_**Elide is under construction. You can also browse to [`v2`][12] or [`v1`][11].**_

<hr />

Elide is a polyglot application framework which brings together some of the best tools and  techniques available today
for effective and robust application development.


### What do you mean by _application framework_?

Elide could be characterized as a _meta-framework_, in the sense that it is largely powered by mature and well-supported
software under the hood. Via a thin combination of build tooling and runtime interfaces, the developer is able to
leverage these tools together without needing to stitch them together in brittle and unreliable ways.


#### Cross-platform features

- **Primitives.** Access the entire [Kotlin Standard Library](), with additional extensions and a the full suite of
  [KotlinX]() extensions, including [`serialization`](https://kotlinlang.org/docs/serialization.html),
  [`atomicfu`](https://github.com/Kotlin/kotlinx.atomicfu),
  [`coroutines`](https://github.com/Kotlin/kotlinx.coroutines), and
  [`datetime`](https://github.com/Kotlin/kotlinx-datetime).

- **Logging.** Use an identical interface across platforms, which calls into the best layer available for that system.
  For example, on the JVM logging calls into `slf4j`, and in JavaScript, `console.*`.

- **Observability.** Generate uniform logs across platforms, which correlate and provide a fully observable picture of
  application behavior.

- **Data modeling.** Express data models once, with support for databasing, serialization/de-serialization, and data
  exchange via RESTful APIs, gRPC, or gRPC-Web.


## Distinguishing features

- **Native development with Kotlin.** Write your core application logic and models once, and share them across platforms
  transparently. Leverage [Kotest](https://kotest.io) for cross-platform write-once-run-everywhere testing.

- **Isomorphic SSR with React.** Write your UI in React, using JavaScript, TypeScript, or Kotlin. Package it for serving
  via [client-side rendering or hybrid isomorphic rendering](https://web.dev/rendering-on-the-web/) directly from your
  Kotlin server.

- **Transparent API services with gRPC.** Cross-service AOP with
  [gRPC Dekorator](https://github.com/mottljan/grpc-dekorator), with the native ability to serve data to native clients
  via raw [gRPC][6] or native [gRPC-Web](https://github.com/grpc/grpc-web).

- **Model-driven development.** Write your models once, and use them everywhere, across platforms, **without copying**,
  **without glue-code**, and **without DTOs**. Via [Protobuf][5] and
  [Kotlin data classes](https://kotlinlang.org/docs/data-classes.html), the same Elide model can be used with your
  database, your API, and your UI.


### Powered-by

Elide is modular. You can mix and match the following technologies in **server**, **client**, or **hybrid/fullstack**
development scenarios:

- [**Kotlin**][1]. Elide is written from the inside-out with support for Kotlin/Multiplatform, including native
  platforms. This means you can use the same consistent code across the server-side and client-side(s) of your app.

- [**GraalVM**][2]. GraalVM is a JVM and toolchain from Oracle which includes modernized JIT support, cross-language
  [polyglot][10] development in JS, Ruby, Python, and LLVM, and the ability to build native binaries from JVM apps.

- [**Micronaut**][3]. Micronaut is a new JVM-based framework for building server-side applications. Elide leverages
  [Micronaut's](https://docs.micronaut.io/latest/guide/#ioc)
  [dependency injection](https://en.wikipedia.org/wiki/Dependency_injection) and
  [AOP](https://docs.micronaut.io/latest/guide/#aop) features, and transparently works with most add-ons.

- [**React**][4]. React is a popular UI library, written for browser and server environments. Elide leverages
  [Kotlin/JS support for React](https://kotlinlang.org/docs/js-get-started.html) for isomorphic UI rendering. CSR and
  SSR modes are supported natively.

- [**Protobuf**][5] / [**gRPC**][6]. Elide leverages cross-platform serialization through KotlinX's
  [`protobuf`](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/formats.md#protobuf-experimental)
  module, and includes **native support for [gRPC Web](https://github.com/grpc/grpc-web)** without running a proxy.
  Enforce API keys and traceability without [ESPv2](https://github.com/GoogleCloudPlatform/esp-v2).

- [**Closure**][7]. Use the Closure Tools family of software to develop performant and low-level JavaScript apps, which
  are served on-the-fly directly from your application JAR. Render Soy templates server-side with Google's blazing-fast
  [Soy Sauce](https://github.com/google/closure-templates/blob/master/documentation/dev/adv-java.md) runtime, via
  pre-compiling templates to Java bytecode.

- [**Bazel**][8]. Built-in support for Bazel. Load directly from a tarball, build only what you use. Extensive suite of
  utility macros and rules.

- [**Gradle**][9]. Early support for building multi-platform Kotlin applications via Gradle, including integrated
  support for [Webpack](https://webpack.js.org/)-based frontend builds and [esbuild](https://esbuild.github.io/)-based
  embedded SSR builds.

- [**PSOL**][10]. Early support for native integration with the
  [Pagespeed Optimization Libraries](https://developers.google.com/speed/pagespeed/module), enabling on-the-fly
  optimization of server-side content.


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

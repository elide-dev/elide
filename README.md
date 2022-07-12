# `elide` (v3)

_elide: verb. to omit (a sound or syllable) when speaking. to join together; to merge._

<hr />

[![Build](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml/badge.svg)](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml)
[![codecov](https://codecov.io/gh/elide-dev/v3/branch/v3/graph/badge.svg?token=FXxhJlpKG3)](https://codecov.io/gh/elide-dev/v3)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=reliability_rating&token=7e7d03a5cb8a12b7297eb6eedf5fe9b93ade6d75)](https://sonarcloud.io/summary/new_code?id=elide-dev_v3)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=security_rating&token=7e7d03a5cb8a12b7297eb6eedf5fe9b93ade6d75)](https://sonarcloud.io/summary/new_code?id=elide-dev_v3)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=elide-dev_v3&metric=sqale_rating&token=7e7d03a5cb8a12b7297eb6eedf5fe9b93ade6d75)](https://sonarcloud.io/summary/new_code?id=elide-dev_v3)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Felide-dev%2Fv3.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Felide-dev%2Fv3?ref=badge_shield)


[![Kotlin](https://img.shields.io/badge/Kotlin-1.7.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Kotlin](https://img.shields.io/badge/Java-17-blue.svg?logo=java)](https://openjdk.org/projects/jdk/17/)
[![ECMA](https://img.shields.io/badge/ECMA-2020-blue.svg?logo=javascript)](https://reactjs.org/)
[![GraalVM](https://img.shields.io/badge/GraalVM-22.1.x-blue.svg?logo=oracle)](https://www.graalvm.org/)
[![Micronaut](https://img.shields.io/badge/Micronaut-3.5.x-blue.svg?logo=java)](https://micronaut.io)
[![React](https://img.shields.io/badge/React-18.x-blue.svg?logo=react)](https://reactjs.org/)


_**Elide is under construction. You can also browse to [`v2`][12] or [`v1`][11].**_

<hr />

Elide is a **Kotlin/Multiplatform meta-framework** for **rapid, cross-platform application development**. Write once in
Kotlin and deploy everywhere: your server, the browser, and native app targets.


### What do you mean by _meta-framework_?

Elide could be characterized as a _meta-framework_ in the sense that it is powered entirely by mature and well-supported
software under the hood. Via a thin combination of build tooling and runtime interfaces, the developer can
leverage these tools together without worrying about performance or compatibility.


#### Cross-platform features

- **Primitives.** Access the entire [Kotlin Standard Library](), with additional sugar added, and the full suite of
  [KotlinX]() extensions including [`serialization`](https://kotlinlang.org/docs/serialization.html),
  [`atomicfu`](https://github.com/Kotlin/kotlinx.atomicfu),
  [`coroutines`](https://github.com/Kotlin/kotlinx.coroutines), and
  [`datetime`](https://github.com/Kotlin/kotlinx-datetime).

- **Logging.** Use an identical interface across platforms, which calls into the best layer available for that system.
  For example, on the JVM, logging calls into `slf4j`, and in JavaScript, `console.*`.

- **Observability.** Generate uniform logs across platforms, which correlate and provide a fully observable picture of
  application behavior.

- **Data modeling.** Express data models once, with support for databasing, serialization/de-serialization, and data
  exchange via RESTful APIs, gRPC, or gRPC-Web.


## Distinguishing features

- **Native development with Kotlin.** Write your core application logic and models once, and share them across platforms
  transparently. Leverage [Kotest](https://kotest.io) for cross-platform, write-once-run-native testing.

- **Isomorphic SSR with React.** Write your UI in React, using JavaScript, TypeScript, or Kotlin. Package it for serving
  via [client-side rendering or hybrid isomorphic rendering](https://web.dev/rendering-on-the-web/) directly from your
  Kotlin server.

- **Model-driven development.** Write your models once, and use them everywhere, across platforms, **without copying**,
  **without glue-code**, and **without DTOs**. Via [Protobuf][5] and
  [Kotlin data classes](https://kotlinlang.org/docs/data-classes.html), the same Elide model is code-generated for use
  with your database, API, and UI.


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

- [**Closure**][7]. Use the Closure Tools family of software to develop performant and low-level JavaScript apps that
  are served directly from your application JAR. Render Soy templates server-side with Google's blazing-fast
  [Soy Sauce](https://github.com/google/closure-templates/blob/master/documentation/dev/adv-java.md) runtime, via
  pre-compiling templates to Java bytecode.

- [**Bazel**][8]. Built-in support for Bazel. Load directly from a tarball; build only what you use. Extensive suite of
  utility macros and rules.

- [**Gradle**][9]. Early support for building multi-platform Kotlin applications via Gradle, including integrated
  support for [Webpack](https://webpack.js.org/)-based frontend builds and [esbuild](https://esbuild.github.io/)-based
  embedded SSR builds.


## Version compatibility

The following version matrix indicates tested support across tool and platform versions, including **Java**,
**Kotlin**, **GraalVM**, **Micronaut**, and **React**.

Following this guide is recommended but optional. Depending on the style of development you're doing with Elide, you may
not need some of these components:

| Status                                                          | **Java**  | **Kotlin** | **GraalVM** | **Micronaut** | **React** | **Protobuf/gRPC** |
|-----------------------------------------------------------------|-----------|------------|-------------|---------------|-----------|-------------------|
| ![Status](https://img.shields.io/badge/-experimental-important) | `Java 19` | `1.7.0`    | `22.1.x`    | `3.5.x`       | `18.x`    | `3.20.1`/`1.46.0` |
| ![Status](https://img.shields.io/badge/-tested-success)         | `Java 17` | `1.7.0`    | `22.1.x`    | `3.5.x`       | `18.x`    | `3.20.1`/`1.46.0` |
| ![Status](https://img.shields.io/badge/-tested-success)         | `Java 11` | `1.7.0`    | `22.1.x`    | `3.5.x`       | `18.x`    | `3.20.1`/`1.46.0` |
| ![Status](https://img.shields.io/badge/-no%20support-yellow)    | `Java 8`  | --         | --          | --            | --        | --                |


If you aren't using certain components on this list, for example, gRPC/Protobuf, you can ignore that column entirely.


## Platform Support

Elide itself generally supports any platform supported by GraalVM. This includes **Linux**, **macOS**, and **Windows**.
Client-side, all major browsers are supported, along with Android and iOS via Kotlin/Native.

Please consult the table below for a complete list of supported platforms and architectures:

| Build | OS             | Arch     | Libc   | Supported | Notes                  |
|-------|:---------------|----------|--------|:---------:|:-----------------------|
| [![Build](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml/badge.svg)](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml) | Linux (Ubuntu) | `x86_64` | `libc` | ✅ | Default JVM platform   |
| [![Build](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml/badge.svg)](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml) | macOS          | `x86_64` | `libc` | ✅ | Supported dev platform |
| [![Build](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml/badge.svg)](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml) | macOS          | `arm64`  | `libc` | ✅ | Supported dev platform |
| [![Build](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml/badge.svg)](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml) | Linux (Alpine) | `x86_64` | `musl` | ☢️ | _Experimental_         |
| [![Build](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml/badge.svg)](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml) | Linux (Ubuntu) | `arm64`  | `libc` | ☢️ | _Experimental_         |
| [![Build](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml/badge.svg)](https://github.com/elide-dev/v3/actions/workflows/build.ci.yml) | Windows        | `x86_64` | N/A    | ☢️ | _Experimental_         |

Bazel, Gradle, GraalVM, and Kotlin/Native must support a given platform for Elide to work across architectures.


### **"What about [x] functionality that I want/need?"**

Java, and, by extension, Kotlin, has **one of the broadest and most active software ecosystems** on the planet, closely
followed by NodeJS, via NPM.

Elide supports both and allows you to access software written for both. Here are some examples to get you going.

- _I want to..._
  - **Server-side (Micronaut)**
    - ... localize my app: [Micronaut i18n](https://guides.micronaut.io/latest/localized-message-source-gradle-kotlin.html)
    - ... build an event-driven app: [RabbitMQ](https://guides.micronaut.io/latest/micronaut-rabbitmq.html), [nats.io](https://micronaut-projects.github.io/micronaut-nats/latest/guide/)
    - ... connect to my SQL database: [Hibernate](https://guides.micronaut.io/latest/micronaut-jpa-hibernate-gradle-java.html), [Exposed](https://github.com/JetBrains/Exposed)
    - ... connect to my NoSQL database: Micronaut supports [MongoDB](https://micronaut-projects.github.io/micronaut-mongodb/latest/guide/) and [Cassandra](https://micronaut-projects.github.io/micronaut-cassandra/latest/guide), Elide additionally provides [Firestore](https://firebase.google.com/docs/firestore) and [Redis](https://redis.io)
    - ... send emails to users: [Micronaut Email](https://micronaut-projects.github.io/micronaut-email/latest/guide/) (Sendgrid, Postmark, SES, Mailjet, JavaMail)
    - ... use a templating engine: [Micronaut Views](https://micronaut-projects.github.io/micronaut-views/latest/guide/) (Soy, Thymeleaf, Velocity, Freemarker, Rocker, Handlebars, JTE)
    - ... cache intelligently: [Micronaut Cache](https://guides.micronaut.io/latest/micronaut-cache-gradle-kotlin.html), (Ehcache, Redis, Hazelcast, Infinispan)
    - ... authorize my users: [Micronaut Security](https://micronaut-projects.github.io/micronaut-security/latest/guide/) (JWT, OAuth2, OIDC, LDAP, X.509)
    - ... build containers: compatibility with [Docker Gradle Plugin](https://github.com/bmuschko/gradle-docker-plugin), [`rules_docker`](https://github.com/bazelbuild/rules_docker), and [Jib](https://github.com/GoogleContainerTools/jib)
    - ... host on [Kubernetes](https://kubernetes.io): [Micronaut Kubernetes](https://micronaut-projects.github.io/micronaut-kubernetes/snapshot/guide/), [Skaffold](https://skaffold.dev) is supported
  - **Serve an API**
    - ... you can use [gRPC](https://grpc.io): [Micronaut gRPC](https://micronaut-projects.github.io/micronaut-grpc/snapshot/guide/index.html), with native [gRPC-Web](https://github.com/grpc/grpc-web) support coming soon
    - ... you can use [GraphQL](https://graphql.org): [Micronaut GraphQL](https://micronaut-projects.github.io/micronaut-graphql/latest/guide/) (RESTful or via WebSockets)
    - ... you can use [OpenAPI](https://www.openapis.org): [Micronaut OpenAPI](https://micronaut-projects.github.io/micronaut-openapi/latest/guide/index.html) can generate your specs directly from your code
  - **Build a web app**
    - ... you can use pure Kotlin: [`kotlinx.html`](https://github.com/Kotlin/kotlinx.html), [Kotlin/JS](https://kotlinlang.org/docs/js-get-started.html)
    - ... you can use TypeScript or pure JS: [`esbuild`](https://esbuild.github.io/) can be used to build both bundle types
    - ... you can use [Angular](https://angular.io/): SSR is supported via [Angular Universal](https://angular.io/guide/universal)
    - ... you can use [Vue](https://vuejs.org/): SSR is supported [out of the box](https://vuejs.org/guide/scaling-up/ssr.html)
    - ... you can use [J2CL](https://github.com/google/j2cl) & [Closure][7]: rule-level support for [Elemental2](https://github.com/google/elemental2) as well
  - **Build a mobile app**
    - (tbd)


## About this framework

Elide has existed in different forms for over a decade. It was first released under the name `canteen` in 2011 as a
Python package. Later, the code evolved into Bazel/Java, then Bazel/Kotlin, and now Bazel/Gradle/Kotlin.

These frameworks were distinct from each other, written for a moment in time, but they shared authors, design patterns,
and goals:

- **The developer should have to write as little code as possible** without resorting to convention. Elide, in this way,
  is partly a reaction to the drawbacks of Rails-style architectures (and Next.js, which came much later).

- **If it builds, it should work.** As much as possible, type checking and code-gen should be leveraged where a human
  developer adds little or no value. Aligning types is a detailed and error-prone process, which is well suited for a
  computer.

- **A thing is just a thing. Not what is said of that thing.** Data is the effective king of runtime. An application's
  concepts should be strongly expressed as models, and the developer _should not have to repeat themselves_ across
  platforms to actualize or interchange expressions of those concepts.


### History


#### Adopters

Collectively, over a span of 10+ years, Elide has served millions of requests per month on behalf of the following
organizations (newest first):

- **[Cookies](https://cookies.co).** Elide powered all customer-facing digital properties, including the main Cookies
  site and e-commerce experience, with traffic reaching millions of users and unique hits per month.

- **[Bloombox](https://github.com/bloombox).** Elide powered the APIs underlying Bloombox's B2B retail application,
  scaling to millions of requests per month.

- **[Ampush](https://ampush.com).** Elide powered internal ad-tech systems at Ampush, which reached millions of events
  and requests per day.

- **[Keen IO](https://keen.io).** Elide powered the Keen website for a time, which reached millions of developers across
  the span of its early use in Python.



#### Timeline

- [2012] **`canteen`**: Leveraged Python meta-classes and cooperative greenlets after the release of
  [`gevent`](http://www.gevent.org/). "Holds more water than a [flask](https://pypi.org/project/Flask/)." Inspired
  heavily by [`tipfy`](https://github.com/moraes/tipfy). Created at [Keen IO](https://keen.io) and adopted to run
  production systems at [Ampush Media](https://ampush.com).

- [2015]: **`gust`**: Written to integrate Kotlin with Bazel, ultimately with support for Soy, J2CL, Protobuf,
  and the Closure ecosystem of tools (via [`rules_closure`](https://github.com/bazelbuild/rules_closure), shout out to
  @jart!). Powered production systems at Momentum Ideas and [Bloombox](https://github.com/bloombox).

- [2020]: **`gust` (`elide` v1)**: Rewritten in Java/Kotlin, with native support for Bazel, to continue `gust`'s
  evolution for production use at [Cookies](https://cookies.co).

- [2021]: **`elide` v2**: New model layer support with the addition of Redis, Firestore, and Spanner, to support the
  launch of [Cookies](https://cookies.co)' integrated e-commerce experience.

- [2022-now]: **`elide` v3**: Complete rewrite into pure Kotlin to leverage Kotlin Multiplatform, with native support
  for polyglot development via GraalVM, gRPC/Protobuf, and React. Build tooling support through Bazel and Gradle.


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


## License
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Felide-dev%2Fv3.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Felide-dev%2Fv3?ref=badge_large)

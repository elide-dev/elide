# Module ssr

`elide-ssr` provides cross-platform types for polyglot SSR (_Server-side Rendering_) operations.

## What is SSR?

SSR stands for _Server-side Rendering_, and refers to an architecture where the content of a given webpage is returned
directly from the server, rather than being rendered on the client. This is in contrast to _Client-side Rendering_,
where a JavaScript bundle is served which dynamically renders the page on the client's end.

SSR is famously powerful in the JVM ecosystem, and even in the JS ecosystem, but there is no easy way to use SSR-style
rendering in a polyglot environment. This is where Elide comes in.

Using GraalVM, Elide can run the same code on both the server and the client, allowing for a seamless isomorphic SSR
experience and development setup without resorting to multiple codebases or runtimes (e.g. Node).

## How SSR works in Elide

SSR in Elide is powered by [GraalVM](https://graalvm.org), like all language interoperability. The `elide-ssr` module
defines types which are used in both frontend and backend modules, like `elide-frontend` an `elide-server.

Developers should implement controllers based on the classes within this module, and package their code using compatible
build tooling (e.g. Elide's Gradle plugin). At runtime, assets are resolved and served from an embedded blob.

## Installation

The `elide-ssr` module is provided via
[Maven Central](https://search.maven.org/search?q=g:dev.elide%20AND%20a:elide-ssr), and also via Elide's own
[GitHub Packages](https://github.com/orgs/elide-dev/packages?ecosystem=maven&q=core&tab=packages&ecosystem=maven&q=elide-ssr)
repository.

**Via Gradle (Catalog):**

```kotlin
implementation(framework.elide.ssr)
```

**Via Gradle (Kotlin DSL):**

```kotlin
implementation("dev.elide:elide-ssr")
```

**Via Gradle (Groovy DSL):**

```kotlin
implementation "dev.elide:elide-ssr"
```

**Via Maven:**

```xml
<dependency>
  <groupId>dev.elide</groupId>
  <artifactId>elide-ssr-jvm</artifactId>
</dependency>
```

# Package elide.ssr

Core type definitions used by the SSR engine, and shared across client and server targets.

# Package elide.ssr.nnotations

Pure-Kotlin annotations which are used by the SSR engine. For example, the `Props` annotation is provided for bridging
values to React.

# Package elide.ssr.type

Defines type layouts for polyglot (shared) types related to SSR.

# Package elide.vm.annotations

Defines low-level annotations like `Polyglot`, which surfaces types for cross-language use.

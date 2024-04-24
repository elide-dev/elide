# Module rpc

`elide-rpc` provides a cross-platform, gRPC-Web-compatible RPC and remoting framework for Elide.

## Installation

The `elide-rpc` module is provided via
[Maven Central](https://search.maven.org/search?q=g:dev.elide%20AND%20a:elide-rpc), and also via Elide's own
[GitHub Packages](https://github.com/orgs/elide-dev/packages?ecosystem=maven&q=core&tab=packages&ecosystem=maven&q=elide-rpc)
repository.

**Via Gradle (Catalog):**

```kotlin
implementation(framework.elide.rpc)
```

**Via Gradle (Kotlin DSL):**

```kotlin
implementation("dev.elide:elide-rpc")
```

**Via Gradle (Groovy DSL):**

```kotlin
implementation "dev.elide:elide-rpc"
```

**Via Maven:**

```xml
<dependency>
  <groupId>dev.elide</groupId>
  <artifactId>elide-rpc-jvm</artifactId>
</dependency>
```

# Package elide.rpc.server

Server-side RPC definitions and utilities.

# Package elide.rpc.server.web

gRPC-Web compatible serving controller for Elide RPC messages.

# Package grpcweb

gRPC-Web typings for Kotlin/JS.

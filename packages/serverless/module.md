# Module serverless

`elide-serverless` implements pure-Kotlin HTTP and networking utilities for use in serverless environments.

## Installation

The `elide-serverless` module is provided via
[Maven Central](https://search.maven.org/search?q=g:dev.elide%20AND%20a:elide-rpc), and also via Elide's own
[GitHub Packages](https://github.com/orgs/elide-dev/packages?ecosystem=maven&q=core&tab=packages&ecosystem=maven&q=elide-rpc)
repository.

**Via Gradle (Catalog):**

```kotlin
implementation(framework.elide.serverless)
```

**Via Gradle (Kotlin DSL):**

```kotlin
implementation("dev.elide:elide-serverless")
```

**Via Gradle (Groovy DSL):**

```kotlin
implementation "dev.elide:elide-serverless"
```

**Via Maven:**

```xml
<dependency>
  <groupId>dev.elide</groupId>
  <artifactId>elide-serverless-jvm</artifactId>
</dependency>
```

# Package elide.http

Defines Elide's pure-Kotlin HTTP types, which are modeled after the WhatWG Fetch API.

# Package elide.http.api

Defines API interfaces implemented by Elide's HTTP layer, which are modeled after the WhatWG Fetch API.

# Package elide.net

Provides implementations of low-level network types defined for serverless use.

# Package elide.net.api

Provides API defintions for low-level network types defined for serverless use.

# Module graalvm-js

`elide-graalvm-js` provides server-side JavaScript language support for Elide.

## JavaScript support in Elide

JavaScript support is always built into Elide; the engine is powered by GraalJs. When operating server-side, this
package is loaded to provide type layouts and other runtime support for various libraries.

Libraries supported by this package include:

- [Emotion](https://emotion.sh/docs/introduction), a CSS-in-JS library
- [Preact](https://preactjs.com/), a fast 3kB alternative to React
- [React](https://reactjs.org/), a declarative, efficient, and flexible JavaScript library for building user interfaces
- [Remix](https://remix.run/), a full-stack web framework for React
- [WebStreams](https://streams.spec.whatwg.org/), a standard for representing and processing streams of data

Modules provided by this package are loaded at runtime within the JavaScript VM.

## Installation

This package is made available via Maven Central and Elide's package repository, but is not designed for end-user
consumption. Language support is built-in to the Elide runtime.

**Via Gradle (Catalog):**

```kotlin
implementation(framework.elide.graalvm.js)
```

**Via Gradle (Kotlin DSL):**

```kotlin
implementation("dev.elide:elide-graalvm-js")
```

**Via Gradle (Groovy DSL):**

```kotlin
implementation "dev.elide:elide-graalvm-js"
```

**Via Maven:**

```xml
<dependency>
  <groupId>dev.elide</groupId>
  <artifactId>elide-graalvm-js</artifactId>
</dependency>
```

# Package elide.frontend.ssr

JavaScript type layouts for SSR (server-side rendering) integration.

# Package elide.runtime.gvm

JavaScript runtime support for GraalVM.

# Package emotion.cache

Emotion server-side style rendering cache types.

# Package emotion.server

Emotion server-side style rendering types.

# Package preact.ssr

Preact server-side rendering types.

# Package react.dom.server

React DOM server-side rendering types.

# Package react.router.dom.server

React Router DOM server-side rendering types.

# Package remix.run.router

Remix router server-side rendering types.

# Package webstreams.polyfill

WebStreams polyfill types.

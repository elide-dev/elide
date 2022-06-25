//[graalvm](../../index.md)/[elide.server](index.md)

# Package elide.server

## Types

| Name | Summary |
|---|---|
| [SSRContent](-s-s-r-content/index.md) | [jvm]<br>class [SSRContent](-s-s-r-content/index.md)(prettyhtml: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, builder: HTML.() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) : [ServerRenderer](../elide.server.ssr/-server-renderer/index.md) |

## Functions

| Name | Summary |
|---|---|
| [injectSSR](inject-s-s-r.md) | [jvm]<br>fun BODY.[injectSSR](inject-s-s-r.md)(domId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = defaultSsrDomId, classes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = emptySet(), attrs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt; = emptyList(), path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = nodeSsrDefaultPath)<br>Evaluate and inject SSR content into a larger HTML page, using a `<main>` tag as the root element in the dom; apply [domId](inject-s-s-r.md), [classes](inject-s-s-r.md), and any additional [attrs](inject-s-s-r.md) to the root element, if specified. |
| [ssr](ssr.md) | [jvm]<br>suspend fun [ssr](ssr.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = nodeSsrDefaultPath, response: MutableHttpResponse&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt; = HttpResponse.ok()): MutableHttpResponse&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt;<br>Load and serve a JavaScript bundle server-side, executing it within the context of an isolated GraalVM JavaScript runtime; then, collect the output and return it as an HTTP response.<br>[jvm]<br>fun [ssr](ssr.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = nodeSsrDefaultPath, response: MutableHttpResponse&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt; = HttpResponse.ok(), block: HTML.() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): MutableHttpResponse&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt;<br>Load and serve a JavaScript bundle server-side, executing it within the context of an isolated GraalVM JavaScript runtime; then, collect the output and return it as an HTTP response, within the provided HTML builder, which will be used to render the initial page frame. |

## Properties

| Name | Summary |
|---|---|
| [defaultSsrDomId](default-ssr-dom-id.md) | [jvm]<br>const val [defaultSsrDomId](default-ssr-dom-id.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [nodeSsrDefaultPath](node-ssr-default-path.md) | [jvm]<br>const val [nodeSsrDefaultPath](node-ssr-default-path.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

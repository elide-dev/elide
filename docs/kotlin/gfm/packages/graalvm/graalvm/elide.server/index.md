//[graalvm](../../index.md)/[elide.server](index.md)

# Package elide.server

## Functions

| Name | Summary |
|---|---|
| [injectSSR](inject-s-s-r.md) | [jvm]<br>suspend fun [BODY](../../../../packages/server/kotlinx.html/-b-o-d-y/index.md).[injectSSR](inject-s-s-r.md)(handler: [ElideController](../../../../packages/server/server/elide.server.controller/-elide-controller/index.md), request: HttpRequest&lt;*&gt;, domId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = DEFAULT_SSR_DOM_ID, classes: [Set](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; = emptySet(), attrs: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt; = emptyList(), path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = NODE_SSR_DEFAULT_PATH, invocationBase: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = DEFAULT_INVOCATION_BASE, invocationTarget: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = DEFAULT_INVOCATION_TARGET, embeddedRoot: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = EMBEDDED_ROOT)<br>Evaluate and inject SSR content into a larger HTML page, using a `<main>` tag as the root element in the dom; apply [domId](inject-s-s-r.md), [classes](inject-s-s-r.md), and any additional [attrs](inject-s-s-r.md) to the root element, if specified. |
| [ssr](ssr.md) | [jvm]<br>suspend fun [ssr](ssr.md)(request: HttpRequest&lt;*&gt;, path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = NODE_SSR_DEFAULT_PATH, response: MutableHttpResponse&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt; = HttpResponse.ok(), block: suspend [HTML](../../../../packages/server/kotlinx.html/-h-t-m-l/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): MutableHttpResponse&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt;<br>Load and serve a JavaScript bundle server-side, executing it within the context of an isolated GraalVM JavaScript runtime; then, collect the output and return it as an HTTP response, within the provided HTML builder, which will be used to render the initial page frame. |

## Properties

| Name | Summary |
|---|---|
| [DEFAULT_INVOCATION_BASE](-d-e-f-a-u-l-t_-i-n-v-o-c-a-t-i-o-n_-b-a-s-e.md) | [jvm]<br>val [DEFAULT_INVOCATION_BASE](-d-e-f-a-u-l-t_-i-n-v-o-c-a-t-i-o-n_-b-a-s-e.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [DEFAULT_INVOCATION_TARGET](-d-e-f-a-u-l-t_-i-n-v-o-c-a-t-i-o-n_-t-a-r-g-e-t.md) | [jvm]<br>val [DEFAULT_INVOCATION_TARGET](-d-e-f-a-u-l-t_-i-n-v-o-c-a-t-i-o-n_-t-a-r-g-e-t.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |
| [DEFAULT_SSR_DOM_ID](-d-e-f-a-u-l-t_-s-s-r_-d-o-m_-i-d.md) | [jvm]<br>const val [DEFAULT_SSR_DOM_ID](-d-e-f-a-u-l-t_-s-s-r_-d-o-m_-i-d.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [NODE_SSR_DEFAULT_PATH](-n-o-d-e_-s-s-r_-d-e-f-a-u-l-t_-p-a-t-h.md) | [jvm]<br>const val [NODE_SSR_DEFAULT_PATH](-n-o-d-e_-s-s-r_-d-e-f-a-u-l-t_-p-a-t-h.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |

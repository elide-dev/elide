//[graalvm](../../index.md)/[elide.server](index.md)/[ssr](ssr.md)

# ssr

[jvm]\
suspend fun [ssr](ssr.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = nodeSsrDefaultPath, response: MutableHttpResponse&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt; = HttpResponse.ok()): MutableHttpResponse&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt;

Load and serve a JavaScript bundle server-side, executing it within the context of an isolated GraalVM JavaScript runtime; then, collect the output and return it as an HTTP response.

Additional response properties, such as headers, may be set on the return result, as it is kept mutable. To change initial parameters like the HTTP status, use the [response](ssr.md) parameter via constructors like HttpResponse.notFound.

#### Return

HTTP response wrapping the generated React SSR output, or an HTTP response which serves a 404 if the asset     could not be located at the specified path.

## Parameters

jvm

| | |
|---|---|
| path | Path to the React SSR entrypoint script, which should be embedded within the asset section of the JAR. |
| response | Mutable HTTP response to fill with the resulting SSR content. Sets the status and headers. |

[jvm]\
fun [ssr](ssr.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = nodeSsrDefaultPath, response: MutableHttpResponse&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt; = HttpResponse.ok(), block: HTML.() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): MutableHttpResponse&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt;

Load and serve a JavaScript bundle server-side, executing it within the context of an isolated GraalVM JavaScript runtime; then, collect the output and return it as an HTTP response, within the provided HTML builder, which will be used to render the initial page frame.

Additional response properties, such as headers, may be set on the return result, as it is kept mutable. To change initial parameters like the HTTP status, use the [response](ssr.md) parameter via constructors like HttpResponse.notFound.

#### Return

HTTP response wrapping the generated React SSR output, or an HTTP response which serves a 404 if the asset     could not be located at the specified path.

## Parameters

jvm

| | |
|---|---|
| path | Path to the React SSR entrypoint script, which should be embedded within the asset section of the JAR. |
| response | Mutable HTTP response to fill with the resulting SSR content. Sets the status and headers. |
| block |  |

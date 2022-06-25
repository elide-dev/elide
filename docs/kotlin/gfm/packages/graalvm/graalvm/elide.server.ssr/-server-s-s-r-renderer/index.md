//[graalvm](../../../index.md)/[elide.server.ssr](../index.md)/[ServerSSRRenderer](index.md)

# ServerSSRRenderer

[jvm]\
class [ServerSSRRenderer](index.md)(script: [JsRuntime.ExecutableScript](../../elide.runtime.graalvm/-js-runtime/-executable-script/index.md)) : [ServerRenderer](../-server-renderer/index.md)

Renderer class which executes JavaScript via SSR and provides the resulting response to Micronaut.

## Constructors

| | |
|---|---|
| [ServerSSRRenderer](-server-s-s-r-renderer.md) | [jvm]<br>fun [ServerSSRRenderer](-server-s-s-r-renderer.md)(script: [JsRuntime.ExecutableScript](../../elide.runtime.graalvm/-js-runtime/-executable-script/index.md)) |

## Functions

| Name | Summary |
|---|---|
| [render](render.md) | [jvm]<br>open override fun [render](render.md)(): [ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)<br>Render the attached [script](../../../../../packages/graalvm/elide.server.ssr/-server-s-s-r-renderer/script.md) and return the resulting content as a [ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html), built from the result of [renderInline](render-inline.md). |
| [renderInline](render-inline.md) | [jvm]<br>fun [renderInline](render-inline.md)(): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?<br>Render the attached [script](../../../../../packages/graalvm/elide.server.ssr/-server-s-s-r-renderer/script.md) and return the resulting content as a regular [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html). |
| [renderResponse](render-response.md) | [jvm]<br>fun [renderResponse](render-response.md)(response: MutableHttpResponse&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt;): MutableHttpResponse&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt;<br>Render the attached [script](../../../../../packages/graalvm/elide.server.ssr/-server-s-s-r-renderer/script.md) into a [ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html), and wrap it in a Micronaut MutableHttpResponse provided at [response](render-response.md). |

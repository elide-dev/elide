//[graalvm](../../../index.md)/[elide.server.ssr](../index.md)/[ServerSSRRenderer](index.md)/[renderResponse](render-response.md)

# renderResponse

[jvm]\
suspend fun [renderResponse](render-response.md)(response: MutableHttpResponse&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt;): MutableHttpResponse&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt;

Render the attached [script](../../../../../packages/server/kotlinx.html/index.md) into a [ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html), and wrap it in a Micronaut MutableHttpResponse provided at [response](render-response.md).

#### Return

Mutable [response](render-response.md) with body data filled in from the execution result of [script](../../../../../packages/server/kotlinx.html/index.md).

## Parameters

jvm

| | |
|---|---|
| response | Base mutable response to fill body data for. |

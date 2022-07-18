//[graalvm](../../../index.md)/[elide.server.ssr](../index.md)/[ServerSSRRenderer](index.md)/[render](render.md)

# render

[jvm]\
open suspend override fun [render](render.md)(): [ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)

Render the attached [script](../../../../../packages/server/kotlinx.html/index.md) and return the resulting content as a [ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html), built from the result of [renderSuspendAsync](render-suspend-async.md).

#### Return

Byte stream of resulting content.

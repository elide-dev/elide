//[graalvm](../../../index.md)/[elide.server.ssr](../index.md)/[ServerSSRRenderer](index.md)/[render](render.md)

# render

[jvm]\
open override fun [render](render.md)(): [ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)

Render the attached [script](../../../../graalvm/elide.server.ssr/-server-s-s-r-renderer/script.md) and return the resulting content as a [ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html), built from the result of [renderInline](render-inline.md).

#### Return

Byte stream of resulting content.

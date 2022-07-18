//[server](../../../index.md)/[elide.server](../index.md)/[HtmlRenderer](index.md)

# HtmlRenderer

[jvm]\
class [HtmlRenderer](index.md)(prettyhtml: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, handler: [PageController](../../elide.server.controller/-page-controller/index.md)? = null, builder: suspend [HTML](../../../../../packages/server/kotlinx.html/-h-t-m-l/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) : [SuspensionRenderer](../-suspension-renderer/index.md)&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt;

## Constructors

| | |
|---|---|
| [HtmlRenderer](-html-renderer.md) | [jvm]<br>fun [HtmlRenderer](-html-renderer.md)(prettyhtml: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, handler: [PageController](../../elide.server.controller/-page-controller/index.md)? = null, builder: suspend [HTML](../../../../../packages/server/kotlinx.html/-h-t-m-l/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)) |

## Functions

| Name | Summary |
|---|---|
| [render](render.md) | [jvm]<br>open suspend override fun [render](render.md)(): [ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html) |

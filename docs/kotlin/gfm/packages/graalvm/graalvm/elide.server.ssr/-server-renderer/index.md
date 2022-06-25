//[graalvm](../../../index.md)/[elide.server.ssr](../index.md)/[ServerRenderer](index.md)

# ServerRenderer

[jvm]\
interface [ServerRenderer](index.md) : [ResponseRenderer](../../../../../packages/graalvm/elide.server/-response-renderer/index.md)&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt; 

Describes supported server-renderer API methods, which are used by the framework to translate result content from embedded SSR scripts.

## Functions

| Name | Summary |
|---|---|
| [render](index.md#-2105732579%2FFunctions%2F-106064166) | [jvm]<br>abstract fun [render](index.md#-2105732579%2FFunctions%2F-106064166)(): [ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html) |

## Inheritors

| Name |
|---|
| [SSRContent](../../elide.server/-s-s-r-content/index.md) |
| [ServerSSRRenderer](../-server-s-s-r-renderer/index.md) |

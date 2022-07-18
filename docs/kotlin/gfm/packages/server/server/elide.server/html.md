//[server](../../index.md)/[elide.server](index.md)/[html](html.md)

# html

[jvm]\
suspend fun [PageController](../elide.server.controller/-page-controller/index.md).[html](html.md)(block: suspend [HTML](../../../../packages/server/kotlinx.html/-h-t-m-l/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [RawResponse](index.md#852884585%2FClasslikes%2F-1343588467)

Responds to a client with an HTML response, using specified [block](html.md) to build an HTML page via Kotlin's HTML DSL.

#### Return

HTTP response wrapping the HTML page, with a content type of `text/html; charset=utf-8`.

## Parameters

jvm

| | |
|---|---|
| block | Block to execute to build the HTML page. |

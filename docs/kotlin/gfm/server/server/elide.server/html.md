//[server](../../index.md)/[elide.server](index.md)/[html](html.md)

# html

[jvm]\
fun [html](html.md)(block: HTML.() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): HttpResponse&lt;[ByteArrayOutputStream](https://docs.oracle.com/javase/8/docs/api/java/io/ByteArrayOutputStream.html)&gt;

Responds to a client with an HTML response, using specified [block](html.md) to build an HTML page via Kotlin's HTML DSL.

#### Return

HTTP response wrapping the HTML page, with a content type of `text/html; charset=utf-8`.

## Parameters

jvm

| | |
|---|---|
| block | Block to execute to build the HTML page. |

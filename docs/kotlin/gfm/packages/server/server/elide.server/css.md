//[server](../../index.md)/[elide.server](index.md)/[css](css.md)

# css

[jvm]\
fun [css](css.md)(block: CssBuilder.() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [StreamedAssetResponse](index.md#-491452832%2FClasslikes%2F-1343588467)

Responds to a client with an HTML response, using specified [block](css.md) to build the CSS document via Kotlin's CSS DSL.

#### Return

HTTP response wrapping the CSS content, with a content type of `text/css; charset=utf-8`.

#### Parameters

jvm

| | |
|---|---|
| block | Block to execute to build the CSS document. |

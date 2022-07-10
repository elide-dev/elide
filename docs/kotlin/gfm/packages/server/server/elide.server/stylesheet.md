//[server](../../index.md)/[elide.server](index.md)/[stylesheet](stylesheet.md)

# stylesheet

[jvm]\
inline fun [HEAD](../../../../packages/server/kotlinx.html/-h-e-a-d/index.md).[stylesheet](stylesheet.md)(uri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), media: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, attrs: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null)

Generates a CSS link from the provided handler [uri](stylesheet.md), optionally including the specified [attrs](stylesheet.md).

[jvm]\
suspend fun [PageController](../elide.server.controller/-page-controller/index.md).[stylesheet](stylesheet.md)(request: HttpRequest&lt;*&gt;, moduleId: [AssetModuleId](index.md#-803173189%2FClasslikes%2F-1343588467)): [StreamedAssetResponse](index.md#-491452832%2FClasslikes%2F-1343588467)

suspend fun [PageController](../elide.server.controller/-page-controller/index.md).[stylesheet](stylesheet.md)(request: HttpRequest&lt;*&gt;, block: [AssetHandler](-asset-handler/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [StreamedAssetResponse](index.md#-491452832%2FClasslikes%2F-1343588467)

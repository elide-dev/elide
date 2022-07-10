//[server](../../index.md)/[elide.server](index.md)/[script](script.md)

# script

[jvm]\
inline fun [HEAD](../../../../packages/server/kotlinx.html/-h-e-a-d/index.md).[script](script.md)(uri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), defer: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, async: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, type: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;application/javascript&quot;, attrs: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null)

Generates a `<head>` script link from the provided handler [uri](script.md), optionally including the specified [attrs](script.md).

[jvm]\
inline fun [BODY](../../../../packages/server/kotlinx.html/-b-o-d-y/index.md).[script](script.md)(uri: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), defer: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, async: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, type: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) = &quot;application/javascript&quot;, attrs: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;? = null)

Generates a `<body>` script link from the provided handler [uri](script.md), optionally including the specified [attrs](script.md).

[jvm]\
suspend fun [PageController](../elide.server.controller/-page-controller/index.md).[script](script.md)(request: HttpRequest&lt;*&gt;, moduleId: [AssetModuleId](index.md#-803173189%2FClasslikes%2F-1343588467)): [StreamedAssetResponse](index.md#-491452832%2FClasslikes%2F-1343588467)

Serve a static script asset embedded within the application, based on the provided [moduleId](script.md), and customizing the response based on the provided [request](script.md).

#### Return

Streamed asset response for the desired module ID.

## Parameters

jvm

| | |
|---|---|
| request | Request to consider when creating the asset response. |
| moduleId | Module ID for the asset which we wish to serve. |

[jvm]\
suspend fun [PageController](../elide.server.controller/-page-controller/index.md).[script](script.md)(request: HttpRequest&lt;*&gt;, block: [AssetHandler](-asset-handler/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [StreamedAssetResponse](index.md#-491452832%2FClasslikes%2F-1343588467)

Serve a static script asset embedded within the application, based on the provided [block](script.md), which should customize the serving of the script and declare a module ID.

#### Return

Streamed asset response generated from the configuration provided by [block](script.md).

## Parameters

jvm

| | |
|---|---|
| request | Request to consider when creating the asset response. |
| block | Block which, when executed, will configure the script for a response. |

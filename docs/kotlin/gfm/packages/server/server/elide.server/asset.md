//[server](../../index.md)/[elide.server](index.md)/[asset](asset.md)

# asset

[jvm]\
suspend fun [PageController](../elide.server.controller/-page-controller/index.md).[asset](asset.md)(request: HttpRequest&lt;*&gt;, moduleId: [AssetModuleId](index.md#-803173189%2FClasslikes%2F-1343588467), type: [AssetType](../elide.server.assets/-asset-type/index.md)? = null): [StreamedAssetResponse](index.md#-491452832%2FClasslikes%2F-1343588467)

Serve an application asset file which is embedded in the application JAR as a registered server asset, from the application resource path `/assets`.

To use module ID-based assets, files must be registered at build time through the Elide Plugin for Gradle, or must produce an equivalent protocol buffer manifest.

#### Return

HTTP response wrapping the desired asset, or an HTTP response which serves a 404 if the asset could not be     located at the specified path.

## Parameters

jvm

| | |
|---|---|
| moduleId | ID of the asset module we wish to serve. |
| type | Specifies the asset type expected to be served by this call, if known. |

[jvm]\
suspend fun [PageController](../elide.server.controller/-page-controller/index.md).[asset](asset.md)(request: HttpRequest&lt;*&gt;, type: [AssetType](../elide.server.assets/-asset-type/index.md)? = null, block: suspend [AssetHandler](-asset-handler/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [StreamedAssetResponse](index.md#-491452832%2FClasslikes%2F-1343588467)

Generate a [StreamedAssetResponse](index.md#-491452832%2FClasslikes%2F-1343588467) which serves an asset embedded within the application, and specified by the provided [block](asset.md); [request](asset.md) will be considered when producing the response.

#### Return

Structure which streams the resolved asset content as the response.

## Parameters

jvm

| | |
|---|---|
| request | HTTP request to consider when producing the desired asset response. |
| type | Type of asset expected to be returned with this response. |
| block | Block to customize the serving of this asset and declare a module ID. |

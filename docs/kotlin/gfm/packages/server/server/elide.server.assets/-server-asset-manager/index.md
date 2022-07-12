//[server](../../../index.md)/[elide.server.assets](../index.md)/[ServerAssetManager](index.md)

# ServerAssetManager

[jvm]\
@Context

class [ServerAssetManager](index.md) : [AssetManager](../-asset-manager/index.md)

Built-in asset manager implementation for use with Elide applications.

Resolves and loads assets embedded in the application at build-time, based on binary-encoded protocol buffer messages which define the dependency structure and metadata of each embedded asset.

## Parameters

jvm

| | |
|---|---|
| assetConfig | Server-side asset configuration. |
| assetIndex | Active asset index for this server run. |
| reader | Active asset reader implementation for this server run. |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [findAssetByModuleId](../-asset-manager/find-asset-by-module-id.md) | [jvm]<br>open fun [findAssetByModuleId](../-asset-manager/find-asset-by-module-id.md)(asset: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467)): [AssetPointer](../-asset-pointer/index.md)?<br>Resolve an [AssetPointer](../-asset-pointer/index.md) for the specified [asset](../-asset-manager/find-asset-by-module-id.md) module ID; if none can be located within the current set of live server assets, return `null`. |
| [linkForAsset](link-for-asset.md) | [jvm]<br>open override fun [linkForAsset](link-for-asset.md)(module: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), overrideType: [AssetType](../-asset-type/index.md)?): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [renderAssetAsync](render-asset-async.md) | [jvm]<br>open suspend override fun [renderAssetAsync](render-asset-async.md)(request: HttpRequest&lt;*&gt;, asset: [ServerAsset](../-server-asset/index.md)): Deferred&lt;[StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)&gt; |
| [resolve](../-asset-manager/resolve.md) | [jvm]<br>open fun [resolve](../-asset-manager/resolve.md)(request: HttpRequest&lt;*&gt;, moduleId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null): [ServerAsset](../-server-asset/index.md)?<br>Resolve the asset requested by the provided HTTP [request](../-asset-manager/resolve.md); if the corresponding file cannot be found, return `null`, and otherwise, throw an error. |
| [serveAsync](../-asset-manager/serve-async.md) | [jvm]<br>open suspend fun [serveAsync](../-asset-manager/serve-async.md)(request: HttpRequest&lt;*&gt;, moduleId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null): Deferred&lt;[StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)&gt;<br>Asynchronously produce an HTTP response which serves the asset described by the provided [request](../-asset-manager/serve-async.md); if the asset in question cannot be located, serve a `404 Not Found`, and for any other error, serve a `500 Internal Server Error`. |
| [serveNotFoundAsync](../-asset-manager/serve-not-found-async.md) | [jvm]<br>open fun [serveNotFoundAsync](../-asset-manager/serve-not-found-async.md)(request: HttpRequest&lt;*&gt;): Deferred&lt;[StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)&gt;<br>Serve a response of status HTTP 404 (Not Found), in response to a request for an asset which could not be located by the built-in asset system. |

## Properties

| Name | Summary |
|---|---|
| [logging](logging.md) | [jvm]<br>open override val [logging](logging.md): Logger |
| [reader](reader.md) | [jvm]<br>open override val [reader](reader.md): [AssetReader](../-asset-reader/index.md) |

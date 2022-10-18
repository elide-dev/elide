//[server](../../../index.md)/[elide.server.assets](../index.md)/[ServerAssetReader](index.md)

# ServerAssetReader

[jvm]\
@Context

@Singleton

class [ServerAssetReader](index.md) : [AssetReader](../-asset-reader/index.md)

Default implementation of an [AssetReader](../-asset-reader/index.md); used in concert with the default [AssetManager](../-asset-manager/index.md) to fulfill HTTP requests for static assets embedded within the application.

#### Parameters

jvm

| | |
|---|---|
| assetConfig | Server-side asset configuration. |
| assetIndex | Live index of asset data. |

## Functions

| Name | Summary |
|---|---|
| [findByModuleId](find-by-module-id.md) | [jvm]<br>open override fun [findByModuleId](find-by-module-id.md)(moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467)): [ServerAsset](../-server-asset/index.md)? |
| [pointerTo](pointer-to.md) | [jvm]<br>open override fun [pointerTo](pointer-to.md)(moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467)): [AssetPointer](../-asset-pointer/index.md)? |
| [readAsync](read-async.md) | [jvm]<br>open suspend override fun [readAsync](read-async.md)(descriptor: [ServerAsset](../-server-asset/index.md), request: HttpRequest&lt;*&gt;): Deferred&lt;[RenderedAsset](../-rendered-asset/index.md)&gt; |
| [resolve](../-asset-resolver/resolve.md) | [jvm]<br>open fun [resolve](../-asset-resolver/resolve.md)(request: HttpRequest&lt;*&gt;): [ServerAsset](../-server-asset/index.md)?<br>Resolve the provided HTTP [request](../-asset-resolver/resolve.md) to an asset path string, and then resolve the asset path string to a loaded [ServerAsset](../-server-asset/index.md), if possible; return `null` if the asset cannot be located.<br>[jvm]<br>open override fun [resolve](resolve.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ServerAsset](../-server-asset/index.md)? |

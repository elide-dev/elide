//[server](../../../index.md)/[elide.server.assets](../index.md)/[ServerAssetReader](index.md)

# ServerAssetReader

[jvm]\
@Context

@Singleton

class [ServerAssetReader](index.md) : [AssetReader](../-asset-reader/index.md)

Default implementation of an [AssetReader](../-asset-reader/index.md); used in concert with the default [AssetManager](../-asset-manager/index.md) to fulfill HTTP requests for static assets embedded within the application.

## Constructors

| | |
|---|---|
| [ServerAssetReader](-server-asset-reader.md) | [jvm]<br>fun [ServerAssetReader](-server-asset-reader.md)() |

## Functions

| Name | Summary |
|---|---|
| [readAsync](read-async.md) | [jvm]<br>open suspend override fun [readAsync](read-async.md)(descriptor: [ServerAsset](../-server-asset/index.md)): Deferred&lt;[RenderedAsset](../-rendered-asset/index.md)&gt; |
| [resolve](../-asset-resolver/resolve.md) | [jvm]<br>open fun [resolve](../-asset-resolver/resolve.md)(request: HttpRequest&lt;*&gt;): [ServerAsset](../-server-asset/index.md)?<br>open override fun [resolve](resolve.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ServerAsset](../-server-asset/index.md)? |

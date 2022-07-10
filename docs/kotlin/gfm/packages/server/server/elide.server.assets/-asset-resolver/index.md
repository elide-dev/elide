//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetResolver](index.md)

# AssetResolver

[jvm]\
@[API](../../../../../packages/base/base/elide.annotations/-a-p-i/index.md)

interface [AssetResolver](index.md)

Describes the expected API surface for a resolver of server-side assets, which, in cooperation with an [AssetReader](../-asset-reader/index.md) and under the management of an [AssetManager](../-asset-manager/index.md), is responsible for checking asset paths for existence and translating them to absolute paths which may be read and served.

## See also

jvm

| | |
|---|---|
| [elide.server.assets.AssetReader](../-asset-reader/index.md) | which is charged with efficiently reading asset content. |
| [elide.server.assets.AssetManager](../-asset-manager/index.md) | which coordinates between the [AssetReader](../-asset-reader/index.md) and [AssetResolver](index.md). |
| [elide.server.assets.RenderedAsset](../-rendered-asset/index.md) | for the generic return value model leveraged by [AssetManager](../-asset-manager/index.md). |
| [elide.server.assets.ServerAsset](../-server-asset/index.md) | for the symbolic asset reference model leveraged by [AssetManager](../-asset-manager/index.md). |

## Functions

| Name | Summary |
|---|---|
| [resolve](resolve.md) | [jvm]<br>open fun [resolve](resolve.md)(request: HttpRequest&lt;*&gt;): [ServerAsset](../-server-asset/index.md)?<br>abstract fun [resolve](resolve.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ServerAsset](../-server-asset/index.md)? |

## Inheritors

| Name |
|---|
| [AssetReader](../-asset-reader/index.md) |
| [ServerAssetResolver](../-server-asset-resolver/index.md) |

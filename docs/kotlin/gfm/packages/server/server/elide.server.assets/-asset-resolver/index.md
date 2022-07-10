//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetResolver](index.md)

# AssetResolver

[jvm]\
@[API](../../../../../packages/base/base/elide.annotations/-a-p-i/index.md)

interface [AssetResolver](index.md)

Describes the expected API surface for a resolver of server-side assets, which, in cooperation with an [AssetReader](../-asset-reader/index.md) and under the management of an [AssetManager](../-asset-manager/index.md), is responsible for checking asset paths for existence and translating them to absolute paths which may be read and served.

###  Asset tags

&quot;Asset tags&quot; are short strings which are computed at build time from a cryptographic hash of the asset's contents. Tags can be used to serve an asset from the asset controller, using URLs like `/_/asset/{tag}.{ext}`, where `{tag}` is the asset tag and `{ext}` is the asset's file extension (prefix adjustable by user).

Developers reference assets by their *module ID*, which is a short recognizable string assigned by the developer to a given asset.

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
| [resolve](resolve.md) | [jvm]<br>open fun [resolve](resolve.md)(request: HttpRequest&lt;*&gt;): [ServerAsset](../-server-asset/index.md)?<br>Resolve the provided HTTP [request](resolve.md) to an asset path string, and then resolve the asset path string to a loaded [ServerAsset](../-server-asset/index.md), if possible; return `null` if the asset cannot be located.<br>[jvm]<br>abstract fun [resolve](resolve.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ServerAsset](../-server-asset/index.md)?<br>Resolve the provided [path](resolve.md) to a server asset, if possible, or return `null` to indicate that the asset could not be located; the given [path](resolve.md) value can be prefixed with the asset serving prefix (`/_/asset` by default) or not prefixed at all. |

## Inheritors

| Name |
|---|
| [AssetReader](../-asset-reader/index.md) |

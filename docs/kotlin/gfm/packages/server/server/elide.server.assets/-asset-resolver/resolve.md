//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetResolver](index.md)/[resolve](resolve.md)

# resolve

[jvm]\
abstract fun [resolve](resolve.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ServerAsset](../-server-asset/index.md)?

Resolve the provided [path](resolve.md) to a server asset, if possible, or return `null` to indicate that the asset could not be located; the given [path](resolve.md) value can be prefixed with the asset serving prefix (`/_/asset` by default) or not prefixed at all.

**Note:** Only asset tags should be used to serve resources at runtime in production circumstances.

#### Return

Resolved server asset, or `null`, indicating that the asset could not be located.

## Parameters

jvm

| | |
|---|---|
| path | Path for the asset which we should resolve. |

[jvm]\
open fun [resolve](resolve.md)(request: HttpRequest&lt;*&gt;): [ServerAsset](../-server-asset/index.md)?

Resolve the provided HTTP [request](resolve.md) to an asset path string, and then resolve the asset path string to a loaded [ServerAsset](../-server-asset/index.md), if possible; return `null` if the asset cannot be located.

#### Return

Resolved server-held asset, or `null` to indicate that the asset could not be located.

## Parameters

jvm

| | |
|---|---|
| request | HTTP request for the asset which we should resolve. |

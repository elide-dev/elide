//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetManager](index.md)/[resolve](resolve.md)

# resolve

[jvm]\
open fun [resolve](resolve.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ServerAsset](../-server-asset/index.md)?

Resolve the provided asset [path](resolve.md) to a [ServerAsset](../-server-asset/index.md) descriptor; if the file cannot be found, return `null`, and otherwise, throw an error.

#### Return

Resolved server asset, or `null` if one could not be located at the provided [path](resolve.md).

## Parameters

jvm

| | |
|---|---|
| path | Relative path to the asset which we want to resolve. |

[jvm]\
open fun [resolve](resolve.md)(request: HttpRequest&lt;*&gt;): [ServerAsset](../-server-asset/index.md)?

Resolve the asset requested by the provided HTTP [request](resolve.md); if the corresponding file cannot be found, return `null`, and otherwise, throw an error.

#### Return

Resolved server asset, or `null` if one could not be located at the calculated path provided by [request](resolve.md).

## Parameters

jvm

| | |
|---|---|
| request | HTTP request to interpret into a relative asset path and return a descriptor for. |

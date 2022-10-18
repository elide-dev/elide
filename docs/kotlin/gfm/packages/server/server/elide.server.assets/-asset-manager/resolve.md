//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetManager](index.md)/[resolve](resolve.md)

# resolve

[jvm]\
open fun [resolve](resolve.md)(request: HttpRequest&lt;*&gt;, moduleId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null): [ServerAsset](../-server-asset/index.md)?

Resolve the asset requested by the provided HTTP [request](resolve.md); if the corresponding file cannot be found, return `null`, and otherwise, throw an error.

#### Return

Resolved server asset, or `null` if one could not be located at the calculated path provided by [request](resolve.md).

#### Parameters

jvm

| | |
|---|---|
| request | HTTP request to interpret into a relative asset path and return a descriptor for. |
| moduleId | Resolved asset module ID to serve, if known; if `null`, one will be resolved from the [request](resolve.md). |

//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetManager](index.md)/[serveAsync](serve-async.md)

# serveAsync

[jvm]\
open suspend fun [serveAsync](serve-async.md)(request: HttpRequest&lt;*&gt;, moduleId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null): Deferred&lt;[StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)&gt;

Asynchronously produce an HTTP response which serves the asset described by the provided [request](serve-async.md); if the asset in question cannot be located, serve a `404 Not Found`, and for any other error, serve a `500 Internal Server Error`.

#### Return

Deferred task which resolves to an HTTP response serving the requested asset.

#### Parameters

jvm

| | |
|---|---|
| request | HTTP request which should be translated into an asset path and served. |
| moduleId | Resolved asset module ID to serve, if known; if `null`, one will be resolved from the [request](serve-async.md). |

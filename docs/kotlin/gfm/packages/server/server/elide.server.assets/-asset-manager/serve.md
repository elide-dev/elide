//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetManager](index.md)/[serve](serve.md)

# serve

[jvm]\
open suspend fun [serve](serve.md)(request: HttpRequest&lt;*&gt;): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)

Produce an HTTP response which serves the asset described by the provided [request](serve.md); if the asset in question cannot be located, serve a `404 Not Found`, and for any other error, serve a `500 Internal Server Error`.

#### Return

HTTP response serving the requested asset.

## See also

jvm

| | |
|---|---|
| [elide.server.assets.AssetManager](serve-async.md) | for an asynchronous variant of this method. |

## Parameters

jvm

| | |
|---|---|
| request | HTTP request which should be translated into an asset path and served. |

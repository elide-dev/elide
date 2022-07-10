//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetManager](index.md)/[renderAsset](render-asset.md)

# renderAsset

[jvm]\
open suspend fun [renderAsset](render-asset.md)(request: HttpRequest&lt;*&gt;, asset: [ServerAsset](../-server-asset/index.md)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)

Suspending but synchronous variant of [renderAssetAsync](render-asset-async.md), which is responsible for rendering a resolved asset to an HTTP response; this variant returns a response value directly.

This method calls is an alias which simply awaits an async result. Further documentation is available on the implementing method.

#### Return

Streamed asset response.

## See also

jvm

| | |
|---|---|
| [elide.server.assets.AssetManager](render-asset-async.md) | for the asynchronous form of this method. |

## Parameters

jvm

| | |
|---|---|
| asset | Resolved server asset which should be served by this call. |

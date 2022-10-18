//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetManager](index.md)/[renderAssetAsync](render-asset-async.md)

# renderAssetAsync

[jvm]\
abstract suspend fun [renderAssetAsync](render-asset-async.md)(request: HttpRequest&lt;*&gt;, asset: [ServerAsset](../-server-asset/index.md)): Deferred&lt;[StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)&gt;

Responsible for converting a known-good asset held by the server into an efficient [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467) which serves the asset to the invoking client.

This method is the core of the runtime portion of the asset system. When an asset is requested via an endpoint managed by the [AssetController](../-asset-controller/index.md), it effectively calls into this method, after resolving the asset, in order to actually serve it.

[StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467) is mapped to Netty/Micronaut classes under the hood which are optimal for serving static asset data.

###  Dynamic asset transformation

If the asset must be transformed before being returned, especially in some computationally-expensive manner, then the underlying method should switch out to the I/O scheduler (or some other scheduler) in order to avoid any blocking behavior.

###  Response variability

If the response needs to be customized based on the provided [request](render-asset-async.md), make sure to include any relevant request headers as `Vary` values in the response, so that HTTP caching can work correctly.

#### Return

Deferred task which resolves to a [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467) satisfying a request to serve the provided resolved [asset](render-asset-async.md) data.

#### Parameters

jvm

| | |
|---|---|
| request | HTTP request which this render cycle is responding to. |
| asset | Resolved server asset which we intend to render and serve. |

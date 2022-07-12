//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetManager](index.md)/[serveNotFoundAsync](serve-not-found-async.md)

# serveNotFoundAsync

[jvm]\
open fun [serveNotFoundAsync](serve-not-found-async.md)(request: HttpRequest&lt;*&gt;): Deferred&lt;[StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)&gt;

Serve a response of status HTTP 404 (Not Found), in response to a request for an asset which could not be located by the built-in asset system.

The resulting response is not specific to the asset requested, but the [request](serve-not-found-async.md) is provided nonetheless so that implementations may log or perform other relevant follow-up work.

#### Return

Deferred task which resolves to an HTTP 404 response.

## Parameters

jvm

| | |
|---|---|
| request | HTTP request which prompted this 404-not-found response. |

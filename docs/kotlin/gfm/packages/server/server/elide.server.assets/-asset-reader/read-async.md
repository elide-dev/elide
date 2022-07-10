//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetReader](index.md)/[readAsync](read-async.md)

# readAsync

[jvm]\
abstract suspend fun [readAsync](read-async.md)(descriptor: [ServerAsset](../-server-asset/index.md), request: HttpRequest&lt;*&gt;? = null): Deferred&lt;[RenderedAsset](../-rendered-asset/index.md)&gt;

Given a resolved asset [descriptor](read-async.md) which should be known to exist, read the associated asset content, and return it as an async Deferred task which can be awaited, and then consumed.

If the asset underlying the provided asset descriptor is found not to exist, a [FileNotFoundException](https://docs.oracle.com/javase/8/docs/api/java/io/FileNotFoundException.html) is raised.

#### Return

Deferred task which resolves to a rendered asset which may be consumed, corresponding to [descriptor](read-async.md).

## Parameters

jvm

| | |
|---|---|
| descriptor | Resolved asset descriptor, which is expected to exist. |
| request | HTTP request which is asking to be served this asset. |

## Throws

| | |
|---|---|
| [java.io.FileNotFoundException](https://docs.oracle.com/javase/8/docs/api/java/io/FileNotFoundException.html) | if the provided asset cannot be located. |

//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetController](index.md)/[assetGet](asset-get.md)

# assetGet

[jvm]\

@Get(value = &quot;/{tag}.{ext}&quot;)

suspend fun [assetGet](asset-get.md)(request: HttpRequest&lt;*&gt;, tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), ext: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)

Handles HTTP `GET` calls to asset endpoints based on &quot;asset tag&quot; values, which are generated at build time, and are typically composed of  8-16 characters from the tail end of the content hash for the asset.

## Parameters

jvm

| | |
|---|---|
| request | HTTP request incoming to this endpoint. |
| tag | Decoded tag value from the URL. |
| ext | Extension value from the URL. |

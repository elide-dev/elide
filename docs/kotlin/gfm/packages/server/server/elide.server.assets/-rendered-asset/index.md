//[server](../../../index.md)/[elide.server.assets](../index.md)/[RenderedAsset](index.md)

# RenderedAsset

[jvm]\
data class [RenderedAsset](index.md)(val module: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), val type: [AssetType](../-asset-type/index.md), val variant: CompressionMode, val headers: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, val size: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), val lastModified: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), val digest: [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;HashAlgorithm, ByteString&gt;?, val producer: () -&gt; ByteString)

Intermediary class which represents an asset that has been fully prepared to serve to an end-user request, including any headers which should apply to the response.

## Parameters

jvm

| | |
|---|---|
| module | Asset module which was rendered to produce this record. |
| type | Type of asset being served. |
| variant | Compression mode for this asset response. |
| headers | Headers to apply to this asset response. |
| size | Size of the data expected from this asset variant. |
| lastModified | Unix epoch timestamp indicating when this asset was last modified. |
| digest | Raw bytes of the attached digest for this asset. |
| producer | Data payload callable for this asset response. |

## Constructors

| | |
|---|---|
| [RenderedAsset](-rendered-asset.md) | [jvm]<br>fun [RenderedAsset](-rendered-asset.md)(module: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), type: [AssetType](../-asset-type/index.md), variant: CompressionMode, headers: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, size: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), lastModified: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), digest: [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;HashAlgorithm, ByteString&gt;?, producer: () -&gt; ByteString) |

## Properties

| Name | Summary |
|---|---|
| [digest](digest.md) | [jvm]<br>val [digest](digest.md): [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;HashAlgorithm, ByteString&gt;? |
| [headers](headers.md) | [jvm]<br>val [headers](headers.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt; |
| [lastModified](last-modified.md) | [jvm]<br>val [lastModified](last-modified.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) |
| [module](module.md) | [jvm]<br>val [module](module.md): [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467) |
| [producer](producer.md) | [jvm]<br>val [producer](producer.md): () -&gt; ByteString |
| [size](size.md) | [jvm]<br>val [size](size.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) |
| [type](type.md) | [jvm]<br>val [type](type.md): [AssetType](../-asset-type/index.md) |
| [variant](variant.md) | [jvm]<br>val [variant](variant.md): CompressionMode |

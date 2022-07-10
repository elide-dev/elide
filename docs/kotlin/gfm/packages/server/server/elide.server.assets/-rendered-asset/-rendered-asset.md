//[server](../../../index.md)/[elide.server.assets](../index.md)/[RenderedAsset](index.md)/[RenderedAsset](-rendered-asset.md)

# RenderedAsset

[jvm]\
fun [RenderedAsset](-rendered-asset.md)(module: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), type: [AssetType](../-asset-type/index.md), variant: CompressionMode, headers: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;, size: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), lastModified: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), digest: [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;HashAlgorithm, ByteString&gt;?, producer: () -&gt; ByteString)

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

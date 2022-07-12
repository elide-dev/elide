//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetReference](index.md)/[AssetReference](-asset-reference.md)

# AssetReference

[jvm]\
fun [AssetReference](-asset-reference.md)(module: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), assetType: [AssetType](../-asset-type/index.md), href: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), type: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, inline: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, preload: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false)

## Parameters

jvm

| | |
|---|---|
| module | ID of the asset module being referenced. |
| assetType | Type of asset being referenced. |
| href | Relative link to serve the asset. |
| type | Type override for the tag, if applicable. |
| inline | Whether this asset is eligible to be inlined into the page. |
| preload | Whether this asset is eligible to be preloaded. |

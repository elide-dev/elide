//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetReference](index.md)

# AssetReference

[jvm]\
@Serializable

data class [AssetReference](index.md)(val module: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), val assetType: [AssetType](../-asset-type/index.md), val href: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val type: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, val inline: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, val preload: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false) : [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)

Represents a resolved reference to an asset at serving-time, before it is rendered into a link or other tag.

#### Parameters

jvm

| | |
|---|---|
| module | ID of the asset module being referenced. |
| assetType | Type of asset being referenced. |
| href | Relative link to serve the asset. |
| type | Type override for the tag, if applicable. |
| inline | Whether this asset is eligible to be inlined into the page. |
| preload | Whether this asset is eligible to be preloaded. |

## Constructors

| | |
|---|---|
| [AssetReference](-asset-reference.md) | [jvm]<br>fun [AssetReference](-asset-reference.md)(module: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), assetType: [AssetType](../-asset-type/index.md), href: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), type: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null, inline: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false, preload: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false) |

## Properties

| Name | Summary |
|---|---|
| [assetType](asset-type.md) | [jvm]<br>val [assetType](asset-type.md): [AssetType](../-asset-type/index.md) |
| [href](href.md) | [jvm]<br>val [href](href.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [inline](inline.md) | [jvm]<br>val [inline](inline.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [module](module.md) | [jvm]<br>val [module](module.md): [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467) |
| [preload](preload.md) | [jvm]<br>val [preload](preload.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) = false |
| [type](type.md) | [jvm]<br>val [type](type.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null |

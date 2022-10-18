//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetManager](index.md)/[linkForAsset](link-for-asset.md)

# linkForAsset

[jvm]\
abstract fun [linkForAsset](link-for-asset.md)(module: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), overrideType: [AssetType](../-asset-type/index.md)? = null): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)

Generate a relative link to serve the asset specified by the provided [module](link-for-asset.md) ID; the link is built from the active configured asset prefix, plus the &quot;asset tag,&quot; which is a variable-length cryptographic fingerprint of the asset's content.

If the asset system isn't ready, this method may suspend to wait for a period of time for initialization.

#### Return

Relative URI calculated to serve the provided asset.

#### Parameters

jvm

| | |
|---|---|
| module | Asset module ID for which a relative link is needed. |
| overrideType | Overrides the asset type, which governs the file extension in the generated link. |

#### Throws

| | |
|---|---|
| [IllegalArgumentException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-argument-exception/index.html) | If the provided [module](link-for-asset.md) ID cannot be found in the active asset bundle. |

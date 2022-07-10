//[server](../../../../index.md)/[elide.server.assets](../../index.md)/[ServerAsset](../index.md)/[Text](index.md)

# Text

[jvm]\
class [Text](index.md)(descriptor: AssetBundle.GenericBundle, index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)?) : [ServerAsset](../index.md)

Describes a generic text asset of some kind, for example, `humans.txt` or `robots.txt`; when read from the app bundle and interpreted, this class is used to hold file info.

## Parameters

jvm

| | |
|---|---|
| descriptor | Text-type settings bundle describing this asset. |
| index | Index of the content payload, within the live asset bundle, corresponding to this text asset. |

## Constructors

| | |
|---|---|
| [Text](-text.md) | [jvm]<br>fun [Text](-text.md)(descriptor: AssetBundle.GenericBundle, index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)?) |

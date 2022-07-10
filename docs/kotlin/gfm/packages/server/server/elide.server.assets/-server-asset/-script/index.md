//[server](../../../../index.md)/[elide.server.assets](../../index.md)/[ServerAsset](../index.md)/[Script](index.md)

# Script

[jvm]\
class [Script](index.md)(descriptor: AssetBundle.ScriptBundle, index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)?) : [ServerAsset](../index.md)

Describes a JavaScript asset which is embedded in a given Elide application, and described by Elide's protocol buffer structures; when read from the application bundle and interpreted, this class is used to hold script info.

## Parameters

jvm

| | |
|---|---|
| descriptor | Script-type settings bundle describing this asset. |
| index | Index of the content payload, within the live asset bundle, corresponding to this script. |

## Constructors

| | |
|---|---|
| [Script](-script.md) | [jvm]<br>fun [Script](-script.md)(descriptor: AssetBundle.ScriptBundle, index: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)?) |

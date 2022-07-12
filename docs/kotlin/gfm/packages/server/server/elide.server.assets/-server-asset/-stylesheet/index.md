//[server](../../../../index.md)/[elide.server.assets](../../index.md)/[ServerAsset](../index.md)/[Stylesheet](index.md)

# Stylesheet

[jvm]\
class [Stylesheet](index.md)(descriptor: AssetBundle.StyleBundle, index: [SortedSet](https://docs.oracle.com/javase/8/docs/api/java/util/SortedSet.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)&gt;?) : [ServerAsset](../index.md)

Describes a stylesheet asset which is embedded in a given Elide application, and described by Elide's protocol buffer structures; when read from the application bundle and interpreted, this class is used to hold document info.

## Parameters

jvm

| | |
|---|---|
| descriptor | Stylesheet-type settings bundle describing this asset. |
| index | Index of the content payload, within the live asset bundle, corresponding to this stylesheet. |

## Constructors

| | |
|---|---|
| [Stylesheet](-stylesheet.md) | [jvm]<br>fun [Stylesheet](-stylesheet.md)(descriptor: AssetBundle.StyleBundle, index: [SortedSet](https://docs.oracle.com/javase/8/docs/api/java/util/SortedSet.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)&gt;?) |

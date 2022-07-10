//[server](../../../index.md)/[elide.server.assets](../index.md)/[ServerAsset](index.md)

# ServerAsset

[jvm]\
sealed class [ServerAsset](index.md)

Describes a server-side asset which is embedded in an application bundle through Elide's asset tools and protocol buffer for asset bundle metadata.

## Parameters

jvm

| | |
|---|---|
| mediaType | Type of media assigned to this asset descriptor. |
| dynamicEligible | Whether this type of media is eligible for dynamic transformation. |

## Types

| Name | Summary |
|---|---|
| [Script](-script/index.md) | [jvm]<br>class [Script](-script/index.md)(descriptor: AssetBundle.ScriptBundle) : [ServerAsset](index.md)<br>Describes a JavaScript asset which is embedded in a given Elide application, and described by Elide's protocol buffer structures; when read from the application bundle and interpreted, this class is used to hold script info. |
| [Stylesheet](-stylesheet/index.md) | [jvm]<br>class [Stylesheet](-stylesheet/index.md)(descriptor: AssetBundle.StyleBundle) : [ServerAsset](index.md)<br>Describes a stylesheet asset which is embedded in a given Elide application, and described by Elide's protocol buffer structures; when read from the application bundle and interpreted, this class is used to hold document info. |
| [Text](-text/index.md) | [jvm]<br>class [Text](-text/index.md)(descriptor: AssetBundle.GenericBundle) : [ServerAsset](index.md)<br>Describes a generic text asset of some kind, for example, `humans.txt` or `robots.txt`; when read from the app bundle and interpreted, this class is used to hold file info. |

## Inheritors

| Name |
|---|
| [Script](-script/index.md) |
| [Stylesheet](-stylesheet/index.md) |
| [Text](-text/index.md) |

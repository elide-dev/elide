//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetPointer](index.md)

# AssetPointer

[jvm]\
data class [AssetPointer](index.md)(val moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), val type: [AssetType](../-asset-type/index.md), val token: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val etag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), val modified: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), val index: [SortedSet](https://docs.oracle.com/javase/8/docs/api/java/util/SortedSet.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)&gt;?) : [Serializable](https://docs.oracle.com/javase/8/docs/api/java/io/Serializable.html)

Reference to an application-embedded asset.

## Parameters

jvm

| | |
|---|---|
| moduleId | Developer-assigned ID for this asset module. |
| type | Type of asset represented by this reference. |
| token | Full-length Asset Tag (referred to as the &quot;asset token&quot;). |
| tag | Generated asset tag (fingerprint) for this asset, in full (untrimmed) form. |
| etag | Computed ETag for this asset. |
| modified | Last-modified time for this asset; set to `-1` to indicate &quot;unknown&quot;. |
| index | Index of the asset within the asset content payload list of the active asset bundle. |

## Constructors

| | |
|---|---|
| [AssetPointer](-asset-pointer.md) | [jvm]<br>fun [AssetPointer](-asset-pointer.md)(moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), type: [AssetType](../-asset-type/index.md), token: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), etag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), modified: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), index: [SortedSet](https://docs.oracle.com/javase/8/docs/api/java/util/SortedSet.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)&gt;?) |

## Properties

| Name | Summary |
|---|---|
| [etag](etag.md) | [jvm]<br>val [etag](etag.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [index](--index--.md) | [jvm]<br>val [index](--index--.md): [SortedSet](https://docs.oracle.com/javase/8/docs/api/java/util/SortedSet.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)&gt;? |
| [modified](modified.md) | [jvm]<br>val [modified](modified.md): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html) |
| [moduleId](module-id.md) | [jvm]<br>val [moduleId](module-id.md): [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467) |
| [tag](tag.md) | [jvm]<br>val [tag](tag.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [token](token.md) | [jvm]<br>val [token](token.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html) |
| [type](type.md) | [jvm]<br>val [type](type.md): [AssetType](../-asset-type/index.md) |

//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetPointer](index.md)/[AssetPointer](-asset-pointer.md)

# AssetPointer

[jvm]\
fun [AssetPointer](-asset-pointer.md)(moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), type: [AssetType](../-asset-type/index.md), token: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), etag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), modified: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), index: [SortedSet](https://docs.oracle.com/javase/8/docs/api/java/util/SortedSet.html)&lt;[Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)&gt;?)

#### Parameters

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

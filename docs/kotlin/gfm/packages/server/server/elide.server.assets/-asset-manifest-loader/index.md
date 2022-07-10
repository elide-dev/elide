//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetManifestLoader](index.md)

# AssetManifestLoader

[jvm]\
@[API](../../../../../packages/base/base/elide.annotations/-a-p-i/index.md)

interface [AssetManifestLoader](index.md)

Defines the API surface of a loader which is capable of searching for an asset manifest at server startup, and then de-serializing the manifest, so it can be indexed and then made available via an [AssetManager](../-asset-manager/index.md) implementation.

The manifest loader is responsible only for two things:

1. 
   **Locating** the manifest on the classpath, if it exists, from a given or default set of candidate locations.
2. 
   **De-serializing** the manifest from some known format, and producing the result as an AssetBundle.

At server startup, the default set of manifest locations ([assetManifestCandidates](../../../../../packages/server/elide.server.assets/-asset-manifest-loader/-companion/asset-manifest-candidates.md)) is searched unless otherwise specified by the developer.

###  Implementation

Typically, this interface is implemented by [ServerAssetManifestProvider](../../../../../packages/server/elide.server.assets/-server-asset-manifest-provider/index.md). The developer may replace this default implementation using the `@Replaces` annotation from Micronaut. For example:

```kotlin
@Replaces(ServerAssetManifestProvider::class)
class MyAssetManifestProvider : AssetManifestLoader {
  // implementation here
}
```

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [findLoadManifest](find-load-manifest.md) | [jvm]<br>open fun [findLoadManifest](find-load-manifest.md)(): AssetBundle?<br>Find and load an asset manifest embedded within the scope of the current application; search in the default path locations, and if no manifest is found, or no manifest could safely be decoded, return `null`.<br>[jvm]<br>abstract fun [findLoadManifest](find-load-manifest.md)(candidates: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;ManifestFormat, [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt;): AssetBundle?<br>Find and load an asset manifest embedded within the scope of the current application, using the provided set of [candidates](find-load-manifest.md) as locations in the classpath to search. |
| [findManifest](find-manifest.md) | [jvm]<br>open fun [findManifest](find-manifest.md)(): [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;ManifestFormat, [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html)&gt;?<br>Find an asset manifest embedded within the scope of the current application; search in the default path locations, and if no manifest is found, return `null`; otherwise, return the expected format of the found manifest, and an input stream to consume it.<br>[jvm]<br>abstract fun [findManifest](find-manifest.md)(candidates: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;ManifestFormat, [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt;): [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;ManifestFormat, [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html)&gt;?<br>Find an asset manifest present as a resource in the application classpath from the set of provided [candidates](find-manifest.md), each of which represents a potential path and ManifestFormat. |

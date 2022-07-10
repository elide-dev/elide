//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetManifestLoader](index.md)/[findLoadManifest](find-load-manifest.md)

# findLoadManifest

[jvm]\
abstract fun [findLoadManifest](find-load-manifest.md)(candidates: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;ManifestFormat, [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt;): AssetBundle?

Find and load an asset manifest embedded within the scope of the current application, using the provided set of [candidates](find-load-manifest.md) as locations in the classpath to search.

Each pair carries a ManifestFormat and path, indicating the expected format of the bundle.

#### Return

Decoded AssetBundle from one of the candidate locations; which location is lost, and any decoding errors are returned as `null`.

## Parameters

jvm

| | |
|---|---|
| candidates | Candidate locations for the server asset manifest. |

[jvm]\
open fun [findLoadManifest](find-load-manifest.md)(): AssetBundle?

Find and load an asset manifest embedded within the scope of the current application; search in the default path locations, and if no manifest is found, or no manifest could safely be decoded, return `null`.

#### Return

Decoded asset manifest, or `null` if no manifest could be found, or a manifest was found and could not be safely de-serialized.

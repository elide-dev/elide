//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetManifestLoader](index.md)/[findManifest](find-manifest.md)

# findManifest

[jvm]\
abstract fun [findManifest](find-manifest.md)(candidates: [List](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/index.html)&lt;[Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;ManifestFormat, [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)&gt;&gt;): [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;ManifestFormat, [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html)&gt;?

Find an asset manifest present as a resource in the application classpath from the set of provided [candidates](find-manifest.md), each of which represents a potential path and ManifestFormat.

If a manifest is found at any of the candidate locations, a pair of ManifestFormat/[InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html) is returned, which can be used to load a de-serializer and run it. If no manifest is found or anything else goes wrong (i.e. a manifest is found but it is in the wrong format), `null` is returned.

#### Return

Pair of the located ManifestFormat and an [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html), or `null` if no manifest was found.

## Parameters

jvm

| | |
|---|---|
| candidates | Candidate paths and ManifestFormat values where we should search for the manifest. |

[jvm]\
open fun [findManifest](find-manifest.md)(): [Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;ManifestFormat, [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html)&gt;?

Find an asset manifest embedded within the scope of the current application; search in the default path locations, and if no manifest is found, return `null`; otherwise, return the expected format of the found manifest, and an input stream to consume it.

#### Return

Pair carrying the found ManifestFormat and an [InputStream](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html) to consume it, or `null` if no manifest could be located in the default classpath resource locations.

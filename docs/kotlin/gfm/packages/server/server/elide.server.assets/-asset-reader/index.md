//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetReader](index.md)

# AssetReader

[jvm]\
@[API](../../../../../packages/base/base/elide.annotations/-a-p-i/index.md)

interface [AssetReader](index.md) : [AssetResolver](../-asset-resolver/index.md)

Describes the API surface expected for a reader of static assets from some data source; responsible for efficiently reading assets from a resource path and producing resulting content.

Asset readers can cache the results of their reads, if desired, and may base caching decisions on the asset descriptors provided when resolving assets.

###  Resource assets

Typically, resources are embedded in the application JAR or native image, in zipped form, alongside embedded SSR assets and compiled JVM or native classes. The main [AssetReader](index.md) implementation knows how to interpret these assets based on a binary-encoded protocol buffer message *also* embedded within the application.

###  Replacing the main reader

The developer may find it desirable to write and provide their own [AssetReader](index.md) implementation, which can be accomplished via Micronaut's DI system (read on below). In particular, this may be a requirement for stable testing of a broader [AssetManager](../-asset-manager/index.md) implementation.

####  Replacing components of the [AssetManager](../-asset-manager/index.md)

To replace the stock [AssetReader](index.md) implementation, Micronaut's `Replaces` annotation can be used:

```kotlin
package your.package.here;

import elide.server.assets.AssetReader;
import io.micronaut.context.annotation.Replaces;

@Replaces(ServerAssetReader::class)
public class MyCoolAssetReader: AssetReader {
  // (impl here)
}
```

## See also

jvm

| | |
|---|---|
| [elide.server.assets.AssetManager](../-asset-manager/index.md) | which coordinates between the [AssetReader](index.md) and [AssetResolver](../-asset-resolver/index.md). |
| [elide.server.assets.RenderedAsset](../-rendered-asset/index.md) | for the generic return value model leveraged by [AssetManager](../-asset-manager/index.md). |
| [elide.server.assets.ServerAsset](../-server-asset/index.md) | for the symbolic asset reference model leveraged by [AssetManager](../-asset-manager/index.md). |

## Functions

| Name | Summary |
|---|---|
| [readAsync](read-async.md) | [jvm]<br>abstract suspend fun [readAsync](read-async.md)(descriptor: [ServerAsset](../-server-asset/index.md)): Deferred&lt;[RenderedAsset](../-rendered-asset/index.md)&gt;<br>Given a resolved asset [descriptor](read-async.md) which should be known to exist, read the associated asset content, and return it as an async Deferred task which can be awaited, and then consumed. |
| [resolve](../-asset-resolver/resolve.md) | [jvm]<br>open fun [resolve](../-asset-resolver/resolve.md)(request: HttpRequest&lt;*&gt;): [ServerAsset](../-server-asset/index.md)?<br>abstract fun [resolve](../-asset-resolver/resolve.md)(path: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [ServerAsset](../-server-asset/index.md)? |

## Inheritors

| Name |
|---|
| [ServerAssetReader](../-server-asset-reader/index.md) |

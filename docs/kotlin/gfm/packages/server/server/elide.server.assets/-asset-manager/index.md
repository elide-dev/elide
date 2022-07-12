//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetManager](index.md)

# AssetManager

[jvm]\
@[API](../../../../../packages/base/base/elide.annotations/-a-p-i/index.md)

interface [AssetManager](index.md)

Describes the API surface of an *Asset Manager*, which controls access to embedded server-side assets such as CSS and JS documents.

The asset manager is responsible for maintaining a runtime registry of all available assets, which is typically built at server startup. Assets should be held in memory so they may easily be resolved, dynamically transformed, and ultimately served to end-users through either user-defined controllers or the built-in asset controller.

###  Asset descriptors

During the app build, the Elide build tools package scripts for use with SSR or client-side serving. After packaging assets from internal and external builds, protocol buffer descriptors are computed and embedded in the application, which describe how each asset should be served, addressed, and treated with regard to dependencies.

At server startup, the encoded descriptors are loaded, assets are interpreted and loaded, and the server refers to the resulting mapping when called upon to serve assets.

###  Offloading

Static assets do exist on disk, technically, but when embedded in an application JAR or native image, they are automatically zipped and inlined into the application binary. Therefore, OS-level tools like `sendfile` aren't an option.

However, Micronaut and KotlinX Coroutines both have built-in IO scheduling support, which is usually backed by a pool of cached POSIX threads. This offers a good alternative for offloading I/O from a user-defined request handler.

###  Customizing the asset system

The [AssetManager](index.md) is a simple interface which mediates between an [AssetResolver](../-asset-resolver/index.md) and [AssetReader](../-asset-reader/index.md) implementation pair to serve assets at mapped HTTP paths. In some cases, the developer may want to customize this behavior, either in the way asset paths are translated or interpreted (via a custom [AssetResolver](../-asset-resolver/index.md)), or the way asset content is loaded and returned (via a custom [AssetReader](../-asset-reader/index.md)). Both can be used at once if needed.

To customize a given asset system component, use the `Replaces` annotation from Micronaut's context module. For example:

```kotlin
package your.package.here;

import elide.server.assets.AssetReader;
import io.micronaut.context.annotation.Replaces;

@Replaces(ServerAssetReader::class)
public class MyCoolAssetReader: AssetReader {
  // (impl here)
}
```

The [AssetManager](index.md) participates in the DI container, so the developer only needs to provide a component definition. Later, when an [AssetManager](index.md) instance is requested by the app, the main implementation will load and use the developer's custom implementation.

## See also

jvm

| | |
|---|---|
| [elide.server.assets.AssetResolver](../-asset-resolver/index.md) | which is responsible for checking, translating, and loading asset paths. |
| [elide.server.assets.AssetReader](../-asset-reader/index.md) | which is responsible for reading asset content efficiently. |
| [elide.server.assets.RenderedAsset](../-rendered-asset/index.md) | for the generic return value model leveraged by [AssetManager](index.md). |
| [elide.server.assets.ServerAsset](../-server-asset/index.md) | for the symbolic asset reference model leveraged by [AssetManager](index.md). |

## Functions

| Name | Summary |
|---|---|
| [findAssetByModuleId](find-asset-by-module-id.md) | [jvm]<br>open fun [findAssetByModuleId](find-asset-by-module-id.md)(asset: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467)): [AssetPointer](../-asset-pointer/index.md)?<br>Resolve an [AssetPointer](../-asset-pointer/index.md) for the specified [asset](find-asset-by-module-id.md) module ID; if none can be located within the current set of live server assets, return `null`. |
| [linkForAsset](link-for-asset.md) | [jvm]<br>abstract fun [linkForAsset](link-for-asset.md)(module: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), overrideType: [AssetType](../-asset-type/index.md)? = null): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Generate a relative link to serve the asset specified by the provided [module](link-for-asset.md) ID; the link is built from the active configured asset prefix, plus the &quot;asset tag,&quot; which is a variable-length cryptographic fingerprint of the asset's content. |
| [renderAssetAsync](render-asset-async.md) | [jvm]<br>abstract suspend fun [renderAssetAsync](render-asset-async.md)(request: HttpRequest&lt;*&gt;, asset: [ServerAsset](../-server-asset/index.md)): Deferred&lt;[StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)&gt;<br>Responsible for converting a known-good asset held by the server into an efficient [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467) which serves the asset to the invoking client. |
| [resolve](resolve.md) | [jvm]<br>open fun [resolve](resolve.md)(request: HttpRequest&lt;*&gt;, moduleId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null): [ServerAsset](../-server-asset/index.md)?<br>Resolve the asset requested by the provided HTTP [request](resolve.md); if the corresponding file cannot be found, return `null`, and otherwise, throw an error. |
| [serveAsync](serve-async.md) | [jvm]<br>open suspend fun [serveAsync](serve-async.md)(request: HttpRequest&lt;*&gt;, moduleId: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)? = null): Deferred&lt;[StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)&gt;<br>Asynchronously produce an HTTP response which serves the asset described by the provided [request](serve-async.md); if the asset in question cannot be located, serve a `404 Not Found`, and for any other error, serve a `500 Internal Server Error`. |
| [serveNotFoundAsync](serve-not-found-async.md) | [jvm]<br>open fun [serveNotFoundAsync](serve-not-found-async.md)(request: HttpRequest&lt;*&gt;): Deferred&lt;[StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)&gt;<br>Serve a response of status HTTP 404 (Not Found), in response to a request for an asset which could not be located by the built-in asset system. |

## Properties

| Name | Summary |
|---|---|
| [logging](logging.md) | [jvm]<br>abstract val [logging](logging.md): Logger<br>Logger which should be used to emit not-found warnings and other messages from the asset manager implementation which is live for the current server lifecycle. |
| [reader](reader.md) | [jvm]<br>abstract val [reader](reader.md): [AssetReader](../-asset-reader/index.md)<br>Asset reader which is in use for this asset manager; responsible for translating an absolute asset resource path into a stream of actual resource content. |

## Inheritors

| Name |
|---|
| [ServerAssetManager](../-server-asset-manager/index.md) |

//[server](../../../index.md)/[elide.server.controller](../index.md)/[PageController](index.md)

# PageController

[jvm]\
abstract class [PageController](index.md) : [BaseController](../-base-controller/index.md)

Defines the built-in concept of a `Page`-type handler, which is capable of performing SSR, serving static assets, and handling page-level RPC calls.

Page controllers use a dual-pronged mechanism to hook into application code. First, the controller annotates with [Page](../../elide.server.annotations/-page/index.md), which provides AOT advice and route bindings; a suite of on-class functions and injections related to page can also then be inherited from [PageController](index.md), although this is only necessary to leverage static asset serving and SSR. Most of these resources are acquired statically, which keeps things fast.

When the developer calls a method like `ssr` or `asset`, for example, the bean context is consulted, and an `AssetManager` or `JsRuntime` is resolved to satisfy the response.

###  Controller lifecycle

Bean objects created within a Micronaut dependency injection context have an associated *scope*, which governs something called the &quot;bean lifecycle.&quot; The bean lifecycle, and by extension, the bean scope, determines when an instance is constructed, how long it survives, and when garbage is collected.

By default, raw Micronaut controllers are API endpoints. For example, the default input/output `Content-Type` is JSON and the lifecycle is set to `Singleton`. This means a controller is initialized the *first time it is accessed*, and then lives for the duration of the server run.

Pages follow this default and provide on-class primitives to the user, via [PageController](index.md), which help with the management of state, caching, sessions, and so forth.

## Constructors

| | |
|---|---|
| [PageController](-page-controller.md) | [jvm]<br>fun [PageController](-page-controller.md)() |

## Types

| Name | Summary |
|---|---|
| [AssetReferenceBuilder](-asset-reference-builder/index.md) | [jvm]<br>inner class [AssetReferenceBuilder](-asset-reference-builder/index.md)<br>Context handler to collect asset configuration. |

## Functions

| Name | Summary |
|---|---|
| [asset](asset.md) | [jvm]<br>fun [asset](asset.md)(module: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), handler: [PageController.AssetReferenceBuilder](-asset-reference-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)? = null): [AssetReference](../../elide.server.assets/-asset-reference/index.md) |
| [assets](assets.md) | [jvm]<br>open override fun [assets](assets.md)(): [AssetManager](../../elide.server.assets/-asset-manager/index.md) |
| [context](context.md) | [jvm]<br>open override fun [context](context.md)(): ApplicationContext |

## Inheritors

| Name |
|---|
| [PageWithProps](../-page-with-props/index.md) |
| [BuiltinController](../../elide.server.controller.builtin/-builtin-controller/index.md) |

## Extensions

| Name | Summary |
|---|---|
| [asset](../../elide.server/asset.md) | [jvm]<br>suspend fun [PageController](index.md).[asset](../../elide.server/asset.md)(request: HttpRequest&lt;*&gt;, moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), type: [AssetType](../../elide.server.assets/-asset-type/index.md)? = null): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Serve an application asset file which is embedded in the application JAR as a registered server asset, from the application resource path `/assets`.<br>[jvm]<br>suspend fun [PageController](index.md).[asset](../../elide.server/asset.md)(request: HttpRequest&lt;*&gt;, type: [AssetType](../../elide.server.assets/-asset-type/index.md)? = null, block: suspend [AssetHandler](../../elide.server/-asset-handler/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Generate a [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467) which serves an asset embedded within the application, and specified by the provided [block](../../elide.server/asset.md); [request](../../elide.server/asset.md) will be considered when producing the response. |
| [html](../../elide.server/html.md) | [jvm]<br>suspend fun [PageController](index.md).[html](../../elide.server/html.md)(block: suspend [HTML](../../../../../packages/server/kotlinx.html/-h-t-m-l/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [RawResponse](../../elide.server/index.md#852884585%2FClasslikes%2F-1343588467)<br>Responds to a client with an HTML response, using specified [block](../../elide.server/html.md) to build an HTML page via Kotlin's HTML DSL. |
| [script](../../elide.server/script.md) | [jvm]<br>suspend fun [PageController](index.md).[script](../../elide.server/script.md)(request: HttpRequest&lt;*&gt;, moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Serve a static script asset embedded within the application, based on the provided [moduleId](../../elide.server/script.md), and customizing the response based on the provided [request](../../elide.server/script.md).<br>[jvm]<br>suspend fun [PageController](index.md).[script](../../elide.server/script.md)(request: HttpRequest&lt;*&gt;, block: [AssetHandler](../../elide.server/-asset-handler/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Serve a static script asset embedded within the application, based on the provided [block](../../elide.server/script.md), which should customize the serving of the script and declare a module ID. |
| [stylesheet](../../elide.server/stylesheet.md) | [jvm]<br>suspend fun [PageController](index.md).[stylesheet](../../elide.server/stylesheet.md)(request: HttpRequest&lt;*&gt;, moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Serve a static stylesheet asset embedded within the application, based on the provided [moduleId](../../elide.server/stylesheet.md), and customizing the response based on the provided [request](../../elide.server/stylesheet.md).<br>[jvm]<br>suspend fun [PageController](index.md).[stylesheet](../../elide.server/stylesheet.md)(request: HttpRequest&lt;*&gt;, block: [AssetHandler](../../elide.server/-asset-handler/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Serve a static stylesheet asset embedded within the application, based on the provided [block](../../elide.server/stylesheet.md), which should customize the serving of the document and declare a module ID. |

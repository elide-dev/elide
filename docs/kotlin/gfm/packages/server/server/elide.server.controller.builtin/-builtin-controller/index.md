//[server](../../../index.md)/[elide.server.controller.builtin](../index.md)/[BuiltinController](index.md)

# BuiltinController

[jvm]\
abstract class [BuiltinController](index.md) : [PageController](../../elide.server.controller/-page-controller/index.md), [StatusEnabledController](../../elide.server.controller/-status-enabled-controller/index.md)

Base class for built-in controllers provided by Elide.

&quot;Built-in&quot; controllers are mounted within the application context by default, and handle events like global `404 Not Found` and upstream call failures.

###  Built-in controllers

Each built-in controller operates at the default `@Singleton` scope, and complies with [StatusEnabledController](../../elide.server.controller/-status-enabled-controller/index.md). As such, state tied to individual requests is not allowed on built-in controllers unless proper synchronization is used.

Users can replace built-in controllers via Micronaut annotations. See below for more.

###  Overriding built-in controllers

To override a built-in controller, implement [BaseController](../../elide.server.controller/-base-controller/index.md) and annotate your class as follows:

```kotlin
@Controller
@Replaces(SomeBuiltinController::class)
class MyController {
  // ...
}
```

###  Default built-in controllers

The following built-in controllers are provided by the framework by default:

- 
   [NotFoundController](../-not-found-controller/index.md): handles `404 Not Found` events by rendering a designated HTML template.
- 
   [ServerErrorController](../-server-error-controller/index.md): handles generic `500 Internal Server Error` events via a designated HTML template.

###  Low-level error handler

General/low-level error handling is provided at the executor level by [UncaughtExceptionHandler](../../elide.server.runtime.jvm/-uncaught-exception-handler/index.md), which can also be customized / replaced via the same mechanism shown above. See docs on that class for more info.

#### See also

jvm

| | |
|---|---|
| [NotFoundController](../-not-found-controller/index.md) | for the built-in controller which handles `404 Not Found` events. |
| [ServerErrorController](../-server-error-controller/index.md) | for the built-in controller which handles generic internal error events. |
| [UncaughtExceptionHandler](../../elide.server.runtime.jvm/-uncaught-exception-handler/index.md) | for customizable background error handling logic. |

## Constructors

| | |
|---|---|
| [BuiltinController](-builtin-controller.md) | [jvm]<br>fun [BuiltinController](-builtin-controller.md)() |

## Functions

| Name | Summary |
|---|---|
| [asset](../../elide.server.controller/-page-controller/asset.md) | [jvm]<br>fun [asset](../../elide.server.controller/-page-controller/asset.md)(module: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), handler: [PageController.AssetReferenceBuilder](../../elide.server.controller/-page-controller/-asset-reference-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)? = null): [AssetReference](../../elide.server.assets/-asset-reference/index.md) |
| [assets](../../elide.server.controller/-page-controller/assets.md) | [jvm]<br>open override fun [assets](../../elide.server.controller/-page-controller/assets.md)(): [AssetManager](../../elide.server.assets/-asset-manager/index.md) |
| [context](../../elide.server.controller/-page-controller/context.md) | [jvm]<br>open override fun [context](../../elide.server.controller/-page-controller/context.md)(): ApplicationContext |
| [handle](handle.md) | [jvm]<br>abstract suspend fun [handle](handle.md)(request: HttpRequest&lt;out [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;): [RawResponse](../../elide.server/index.md#852884585%2FClasslikes%2F-1343588467)<br>Handles a request to this built-in controller. |

## Inheritors

| Name |
|---|
| [NotFoundController](../-not-found-controller/index.md) |
| [ServerErrorController](../-server-error-controller/index.md) |

## Extensions

| Name | Summary |
|---|---|
| [asset](../../elide.server/asset.md) | [jvm]<br>suspend fun [PageController](../../elide.server.controller/-page-controller/index.md).[asset](../../elide.server/asset.md)(request: HttpRequest&lt;*&gt;, moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), type: [AssetType](../../elide.server.assets/-asset-type/index.md)? = null): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Serve an application asset file which is embedded in the application JAR as a registered server asset, from the application resource path `/assets`.<br>[jvm]<br>suspend fun [PageController](../../elide.server.controller/-page-controller/index.md).[asset](../../elide.server/asset.md)(request: HttpRequest&lt;*&gt;, type: [AssetType](../../elide.server.assets/-asset-type/index.md)? = null, block: suspend [AssetHandler](../../elide.server/-asset-handler/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Generate a [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467) which serves an asset embedded within the application, and specified by the provided [block](../../elide.server/asset.md); [request](../../elide.server/asset.md) will be considered when producing the response. |
| [html](../../elide.server/html.md) | [jvm]<br>suspend fun [PageController](../../elide.server.controller/-page-controller/index.md).[html](../../elide.server/html.md)(block: suspend [HTML](../../../../../packages/server/kotlinx.html/-h-t-m-l/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [RawResponse](../../elide.server/index.md#852884585%2FClasslikes%2F-1343588467)<br>Responds to a client with an HTML response, using specified [block](../../elide.server/html.md) to build an HTML page via Kotlin's HTML DSL. |
| [script](../../elide.server/script.md) | [jvm]<br>suspend fun [PageController](../../elide.server.controller/-page-controller/index.md).[script](../../elide.server/script.md)(request: HttpRequest&lt;*&gt;, moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Serve a static script asset embedded within the application, based on the provided [moduleId](../../elide.server/script.md), and customizing the response based on the provided [request](../../elide.server/script.md).<br>[jvm]<br>suspend fun [PageController](../../elide.server.controller/-page-controller/index.md).[script](../../elide.server/script.md)(request: HttpRequest&lt;*&gt;, block: [AssetHandler](../../elide.server/-asset-handler/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Serve a static script asset embedded within the application, based on the provided [block](../../elide.server/script.md), which should customize the serving of the script and declare a module ID. |
| [stylesheet](../../elide.server/stylesheet.md) | [jvm]<br>suspend fun [PageController](../../elide.server.controller/-page-controller/index.md).[stylesheet](../../elide.server/stylesheet.md)(request: HttpRequest&lt;*&gt;, moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Serve a static stylesheet asset embedded within the application, based on the provided [moduleId](../../elide.server/stylesheet.md), and customizing the response based on the provided [request](../../elide.server/stylesheet.md).<br>[jvm]<br>suspend fun [PageController](../../elide.server.controller/-page-controller/index.md).[stylesheet](../../elide.server/stylesheet.md)(request: HttpRequest&lt;*&gt;, block: [AssetHandler](../../elide.server/-asset-handler/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Serve a static stylesheet asset embedded within the application, based on the provided [block](../../elide.server/stylesheet.md), which should customize the serving of the document and declare a module ID. |

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

## See also

jvm

| | |
|---|---|
| [elide.server.controller.builtin.NotFoundController](../-not-found-controller/index.md) | for the built-in controller which handles `404 Not Found` events. |
| [elide.server.controller.builtin.ServerErrorController](../-server-error-controller/index.md) | for the built-in controller which handles generic internal error events. |
| [elide.server.runtime.jvm.UncaughtExceptionHandler](../../elide.server.runtime.jvm/-uncaught-exception-handler/index.md) | for customizable background error handling logic. |

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

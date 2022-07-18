//[server](../../../index.md)/[elide.server.controller.builtin](../index.md)/[ServerErrorController](index.md)

# ServerErrorController

[jvm]\
@[Eager](../../elide.server.annotations/-eager/index.md)

@Controller

class [ServerErrorController](index.md) : [BuiltinController](../-builtin-controller/index.md)

Default built-in controller which handles `500 Internal Server Error` events.

## Constructors

| | |
|---|---|
| [ServerErrorController](-server-error-controller.md) | [jvm]<br>fun [ServerErrorController](-server-error-controller.md)() |

## Functions

| Name | Summary |
|---|---|
| [asset](../../elide.server.controller/-page-controller/asset.md) | [jvm]<br>fun [asset](../../elide.server.controller/-page-controller/asset.md)(module: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), handler: [PageController.AssetReferenceBuilder](../../elide.server.controller/-page-controller/-asset-reference-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)? = null): [AssetReference](../../elide.server.assets/-asset-reference/index.md) |
| [assets](../../elide.server.controller/-page-controller/assets.md) | [jvm]<br>open override fun [assets](../../elide.server.controller/-page-controller/assets.md)(): [AssetManager](../../elide.server.assets/-asset-manager/index.md) |
| [context](../../elide.server.controller/-page-controller/context.md) | [jvm]<br>open override fun [context](../../elide.server.controller/-page-controller/context.md)(): ApplicationContext |
| [handle](handle.md) | [jvm]<br>@Get(value = &quot;/error/internal&quot;, produces = [&quot;text/html&quot;])<br>@Error(status = HttpStatus.INTERNAL_SERVER_ERROR, global = true)<br>open suspend override fun [handle](handle.md)(request: HttpRequest&lt;out [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;): [RawResponse](../../elide.server/index.md#852884585%2FClasslikes%2F-1343588467) |

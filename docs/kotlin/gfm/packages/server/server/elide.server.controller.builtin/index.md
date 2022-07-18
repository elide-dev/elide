//[server](../../index.md)/[elide.server.controller.builtin](index.md)

# Package elide.server.controller.builtin

## Types

| Name | Summary |
|---|---|
| [BuiltinController](-builtin-controller/index.md) | [jvm]<br>abstract class [BuiltinController](-builtin-controller/index.md) : [PageController](../elide.server.controller/-page-controller/index.md), [StatusEnabledController](../elide.server.controller/-status-enabled-controller/index.md)<br>Base class for built-in controllers provided by Elide. |
| [NotFoundController](-not-found-controller/index.md) | [jvm]<br>@[Eager](../elide.server.annotations/-eager/index.md)<br>@Controller<br>class [NotFoundController](-not-found-controller/index.md) : [BuiltinController](-builtin-controller/index.md)<br>Default built-in controller which handles `404 Not Found` events. |
| [ServerErrorController](-server-error-controller/index.md) | [jvm]<br>@[Eager](../elide.server.annotations/-eager/index.md)<br>@Controller<br>class [ServerErrorController](-server-error-controller/index.md) : [BuiltinController](-builtin-controller/index.md)<br>Default built-in controller which handles `500 Internal Server Error` events. |

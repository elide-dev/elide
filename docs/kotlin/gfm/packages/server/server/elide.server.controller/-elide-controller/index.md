//[server](../../../index.md)/[elide.server.controller](../index.md)/[ElideController](index.md)

# ElideController

[jvm]\
interface [ElideController](index.md)

Describes the top-level expected interface for Elide-based controllers; any base class which inherits from this one may be used as a controller, and activated/deactivated with Micronaut annotations (see: `@Controller`).

## Functions

| Name | Summary |
|---|---|
| [assets](assets.md) | [jvm]<br>abstract fun [assets](assets.md)(): [AssetManager](../../elide.server.assets/-asset-manager/index.md) |
| [context](context.md) | [jvm]<br>abstract fun [context](context.md)(): ApplicationContext |

## Inheritors

| Name |
|---|
| [BaseController](../-base-controller/index.md) |

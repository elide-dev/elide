//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetController](index.md)

# AssetController

[jvm]\
@Requires(property = &quot;elide.assets.enabled&quot;, value = &quot;true&quot;)

@Controller(value = &quot;${elide.assets.prefix}&quot;)

class [AssetController](index.md) : [StatusEnabledController](../../elide.server.controller/-status-enabled-controller/index.md)

Built-in controller implementation which bridges the configured asset serving prefix to the active [AssetManager](../-asset-manager/index.md) layer for this server run.

For this controller to be enabled, the configuration value `elide.assets.enabled` needs to be set to `true`. The asset prefix used by this controller is governed by the configuration value `elide.assets.prefix`.

## Constructors

| | |
|---|---|
| [AssetController](-asset-controller.md) | [jvm]<br>fun [AssetController](-asset-controller.md)() |

## Functions

| Name | Summary |
|---|---|
| [assetGet](asset-get.md) | [jvm]<br>@Get<br>suspend fun [assetGet](asset-get.md)(request: HttpRequest&lt;*&gt;): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>TBD |
| [assetOptions](asset-options.md) | [jvm]<br>@Options<br>suspend fun [assetOptions](asset-options.md)(request: HttpRequest&lt;*&gt;): HttpResponse&lt;*&gt;<br>TBD |

//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetController](index.md)

# AssetController

[jvm]\
@Requires(property = &quot;elide.assets.enabled&quot;, notEquals = &quot;false&quot;)

@Controller(value = &quot;${elide.assets.prefix:/_/assets}&quot;)

class [AssetController](index.md)@Injectconstructor(assetManager: [AssetManager](../-asset-manager/index.md)) : [StatusEnabledController](../../elide.server.controller/-status-enabled-controller/index.md)

Built-in controller implementation which bridges the configured asset serving prefix to the active [AssetManager](../-asset-manager/index.md) layer for this server run.

For this controller to be enabled, the configuration value `elide.assets.enabled` needs to be set to `true`. The asset prefix used by this controller is governed by the configuration value `elide.assets.prefix`.

## Parameters

jvm

| | |
|---|---|
| assetManager | Main asset manager which should be used to resolve and serve assets. |

## Constructors

| | |
|---|---|
| [AssetController](-asset-controller.md) | [jvm]<br>@Inject<br>fun [AssetController](-asset-controller.md)(assetManager: [AssetManager](../-asset-manager/index.md)) |

## Functions

| Name | Summary |
|---|---|
| [assetGet](asset-get.md) | [jvm]<br>@Get(value = &quot;/{tag}.{ext}&quot;)<br>suspend fun [assetGet](asset-get.md)(request: HttpRequest&lt;*&gt;, tag: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), ext: [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Handles HTTP `GET` calls to asset endpoints based on &quot;asset tag&quot; values, which are generated at build time, and are typically composed of  8-16 characters from the tail end of the content hash for the asset. |

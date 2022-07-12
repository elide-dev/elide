//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetManager](index.md)/[findAssetByModuleId](find-asset-by-module-id.md)

# findAssetByModuleId

[jvm]\
open fun [findAssetByModuleId](find-asset-by-module-id.md)(asset: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467)): [AssetPointer](../-asset-pointer/index.md)?

Resolve an [AssetPointer](../-asset-pointer/index.md) for the specified [asset](find-asset-by-module-id.md) module ID; if none can be located within the current set of live server assets, return `null`.

#### Return

Pointer to the resulting asset, or `null` if it could not be located.

## Parameters

jvm

| | |
|---|---|
| asset | Asset module ID to resolve. |

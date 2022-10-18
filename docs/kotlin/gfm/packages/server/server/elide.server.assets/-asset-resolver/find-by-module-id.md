//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetResolver](index.md)/[findByModuleId](find-by-module-id.md)

# findByModuleId

[jvm]\
abstract fun [findByModuleId](find-by-module-id.md)(moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467)): [ServerAsset](../-server-asset/index.md)?

Return the asset module corresponding to the provided [moduleId](find-by-module-id.md), if possible, or return `null` to indicate that the asset could not be located.

#### Return

Resolved server asset, or `null`, indicating that the asset could not be located.

#### Parameters

jvm

| | |
|---|---|
| moduleId | ID for the asset module, assigned by the developer. |

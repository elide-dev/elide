//[server](../../../index.md)/[elide.server.assets](../index.md)/[AssetReader](index.md)/[pointerTo](pointer-to.md)

# pointerTo

[jvm]\
abstract fun [pointerTo](pointer-to.md)(moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467)): [AssetPointer](../-asset-pointer/index.md)?

Resolve a reference to an asset identified by the provided [moduleId](pointer-to.md), in the form of an [AssetPointer](../-asset-pointer/index.md); if no matching asset can be found, return `null` to indicate a not-found failure.

#### Return

Asset pointer resolved for the provided [moduleId](pointer-to.md), or `null`.

#### Parameters

jvm

| | |
|---|---|
| moduleId | ID of the module which we should resolve from the active asset bundle. |

//[server](../../../index.md)/[elide.server](../index.md)/[AssetHandler](index.md)

# AssetHandler

[jvm]\
class [AssetHandler](index.md)(initialExpectedType: [AssetType](../../elide.server.assets/-asset-type/index.md)? = null, handler: [ElideController](../../elide.server.controller/-elide-controller/index.md), request: HttpRequest&lt;*&gt;, moduleId: [AtomicReference](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/AtomicReference.html)&lt;[AssetModuleId](../index.md#-803173189%2FClasslikes%2F-1343588467)?&gt; = AtomicReference(null), expectedType: [AtomicReference](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/AtomicReference.html)&lt;[AssetType](../../elide.server.assets/-asset-type/index.md)?&gt; = AtomicReference(initialExpectedType)) : [BaseResponseHandler](../-base-response-handler/index.md)&lt;[StreamedAsset](../index.md#-1290834015%2FClasslikes%2F-1343588467)&gt;

## Constructors

| | |
|---|---|
| [AssetHandler](-asset-handler.md) | [jvm]<br>fun [AssetHandler](-asset-handler.md)(initialExpectedType: [AssetType](../../elide.server.assets/-asset-type/index.md)? = null, handler: [ElideController](../../elide.server.controller/-elide-controller/index.md), request: HttpRequest&lt;*&gt;, moduleId: [AtomicReference](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/AtomicReference.html)&lt;[AssetModuleId](../index.md#-803173189%2FClasslikes%2F-1343588467)?&gt; = AtomicReference(null), expectedType: [AtomicReference](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/AtomicReference.html)&lt;[AssetType](../../elide.server.assets/-asset-type/index.md)?&gt; = AtomicReference(initialExpectedType)) |

## Functions

| Name | Summary |
|---|---|
| [assetType](asset-type.md) | [jvm]<br>fun [assetType](asset-type.md)(type: [AssetType](../../elide.server.assets/-asset-type/index.md))<br>Declare the expected asset type for this handler. Optional. |
| [module](module.md) | [jvm]<br>fun [module](module.md)(id: [AssetModuleId](../index.md#-803173189%2FClasslikes%2F-1343588467), type: [AssetType](../../elide.server.assets/-asset-type/index.md)? = null)<br>Bind an asset handler to an asset module ID. |
| [respond](index.md#1153369062%2FFunctions%2F-1343588467) | [jvm]<br>open suspend override fun [respond](index.md#1153369062%2FFunctions%2F-1343588467)(response: HttpResponse&lt;[StreamedAsset](../index.md#-1290834015%2FClasslikes%2F-1343588467)&gt;) |

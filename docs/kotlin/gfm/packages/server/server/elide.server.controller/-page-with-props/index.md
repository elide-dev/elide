//[server](../../../index.md)/[elide.server.controller](../index.md)/[PageWithProps](index.md)

# PageWithProps

[jvm]\
abstract class [PageWithProps](index.md)&lt;[State](index.md)&gt; : [PageController](../-page-controller/index.md)

Extends [PageController](../-page-controller/index.md) with support for page-level [State](index.md), produced via the [props](props.md) method; computed state is shared with VM render executions, and can additionally be injected into the page for use by frontend code (typically to hydrate a server-rendered UI).

###  Defining custom page state

When extending `PageWithProps`, a [State](index.md) class must be provided which follows a set of requirements. All [State](index.md) classes must:

- 
   Be annotated with kotlinx.serialization.Serializable to facilitate DOM injection of prop structure. Annotating a class with `Serializable` has its own set of requirements; see the Kotlin Serialization Guide for more info.
- 
   Annotated with HostAccess.Export for each SSR-available property -- this can occur at the top level of a tree of properties, for instance

An example of compliant custom page [State](index.md):

```kotlin
@Serializable
data class HelloProps(
  @get:HostAccess.Export val message: String = "Hello World!",
)
```

And providing that state via the [PageWithProps](index.md) controller:

```kotlin
@Page class HelloPage : PageWithProps<HelloProps>(HelloProps.serializer()) {
  override suspend fun props(): HelloProps {
    // ...
  }
}
```

###  Using state from SSR executions

When running guest language code for SSR, for instance JavaScript, [State](index.md) is made available and can be loaded using frontend tools -- for instance, elide.js.ssr.boot:

```kotlin
boot<HelloProps> { props ->
  // ...
}
```

Optionally, the developer can load the inlined server-side props on their own, via a known DOM ID:

```js
JSON.parse(document.getElementById("ssr-data").textContent || '{}')
```

In SSR mode, elide.js.ssr.boot will read the props (if any), and provide them to the entrypoint for the application so initial render or hydration may be performed, based on the active serving mode.

#### Parameters

jvm

| | |
|---|---|
| State | Represents the serializable property state associated with this controller. [propsAsync](props-async.md) is executed to produce the rendered set of page props. If no state is needed, use [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html). |
| serializer | Kotlin serializer to use for the state attached to this controller. |
| defaultState | Default state value to inject, if any. |

## Functions

| Name | Summary |
|---|---|
| [asset](../-page-controller/asset.md) | [jvm]<br>fun [asset](../-page-controller/asset.md)(module: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), handler: [PageController.AssetReferenceBuilder](../-page-controller/-asset-reference-builder/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)? = null): [AssetReference](../../elide.server.assets/-asset-reference/index.md) |
| [assets](../-page-controller/assets.md) | [jvm]<br>open override fun [assets](../-page-controller/assets.md)(): [AssetManager](../../elide.server.assets/-asset-manager/index.md) |
| [context](../-page-controller/context.md) | [jvm]<br>open override fun [context](../-page-controller/context.md)(): ApplicationContext |
| [finalizeAsync](finalize-async.md) | [jvm]<br>open suspend fun [finalizeAsync](finalize-async.md)(state: [RequestState](../../elide.server.type/-request-state/index.md)): Deferred&lt;[Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;[State](index.md)?, [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?&gt;&gt;<br>&quot;Finalize&quot; the state for this controller, by (1) computing the state, if necessary, and (2) serializing it for embedding into the page; frontend tools can then read this state to hydrate the UI without causing additional calls to the server. |
| [props](props.md) | [jvm]<br>open suspend fun [props](props.md)(request: [RequestState](../../elide.server.type/-request-state/index.md)): [State](index.md)?<br>Compute the server-side [State](index.md) (also referred to as &quot;props&quot;) which should be active for the lifetime of the current request; developer-provided props must follow guidelines to be usable safely (see below). |
| [propsAsync](props-async.md) | [jvm]<br>open suspend fun [propsAsync](props-async.md)(request: [RequestState](../../elide.server.type/-request-state/index.md)): Deferred&lt;[State](index.md)?&gt;<br>Asynchronously compute the server-side [State](index.md) (also referred to as &quot;props&quot;) which should be active for the lifetime of the current request; developer-provided props must follow guidelines to be usable safely (see below). |

## Extensions

| Name | Summary |
|---|---|
| [asset](../../elide.server/asset.md) | [jvm]<br>suspend fun [PageController](../-page-controller/index.md).[asset](../../elide.server/asset.md)(request: HttpRequest&lt;*&gt;, moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467), type: [AssetType](../../elide.server.assets/-asset-type/index.md)? = null): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Serve an application asset file which is embedded in the application JAR as a registered server asset, from the application resource path `/assets`.<br>[jvm]<br>suspend fun [PageController](../-page-controller/index.md).[asset](../../elide.server/asset.md)(request: HttpRequest&lt;*&gt;, type: [AssetType](../../elide.server.assets/-asset-type/index.md)? = null, block: suspend [AssetHandler](../../elide.server/-asset-handler/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Generate a [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467) which serves an asset embedded within the application, and specified by the provided [block](../../elide.server/asset.md); [request](../../elide.server/asset.md) will be considered when producing the response. |
| [html](../../elide.server/html.md) | [jvm]<br>suspend fun [PageController](../-page-controller/index.md).[html](../../elide.server/html.md)(block: suspend [HTML](../../../../../packages/server/kotlinx.html/-h-t-m-l/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [RawResponse](../../elide.server/index.md#852884585%2FClasslikes%2F-1343588467)<br>Responds to a client with an HTML response, using specified [block](../../elide.server/html.md) to build an HTML page via Kotlin's HTML DSL. |
| [script](../../elide.server/script.md) | [jvm]<br>suspend fun [PageController](../-page-controller/index.md).[script](../../elide.server/script.md)(request: HttpRequest&lt;*&gt;, moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Serve a static script asset embedded within the application, based on the provided [moduleId](../../elide.server/script.md), and customizing the response based on the provided [request](../../elide.server/script.md).<br>[jvm]<br>suspend fun [PageController](../-page-controller/index.md).[script](../../elide.server/script.md)(request: HttpRequest&lt;*&gt;, block: [AssetHandler](../../elide.server/-asset-handler/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Serve a static script asset embedded within the application, based on the provided [block](../../elide.server/script.md), which should customize the serving of the script and declare a module ID. |
| [stylesheet](../../elide.server/stylesheet.md) | [jvm]<br>suspend fun [PageController](../-page-controller/index.md).[stylesheet](../../elide.server/stylesheet.md)(request: HttpRequest&lt;*&gt;, moduleId: [AssetModuleId](../../elide.server/index.md#-803173189%2FClasslikes%2F-1343588467)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Serve a static stylesheet asset embedded within the application, based on the provided [moduleId](../../elide.server/stylesheet.md), and customizing the response based on the provided [request](../../elide.server/stylesheet.md).<br>[jvm]<br>suspend fun [PageController](../-page-controller/index.md).[stylesheet](../../elide.server/stylesheet.md)(request: HttpRequest&lt;*&gt;, block: [AssetHandler](../../elide.server/-asset-handler/index.md).() -&gt; [Unit](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)): [StreamedAssetResponse](../../elide.server/index.md#-491452832%2FClasslikes%2F-1343588467)<br>Serve a static stylesheet asset embedded within the application, based on the provided [block](../../elide.server/stylesheet.md), which should customize the serving of the document and declare a module ID. |

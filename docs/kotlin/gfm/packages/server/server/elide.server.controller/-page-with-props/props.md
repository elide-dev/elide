//[server](../../../index.md)/[elide.server.controller](../index.md)/[PageWithProps](index.md)/[props](props.md)

# props

[jvm]\
open suspend fun [props](props.md)(request: [RequestState](../../elide.server.type/-request-state/index.md)): [State](index.md)?

Compute the server-side [State](index.md) (also referred to as &quot;props&quot;) which should be active for the lifetime of the current request; developer-provided props must follow guidelines to be usable safely (see below).

When performing blocking work to compute page properties, implementations should suspend. Both the async and synchronous versions of this method are available for the developer to override (prefer [props](props.md)). If no state is provided by the developer, `null` is returned.

If the developer overrides the method but opts to throw instead, [UnsupportedOperationException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unsupported-operation-exception/index.html) should be thrown, which is caught and translated into `null` state.

To use a given class as server-side [State](index.md), it must:

- 
   Be annotated with kotlinx.serialization.Serializable to facilitate DOM injection of prop structure. Annotating a class with `Serializable` has its own set of requirements; see the Kotlin Serialization Guide for more info.
- 
   Annotated with HostAccess.Export for each SSR-available property -- this can occur at the top level of a tree of properties, for instance

#### Return

State that should be active for this cycle, or `null` if no state is provided or available.

#### See also

jvm

| | |
|---|---|
| [PageWithProps.propsAsync](props-async.md) | for the asynchronous version of this same method (available, but not recommended). |

#### Parameters

jvm

| | |
|---|---|
| request | Computed request state for this request/response cycle. |

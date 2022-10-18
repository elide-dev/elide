//[server](../../../index.md)/[elide.server.controller](../index.md)/[PageWithProps](index.md)/[finalizeAsync](finalize-async.md)

# finalizeAsync

[jvm]\
open suspend fun [finalizeAsync](finalize-async.md)(state: [RequestState](../../elide.server.type/-request-state/index.md)): Deferred&lt;[Pair](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/index.html)&lt;[State](index.md)?, [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)?&gt;&gt;

&quot;Finalize&quot; the state for this controller, by (1) computing the state, if necessary, and (2) serializing it for embedding into the page; frontend tools can then read this state to hydrate the UI without causing additional calls to the server.

#### Return

Deferred task which resolves to a pair, where the first item is the [State](index.md) procured for this cycle via the [props](props.md) and [propsAsync](props-async.md) methods, and the second item is the same state, serialized as a JSON [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html). If no state is available, both pair members are `null`.

#### Parameters

jvm

| | |
|---|---|
| state | Materialized HTTP request state for this cycle. |

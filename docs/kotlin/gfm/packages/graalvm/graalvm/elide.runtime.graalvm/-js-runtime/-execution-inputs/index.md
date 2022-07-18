//[graalvm](../../../../index.md)/[elide.runtime.graalvm](../../index.md)/[JsRuntime](../index.md)/[ExecutionInputs](index.md)

# ExecutionInputs

[jvm]\
class [ExecutionInputs](index.md)&lt;[State](index.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt;(val data: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?&gt; = ConcurrentSkipListMap())

Describes inputs to be made available during a VM execution.

## Constructors

| | |
|---|---|
| [ExecutionInputs](-execution-inputs.md) | [jvm]<br>fun [ExecutionInputs](-execution-inputs.md)(data: [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?&gt; = ConcurrentSkipListMap()) |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |

## Functions

| Name | Summary |
|---|---|
| [context](context.md) | [jvm]<br>fun [context](context.md)(): [RequestState](../../../../../../packages/server/server/elide.server.type/-request-state/index.md)?<br>Host access to fetch the current context; if no execution context is available, `null` is returned. |
| [state](state.md) | [jvm]<br>fun [state](state.md)(): [State](index.md)?<br>Host access to fetch the current state; if no state is available, `null` is returned. |

## Properties

| Name | Summary |
|---|---|
| [data](data.md) | [jvm]<br>val [data](data.md): [Map](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)&lt;[String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html), [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)?&gt; |

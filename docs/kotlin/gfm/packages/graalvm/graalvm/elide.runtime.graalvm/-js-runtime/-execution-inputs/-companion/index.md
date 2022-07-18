//[graalvm](../../../../../index.md)/[elide.runtime.graalvm](../../../index.md)/[JsRuntime](../../index.md)/[ExecutionInputs](../index.md)/[Companion](index.md)

# Companion

[jvm]\
object [Companion](index.md)

## Functions

| Name | Summary |
|---|---|
| [fromRequestState](from-request-state.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>fun &lt;[State](from-request-state.md) : [Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; [fromRequestState](from-request-state.md)(context: [RequestState](../../../../../../../packages/server/server/elide.server.type/-request-state/index.md), state: [State](from-request-state.md)?): [JsRuntime.ExecutionInputs](../index.md)&lt;[State](from-request-state.md)&gt; |

## Properties

| Name | Summary |
|---|---|
| [CONTEXT](-c-o-n-t-e-x-t.md) | [jvm]<br>const val [CONTEXT](-c-o-n-t-e-x-t.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Key where combined state is placed in the execution input data map. |
| [EMPTY](-e-m-p-t-y.md) | [jvm]<br>val [EMPTY](-e-m-p-t-y.md): [JsRuntime.ExecutionInputs](../index.md)&lt;[Any](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-any/index.html)&gt; |
| [STATE](-s-t-a-t-e.md) | [jvm]<br>const val [STATE](-s-t-a-t-e.md): [String](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)<br>Key where shared state is placed in the execution input data map. |

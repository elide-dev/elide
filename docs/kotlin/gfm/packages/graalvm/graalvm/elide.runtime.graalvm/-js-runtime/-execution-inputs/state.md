//[graalvm](../../../../index.md)/[elide.runtime.graalvm](../../index.md)/[JsRuntime](../index.md)/[ExecutionInputs](index.md)/[state](state.md)

# state

[jvm]\
fun [state](state.md)(): [State](index.md)?

Host access to fetch the current state; if no state is available, `null` is returned.

The &quot;state&quot; for an execution is modeled by the developer, via a serializable data class. If state is provided, then it is made available to the JavaScript context.

#### Return

Instance of execution state provided at invocation time, or `null`.

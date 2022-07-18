//[graalvm](../../../../index.md)/[elide.runtime.graalvm](../../index.md)/[JsRuntime](../index.md)/[ExecutionInputs](index.md)/[context](context.md)

# context

[jvm]\
fun [context](context.md)(): [RequestState](../../../../../../packages/server/server/elide.server.type/-request-state/index.md)?

Host access to fetch the current context; if no execution context is available, `null` is returned.

The &quot;context&quot; is modeled by the [RequestState](../../../../../../packages/server/server/elide.server.type/-request-state/index.md) class, which provides a consistent structure with guest language accessors for notable context properties, such as the active HTTP request.

#### Return

Instance of execution context provided at invocation time, or `null`.

//[server](../../../../index.md)/[elide.server.runtime](../../index.md)/[AppExecutor](../index.md)/[Companion](index.md)/[unconfined](unconfined.md)

# unconfined

[jvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

suspend fun &lt;[R](unconfined.md)&gt; [unconfined](unconfined.md)(operation: suspend () -&gt; [R](unconfined.md)): [R](unconfined.md)

Run the provided [operation](unconfined.md) in unconfined mode, where it will start a co-routine in the caller thread, but only until the first suspension point.

The operation is executed against the &quot;unconfined&quot; dispatcher (Dispatchers.Unconfined). For more about confined versus unconfined co-routines, see here:

https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html#unconfined-vs-confined-dispatcher

#### Return

Deferred task providing the result of the [operation](unconfined.md).

## Parameters

jvm

| | |
|---|---|
| R | Return type. |
| operation | Operation to run. Can suspend. |

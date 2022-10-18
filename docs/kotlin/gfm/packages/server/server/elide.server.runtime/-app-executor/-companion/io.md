//[server](../../../../index.md)/[elide.server.runtime](../../index.md)/[AppExecutor](../index.md)/[Companion](index.md)/[io](io.md)

# io

[jvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

suspend fun &lt;[R](io.md)&gt; [io](io.md)(operation: suspend () -&gt; [R](io.md)): [R](io.md)

Run the provided I/O [operation](io.md), returning whatever result is returned by the [operation](io.md).

The operation is executed against the I/O dispatcher (Dispatchers.IO).

#### Return

Deferred task providing the result of the [operation](io.md).

#### Parameters

jvm

| | |
|---|---|
| R | Return type. |
| operation | Operation to run. Can suspend. |

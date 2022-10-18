//[server](../../../../index.md)/[elide.server.runtime](../../index.md)/[AppExecutor](../index.md)/[Companion](index.md)/[async](async.md)

# async

[jvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

suspend fun &lt;[R](async.md)&gt; [async](async.md)(operation: suspend () -&gt; [R](async.md)): Deferred&lt;[R](async.md)&gt;

Run the provided [operation](async.md) asynchronously, returning whatever result is returned by the [operation](async.md).

The operation is executed against the default dispatcher (Dispatchers.Default).

#### Return

Deferred task providing the result of the [operation](async.md).

#### Parameters

jvm

| | |
|---|---|
| R | Return type. |
| operation | Operation to run. Can suspend. |

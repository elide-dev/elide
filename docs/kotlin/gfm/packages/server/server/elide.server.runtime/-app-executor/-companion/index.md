//[server](../../../../index.md)/[elide.server.runtime](../../index.md)/[AppExecutor](../index.md)/[Companion](index.md)

# Companion

[jvm]\
object [Companion](index.md)

## Functions

| Name | Summary |
|---|---|
| [async](async.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>suspend fun &lt;[R](async.md)&gt; [async](async.md)(operation: suspend () -&gt; [R](async.md)): Deferred&lt;[R](async.md)&gt;<br>Run the provided [operation](async.md) asynchronously, returning whatever result is returned by the [operation](async.md). |
| [io](io.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>suspend fun &lt;[R](io.md)&gt; [io](io.md)(operation: suspend () -&gt; [R](io.md)): [R](io.md)<br>Run the provided I/O [operation](io.md), returning whatever result is returned by the [operation](io.md). |
| [main](main.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>suspend fun &lt;[R](main.md)&gt; [main](main.md)(operation: suspend () -&gt; [R](main.md)): [R](main.md)<br>Run the provided [operation](main.md) on the main thread, returning whatever result is returned by the [operation](main.md). |
| [unconfined](unconfined.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>suspend fun &lt;[R](unconfined.md)&gt; [unconfined](unconfined.md)(operation: suspend () -&gt; [R](unconfined.md)): [R](unconfined.md)<br>Run the provided [operation](unconfined.md) in unconfined mode, where it will start a co-routine in the caller thread, but only until the first suspension point. |

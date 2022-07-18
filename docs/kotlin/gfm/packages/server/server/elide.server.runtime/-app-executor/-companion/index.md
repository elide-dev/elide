//[server](../../../../index.md)/[elide.server.runtime](../../index.md)/[AppExecutor](../index.md)/[Companion](index.md)

# Companion

[jvm]\
object [Companion](index.md)

## Functions

| Name | Summary |
|---|---|
| [async](async.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>suspend fun &lt;[R](async.md)&gt; [async](async.md)(operation: suspend () -&gt; [R](async.md)): Deferred&lt;[R](async.md)&gt;<br>Run the provided [operation](async.md) asynchronously, returning whatever result is returned by the [operation](async.md). |
| [io](io.md) | [jvm]<br>@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)<br>suspend fun &lt;[R](io.md)&gt; [io](io.md)(operation: suspend () -&gt; [R](io.md)): [R](io.md)<br>Run the provided I/O [operation](io.md), returning whatever result is returned by the [operation](io.md). |

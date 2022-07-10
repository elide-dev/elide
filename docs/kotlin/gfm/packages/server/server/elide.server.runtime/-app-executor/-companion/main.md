//[server](../../../../index.md)/[elide.server.runtime](../../index.md)/[AppExecutor](../index.md)/[Companion](index.md)/[main](main.md)

# main

[jvm]\

@[JvmStatic](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-jvm-static/index.html)

suspend fun &lt;[R](main.md)&gt; [main](main.md)(operation: suspend () -&gt; [R](main.md)): [R](main.md)

Run the provided [operation](main.md) on the main thread, returning whatever result is returned by the [operation](main.md).

The operation is executed against the main dispatcher (Dispatchers.Main).

#### Return

Deferred task providing the result of the [operation](main.md).

## Parameters

jvm

| | |
|---|---|
| R | Return type. |
| operation | Operation to run. Can suspend. |

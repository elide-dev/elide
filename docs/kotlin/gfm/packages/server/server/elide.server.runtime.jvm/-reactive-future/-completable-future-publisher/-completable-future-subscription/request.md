//[server](../../../../../index.md)/[elide.server.runtime.jvm](../../../index.md)/[ReactiveFuture](../../index.md)/[CompletableFuturePublisher](../index.md)/[CompletableFutureSubscription](index.md)/[request](request.md)

# request

[jvm]\

@[Synchronized](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-synchronized/index.html)

open override fun [request](request.md)(n: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html))

Request the specified number of items from the underlying Subscription. This must **always be

<pre>1</pre>**.

## Parameters

jvm

| | |
|---|---|
| n | Number of elements to request to the upstream (must always be <pre>1</pre>). |

## Throws

| | |
|---|---|
| [kotlin.IllegalArgumentException](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-illegal-argument-exception/index.html) | If any value other than <pre>1</pre> is passed in. |

//[server](../../../../index.md)/[elide.server.runtime.jvm](../../index.md)/[ReactiveFuture](../index.md)/[PublisherListenableFuture](index.md)

# PublisherListenableFuture

[jvm]\
@Immutable

@ThreadSafe

class [PublisherListenableFuture](index.md)&lt;[T](index.md)&gt; : ListenableFuture&lt;[T](index.md)&gt; , Publisher&lt;[T](index.md)&gt; 

Structure that adapts a Publisher to a ListenableFuture interface. We accomplish this by immediately subscribing to the publisher with a callback that dispatches a SettableFuture.

This object is used in the specific circumstance of wrapping a Publisher, and then using the wrapped object as a ListenableFuture (or any descendent or compliant implementation thereof).

## Parameters

jvm

| | |
|---|---|
|  | <T> Generic type returned by the future. </T> |

## Functions

| Name | Summary |
|---|---|
| [addListener](add-listener.md) | [jvm]<br>open override fun [addListener](add-listener.md)(runnable: [Runnable](https://docs.oracle.com/javase/8/docs/api/java/lang/Runnable.html), executor: [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html)) |
| [cancel](cancel.md) | [jvm]<br>open override fun [cancel](cancel.md)(mayInterruptIfRunning: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [get](get.md) | [jvm]<br>open override fun [get](get.md)(): [T](index.md)<br>open operator override fun [get](get.md)(timeout: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), unit: [TimeUnit](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeUnit.html)): [T](index.md) |
| [isCancelled](is-cancelled.md) | [jvm]<br>open override fun [isCancelled](is-cancelled.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [isDone](is-done.md) | [jvm]<br>open override fun [isDone](is-done.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [subscribe](subscribe.md) | [jvm]<br>open override fun [subscribe](subscribe.md)(s: Subscriber&lt;in [T](index.md)&gt;) |

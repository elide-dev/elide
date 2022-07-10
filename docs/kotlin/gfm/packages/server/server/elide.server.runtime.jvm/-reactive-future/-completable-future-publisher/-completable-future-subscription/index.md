//[server](../../../../../index.md)/[elide.server.runtime.jvm](../../../index.md)/[ReactiveFuture](../../index.md)/[CompletableFuturePublisher](../index.md)/[CompletableFutureSubscription](index.md)

# CompletableFutureSubscription

[jvm]\
@Immutable

@ThreadSafe

inner class [CompletableFutureSubscription](index.md) : Subscription

Models a Reactive Java Subscription, which is responsible for propagating events from a Concurrent Java [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) to a Subscriber.

This object is generally used internally by the [CompletableFuturePublisher](../index.md), once a Subscriber attaches itself to a Publisher that is actually a wrapped [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html). Error (exception) events and value events are both propagated. Subscribers based on this wrapping will only ever receive a maximum of **one value** or **one error**.

## Functions

| Name | Summary |
|---|---|
| [cancel](cancel.md) | [jvm]<br>@[Synchronized](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-synchronized/index.html)<br>open override fun [cancel](cancel.md)()<br>Request the publisher to stop sending data and clean up resources. |
| [request](request.md) | [jvm]<br>@[Synchronized](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-synchronized/index.html)<br>open override fun [request](request.md)(n: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html))<br>Request the specified number of items from the underlying Subscription. This must **always be |

//[server](../../../../../index.md)/[elide.server.runtime.jvm](../../../index.md)/[ReactiveFuture](../../index.md)/[ListenableFuturePublisher](../index.md)/[ListenableFutureSubscription](index.md)

# ListenableFutureSubscription

[jvm]\
@Immutable

@ThreadSafe

inner class [ListenableFutureSubscription](index.md) : Subscription

Models a Reactive Java Subscription, which is responsible for propagating events from a ListenableFuture to a Subscriber.

This object is generally used internally by the [ListenableFuturePublisher](../index.md), once a Subscriber attaches itself to a Publisher that is actually a wrapped ListenableFuture. Error (exception) events and value events are both propagated. Subscribers based on this wrapping will only ever receive a maximum of **one value** or **one error**.

## Functions

| Name | Summary |
|---|---|
| [cancel](cancel.md) | [jvm]<br>@[Synchronized](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-synchronized/index.html)<br>open override fun [cancel](cancel.md)()<br>Request the publisher to stop sending data and clean up resources. |
| [request](request.md) | [jvm]<br>@[Synchronized](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-synchronized/index.html)<br>open override fun [request](request.md)(n: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html))<br>Request the specified number of items from the underlying Subscription. This must **always be |

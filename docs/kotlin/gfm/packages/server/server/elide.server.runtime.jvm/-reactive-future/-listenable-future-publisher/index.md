//[server](../../../../index.md)/[elide.server.runtime.jvm](../../index.md)/[ReactiveFuture](../index.md)/[ListenableFuturePublisher](index.md)

# ListenableFuturePublisher

[jvm]\
class [ListenableFuturePublisher](index.md)&lt;[T](index.md)&gt; : Publisher&lt;[T](index.md)&gt; 

Structure that adapts Guava's ListenableFuture to a Reactive Java Publisher, which publishes one item - either the result of the computation, or an error.

This object is used in the specific circumstance that a ListenableFuture is wrapped by a [ReactiveFuture](../index.md), and then used within the Reactive Java ecosystem as a Publisher. We simply set a callback for the future value, upon item-request (one cycle is allowed), and propagate any events received to the publisher.

## Parameters

jvm

| | |
|---|---|
|  | <T> Emit type for this adapter. Matches the publisher it wraps. </T> |

## Types

| Name | Summary |
|---|---|
| [ListenableFutureSubscription](-listenable-future-subscription/index.md) | [jvm]<br>@Immutable<br>@ThreadSafe<br>inner class [ListenableFutureSubscription](-listenable-future-subscription/index.md) : Subscription<br>Models a Reactive Java Subscription, which is responsible for propagating events from a ListenableFuture to a Subscriber. |

## Functions

| Name | Summary |
|---|---|
| [subscribe](subscribe.md) | [jvm]<br>open override fun [subscribe](subscribe.md)(subscriber: Subscriber&lt;in [T](index.md)&gt;) |

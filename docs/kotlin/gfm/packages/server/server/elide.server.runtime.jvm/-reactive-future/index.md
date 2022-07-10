//[server](../../../index.md)/[elide.server.runtime.jvm](../index.md)/[ReactiveFuture](index.md)

# ReactiveFuture

[jvm]\
@Immutable

@ThreadSafe

class [ReactiveFuture](index.md)&lt;[R](index.md)&gt; : Publisher&lt;[R](index.md)?&gt; , ListenableFuture&lt;[R](index.md)?&gt; , ApiFuture&lt;[R](index.md)?&gt; 

Adapts future/async value containers from different frameworks (namely, Reactive Java, Guava, and the JDK).

Create a new `ReactiveFuture` by using any of the [wrap](-companion/wrap.md) factory methods. The resulting object is usable as a Publisher, ListenableFuture, or ApiFuture (from GAX). This object simply wraps whatever inner object is provided, and as such instances are lightweight; there is no default functionality after immediate construction in most cases.</p>

**Caveat:** when using a Publisher as a ListenableFuture (i.e. wrapping a {@link Publisher} and then using any of the typical future methods, like ListenableFuture.addListener, the underlying publisher may not publish more than one value. This is to prevent dropping intermediate values on the floor, silently, before dispatching the future's callbacks, which generally only accept one value. Other than this, things should work &quot;as expected&quot; whether you're looking at them from a Guava, JDK, or Reactive perspective.

## See also

jvm

| | |
|---|---|
| org.reactivestreams.Publisher | Reactive Java type adapted by this object. |
| com.google.common.util.concurrent.ListenableFuture | Guava's extension of the JDK's basic {@link Future}, which adds listener support. |
| com.google.api.core.ApiFuture | Lightweight Guava-like future meant to avoid dependencies on Java in API libraries. |
| [elide.server.runtime.jvm.ReactiveFuture.Companion](-companion/wrap.md) | To wrap a Publisher, ListenableFuture, or ApiFuture. |

## Types

| Name | Summary |
|---|---|
| [Companion](-companion/index.md) | [jvm]<br>object [Companion](-companion/index.md) |
| [CompletableFuturePublisher](-completable-future-publisher/index.md) | [jvm]<br>class [CompletableFuturePublisher](-completable-future-publisher/index.md)&lt;[T](-completable-future-publisher/index.md)&gt; : Publisher&lt;[T](-completable-future-publisher/index.md)&gt; , ListenableFuture&lt;[T](-completable-future-publisher/index.md)&gt; , [CompletionStage](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletionStage.html)&lt;[T](-completable-future-publisher/index.md)&gt; <br>Structure that adapts Java's [CompletableFuture](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html) to a Reactive Java Publisher, which publishes one item - either the result of the computation, or an error. |
| [ListenableFuturePublisher](-listenable-future-publisher/index.md) | [jvm]<br>class [ListenableFuturePublisher](-listenable-future-publisher/index.md)&lt;[T](-listenable-future-publisher/index.md)&gt; : Publisher&lt;[T](-listenable-future-publisher/index.md)&gt; <br>Structure that adapts Guava's ListenableFuture to a Reactive Java Publisher, which publishes one item - either the result of the computation, or an error. |
| [PublisherListenableFuture](-publisher-listenable-future/index.md) | [jvm]<br>@Immutable<br>@ThreadSafe<br>class [PublisherListenableFuture](-publisher-listenable-future/index.md)&lt;[T](-publisher-listenable-future/index.md)&gt; : ListenableFuture&lt;[T](-publisher-listenable-future/index.md)&gt; , Publisher&lt;[T](-publisher-listenable-future/index.md)&gt; <br>Structure that adapts a Publisher to a ListenableFuture interface. We accomplish this by immediately subscribing to the publisher with a callback that dispatches a SettableFuture. |

## Functions

| Name | Summary |
|---|---|
| [addListener](add-listener.md) | [jvm]<br>open override fun [addListener](add-listener.md)(listener: [Runnable](https://docs.oracle.com/javase/8/docs/api/java/lang/Runnable.html), executor: [Executor](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html))<br>Registers a listener to be [run](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html#execute-java.lang.Runnable-) on the given executor. The listener will run when the `Future`'s computation is [complete](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html#isDone--) or, if the computation is already complete, immediately. |
| [cancel](cancel.md) | [jvm]<br>open override fun [cancel](cancel.md)(mayInterruptIfRunning: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Attempts to cancel execution of this task.  This attempt will fail if the task has already completed, has already been cancelled, or could not be cancelled for some other reason. If successful, and this task has not started when `cancel` is called, this task should never run.  If the task has already started, then the `mayInterruptIfRunning` parameter determines whether the thread executing this task should be interrupted in an attempt to stop the task. |
| [get](get.md) | [jvm]<br>open override fun [get](get.md)(): [R](index.md)<br>Waits if necessary for the computation to complete, and then retrieves its result.<br>[jvm]<br>open operator override fun [get](get.md)(timeout: [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-long/index.html), unit: [TimeUnit](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeUnit.html)): [R](index.md)<br>Waits if necessary for at most the given time for the computation to complete, and then retrieves its result, if available. |
| [isCancelled](is-cancelled.md) | [jvm]<br>open override fun [isCancelled](is-cancelled.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns `true` if this task was cancelled before it completed normally. This defers to the underlying future, or a wrapped object if using a Publisher. |
| [isDone](is-done.md) | [jvm]<br>open override fun [isDone](is-done.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Returns `true` if this task completed. This defers to the underlying future, or a wrapped object if using a Reactive Java Publisher. |
| [subscribe](subscribe.md) | [jvm]<br>open override fun [subscribe](subscribe.md)(subscriber: Subscriber&lt;in [R](index.md)?&gt;)<br>Request Publisher to start streaming data. |

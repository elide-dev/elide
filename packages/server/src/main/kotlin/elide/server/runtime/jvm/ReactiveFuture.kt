@file:Suppress(
  "ReactiveStreamsPublisherImplementation",
  "ReactiveStreamsSubscriberImplementation",
)

package elide.server.runtime.jvm

import com.google.api.core.ApiFuture
import com.google.api.core.ApiFutureToListenableFuture
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import elide.server.runtime.jvm.ReactiveFuture.Companion.wrap
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function
import javax.annotation.Nonnull
import javax.annotation.concurrent.Immutable
import javax.annotation.concurrent.ThreadSafe

/**
 * Adapts future/async value containers from different frameworks (namely, Reactive Java, Guava, and the JDK).
 *
 * Create a new `ReactiveFuture` by using any of the [wrap] factory methods. The resulting object is usable as a
 * [Publisher], [ListenableFuture], or [ApiFuture] (from GAX). This object simply wraps whatever inner object is
 * provided, and as such instances are lightweight; there is no default functionality after immediate construction in
 * most cases.</p>
 *
 * **Caveat:** when using a [Publisher] as a [ListenableFuture] (i.e. wrapping a {@link Publisher} and then using any of
 * the typical future methods, like [ListenableFuture.addListener], the underlying publisher may not publish more than
 * one value. This is to prevent dropping intermediate values on the floor, silently, before dispatching the future's
 * callbacks, which generally only accept one value. Other than this, things should work "as expected" whether you're
 * looking at them from a Guava, JDK, or Reactive perspective.
 *
 * @see Publisher Reactive Java type adapted by this object.
 * @see ListenableFuture Guava's extension of the JDK's basic {@link Future}, which adds listener support.
 * @see ApiFuture Lightweight Guava-like future meant to avoid dependencies on Java in API libraries.
 * @see wrap To wrap a [Publisher], [ListenableFuture], or [ApiFuture].
 */
@Immutable
@ThreadSafe
public class ReactiveFuture<R> : Publisher<R?>, ListenableFuture<R?>, ApiFuture<R?> {
  /** Inner future, if one is set. Otherwise [Optional.empty].  */
  private val future: Optional<ListenableFuture<R>>

  /** If a `publisher` is present, this object adapts it to a `future`.  */
  private val publisherAdapter: PublisherListenableFuture<R>?

  /** If a `future` is present, this object adapts it to a `publisher`.  */
  private val futureAdapter: ListenableFuturePublisher<R>?

  /** If a `future` is present, this object adapts it to a `publisher`.  */
  private val javaFutureAdapter: CompletableFuturePublisher<R>?

  /**
   * Spawn a reactive/future adapter in a reactive context, from a [Publisher]. Constructing a reactive future in
   * this manner causes the object to operate in a "publisher-backed" mode.
   *
   * @param publisher Publisher to work with.
   */
  private constructor(publisher: Publisher<R>) {
    future = Optional.empty()
    futureAdapter = null
    publisherAdapter = PublisherListenableFuture(publisher)
    javaFutureAdapter = null
  }

  /**
   * Spawn a reactive/future adapter in a future context, from a [ListenableFuture]. Constructing a reactive
   * future in this manner causes the object to operate in a "future-backed" mode.
   *
   * @param op Future to work with.
   * @param executor Executor to use when running callbacks.
   */
  private constructor(op: ListenableFuture<R>, executor: Executor) {
    future = Optional.of(op)
    futureAdapter = ListenableFuturePublisher(op, executor)
    publisherAdapter = null
    javaFutureAdapter = null
  }

  /**
   * Spawn a reactive/future adapter in a future context, from a [CompletableFuture]. Constructing a reactive
   * future in this manner causes the object to operate in a "future-backed" mode.
   *
   * @param op Future to work with.
   * @param executor Executor to use when running callbacks.
   */
  private constructor(op: CompletableFuture<R>, executor: Executor) {
    future = Optional.empty()
    futureAdapter = null
    publisherAdapter = null
    javaFutureAdapter = CompletableFuturePublisher(op, executor)
  }

  /** @return Internal future representation. */
  @Suppress("ReturnCount")
  private fun resolveFuture(): ListenableFuture<R> {
    if (publisherAdapter != null) return publisherAdapter else if (javaFutureAdapter != null) return javaFutureAdapter
    return future.get()
  }

  /** @return Internal publisher representation. */
  @Suppress("ReturnCount")
  private fun resolvePublisher(): Publisher<R> {
    if (futureAdapter != null) return futureAdapter else if (javaFutureAdapter != null) return javaFutureAdapter
    return Objects.requireNonNull(publisherAdapter)!!
  }

  // -- Compliance: Publisher -- //

  /**
   * Request [Publisher] to start streaming data.
   *
   *
   * This is a "factory method" and can be called multiple times, each time starting a new [Subscription]. Each
   * [Subscription] will work for only a single [Subscriber]. A [Subscriber] should only subscribe
   * once to a single [Publisher]. If the [Publisher] rejects the subscription attempt or otherwise fails it
   * will signal the error via [Subscriber.onError].
   *
   * @param subscriber the [Subscriber] that will consume signals from this [Publisher].
   */
  override fun subscribe(subscriber: Subscriber<in R?>) {
    resolvePublisher().subscribe(subscriber)
  }

  // -- Compliance: Listenable Future -- //

  /**
   * Registers a listener to be [run][Executor.execute] on the given executor. The listener will run
   * when the `Future`'s computation is [complete][Future.isDone] or, if the computation is already
   * complete, immediately.
   *
   *
   * There is no guaranteed ordering of execution of listeners, but any listener added through this method is
   * guaranteed to be called once the computation is complete.
   *
   *
   * Exceptions thrown by a listener will be propagated up to the executor. Any exception thrown during
   * `Executor.execute` (e.g., a `RejectedExecutionException` or an exception thrown by
   * [direct execution][MoreExecutors.directExecutor]) will be caught and logged.
   *
   *
   * Note: For fast, lightweight listeners that would be safe to execute in any thread, consider
   * [MoreExecutors.directExecutor]. Otherwise, avoid it. Heavyweight `directExecutor` listeners can cause
   * problems, and these problems can be difficult to reproduce because they depend on timing. For example:
   *
   *  * The listener may be executed by the caller of `addListener`. That caller may be a
   * UI thread or other latency-sensitive thread. This can harm UI responsiveness.
   *  * The listener may be executed by the thread that completes this `Future`. That
   * thread may be an internal system thread such as an RPC network thread. Blocking that
   * thread may stall progress of the whole system. It may even cause a deadlock.
   *  * The listener may delay other listeners, even listeners that are not themselves `directExecutor` listeners.
   *
   *
   *
   * This is the most general listener interface. For common operations performed using listeners, see
   * [Futures]. For a simplified but general listener interface, see
   * [addCallback()][Futures.addCallback].
   *
   *
   * Memory consistency effects: Actions in a thread prior to adding a listener
   * [*happen-before*](https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4.5) its execution begins,
   * perhaps in another thread.
   *
   *
   * Guava implementations of `ListenableFuture` promptly release references to listeners after executing
   * them.
   *
   * @param listener the listener to run when the computation is complete.
   * @param executor the executor to run the listener in
   * @throws RejectedExecutionException if we tried to execute the listener immediately but the executor rejected it.
   */
  @Throws(RejectedExecutionException::class)
  override fun addListener(listener: Runnable, executor: Executor) {
    resolveFuture().addListener(listener, executor)
  }

  /**
   * Attempts to cancel execution of this task.  This attempt will fail if the task has already completed, has already
   * been cancelled, or could not be cancelled for some other reason. If successful, and this task has not started when
   * `cancel` is called, this task should never run.  If the task has already started, then the
   * `mayInterruptIfRunning` parameter determines whether the thread executing this task should be interrupted in
   * an attempt to stop the task.
   *
   *
   * After this method returns, subsequent calls to [.isDone] will always return `true`.  Subsequent
   * calls to [.isCancelled] will always return `true` if this method returned `true`.
   *
   * @param mayInterruptIfRunning `true` if the thread executing this task should be interrupted; otherwise,
   * in-progress tasks are allowed to complete
   * @return `false` if the task could not be cancelled, typically because it has already completed normally;
   * `true` otherwise.
   */
  override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
    return resolveFuture().cancel(mayInterruptIfRunning)
  }

  /**
   * Returns `true` if this task was cancelled before it completed normally. This defers to the underlying future,
   * or a wrapped object if using a [Publisher].
   *
   * @return `true` if this task was cancelled before it completed
   */
  override fun isCancelled(): Boolean {
    return resolveFuture().isCancelled
  }

  /**
   * Returns `true` if this task completed. This defers to the underlying future, or a wrapped object if using a
   * Reactive Java [Publisher].
   *
   * Completion may be due to normal termination, an exception, or cancellation -- in all of these cases, this method
   * will return `true`.
   *
   * @return `true` if this task completed.
   */
  override fun isDone(): Boolean {
    return resolveFuture().isDone
  }

  /**
   * Waits if necessary for the computation to complete, and then retrieves its result.
   *
   *
   * It is generally recommended to use the variant of this method which specifies a timeout - one must handle the
   * additional [TimeoutException], but on the other hand the computation can never infinitely block if an async
   * value does not materialize.
   *
   * @see .get
   * @return the computed result.
   * @throws CancellationException if the computation was cancelled
   * @throws ExecutionException    if the computation threw an exception
   * @throws InterruptedException  if the current thread was interrupted while waiting
   */
  @Throws(InterruptedException::class, ExecutionException::class)
  override fun get(): R {
    return resolveFuture().get()
  }

  /**
   * Waits if necessary for at most the given time for the computation to complete, and then retrieves its result, if
   * available.
   *
   * @param timeout the maximum time to wait
   * @param unit    the time unit of the timeout argument
   * @return the computed result
   * @throws CancellationException if the computation was cancelled
   * @throws ExecutionException    if the computation threw an exception
   * @throws InterruptedException  if the current thread was interrupted while waiting
   * @throws TimeoutException      if the wait timed out
   */
  @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
  override fun get(timeout: Long, unit: TimeUnit): R {
    return resolveFuture()[timeout, unit]
  }

  /**
   * Structure that adapts a [Publisher] to a [ListenableFuture] interface. We accomplish this by
   * immediately subscribing to the publisher with a callback that dispatches a [SettableFuture].
   *
   *
   * This object is used in the specific circumstance of wrapping a [Publisher], and then using the wrapped
   * object as a [ListenableFuture] (or any descendent or compliant implementation thereof).
   *
   * @param <T> Generic type returned by the future.
  </T> */
  @Immutable
  @ThreadSafe
  public class PublisherListenableFuture<T> internal constructor(publisher: Publisher<T>) :
    ListenableFuture<T>, Publisher<T> {
    /** Whether we have received a value.  */
    private val received = AtomicBoolean(false)

    /** Whether we have completed acquiring a value.  */
    private val completed = AtomicBoolean(false)

    /** Whether we have been cancelled.  */
    private val cancelled = AtomicBoolean(false)

    /** Describes the list of proxied subscribers.  */
    private val subscribers: MutableMap<String, Subscriber<in T>> = ConcurrentHashMap()

    /** Converted/pass-through future value.  */
    private val future: SettableFuture<T> = SettableFuture.create()

    /** Subscription, so we can propagate cancellation.  */
    @Volatile private var subscription: Subscription? = null

    /**
     * Private constructor.
     */
    init {
      publisher.subscribe(object : Subscriber<T> {
        override fun onSubscribe(s: Subscription) {
          subscription = s
        }

        override fun onNext(t: T) {
          if (received.compareAndSet(false, true)) {
            proxyExecute { sub: Subscriber<in T> ->
              sub.onNext(
                t
              )
            }
            future.set(t)
            return
          }
          onError(
            IllegalStateException(
              "Cannot publish multiple items through `ReactiveFuture`."
            )
          )
        }

        override fun onError(t: Throwable) {
          if (!completed.get()) {
            proxyExecute { sub: Subscriber<in T> ->
              sub.onError(
                t
              )
            }
            future.setException(t)
          }
        }

        override fun onComplete() {
          if (completed.compareAndSet(false, true)) {
            proxyExecute { obj: Subscriber<in T> -> obj.onComplete() }
            this@PublisherListenableFuture.clear()
          }
        }
      })
    }

    /**
     * Call something on each proxied publisher subscription, if any.
     *
     * @param operation Operation to execute. Called for each subscriber.
     */
    private fun proxyExecute(operation: Consumer<Subscriber<in T>>) {
      if (subscribers.isNotEmpty()) {
        subscribers.values.forEach(operation)
      }
    }

    /**
     * Remove all subscribers and clear references to futures/publishers/listeners.
     */
    private fun clear() {
      subscribers.clear()
      subscription = null
    }

    /**
     * Drop a subscription (after proxied [Subscription.cancel] is called).
     *
     * @param id ID of the subscription to drop.
     */
    private fun dropSubscription(id: String) {
      subscribers[id]!!.onComplete()
      subscribers.remove(id)
    }

    // -- Interface Compliance: Publisher -- //
    override fun subscribe(s: Subscriber<in T>) {
      val id = subscribers.size.toString()
      val sub: Subscription = object : Subscription {
        override fun request(n: Long) {
          subscription!!.request(n)
        }

        override fun cancel() {
          // kill self
          dropSubscription(id)
        }
      }
      subscribers[id] = s
      s.onSubscribe(sub)
    }

    // -- Interface Compliance: Listenable Future -- //
    override fun addListener(runnable: Runnable, executor: Executor) {
      future.addListener(runnable, executor)
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
      var cancelled = false
      if (!completed.get() && this.cancelled.compareAndSet(false, true)) {
        proxyExecute { obj: Subscriber<in T> -> obj.onComplete() } // dispatch `onComplete` for any subscribers
        subscription!!.cancel() // cancel upwards
        cancelled = future.cancel(mayInterruptIfRunning) // cancel future
        this.clear() // clear references
      }
      return cancelled
    }

    override fun isCancelled(): Boolean {
      return cancelled.get()
    }

    override fun isDone(): Boolean {
      return completed.get() || cancelled.get()
    }

    @Throws(InterruptedException::class, ExecutionException::class)
    override fun get(): T {
      return future.get()
    }

    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    override fun get(timeout: Long, unit: TimeUnit): T {
      return future[timeout, unit]
    }
  }

  /**
   * Structure that adapts Java's [CompletableFuture] to a Reactive Java [Publisher], which publishes one
   * item - either the result of the computation, or an error.
   *
   *
   * This object is used in the specific circumstance that a [CompletableFuture] is wrapped by a
   * [ReactiveFuture], and then used within the Reactive Java or Guava ecosystems as a [Publisher] or a
   * [ListenableFuture] (or [ApiFuture]), or a descendent thereof. As in [ListenableFuturePublisher],
   * we simply set the callback for the future value, upon item-request (one cycle is allowed), and propagate any events
   * received to the publisher.
   *
   * @param <T> Emit type for this adapter. Matches the future it wraps.
  </T> */
  @Suppress("TooManyFunctions")
  public class CompletableFuturePublisher<T> internal constructor(
    @field:Nonnull
    @param:Nonnull
    private val future: CompletableFuture<T>,
    callbackExecutor: Executor
  ) : Publisher<T>, ListenableFuture<T>, CompletionStage<T> {
    private val stage: CompletionStage<T>
    private val callbackExecutor: Executor

    /**
     * Construct an adapter that propagates signals from a `CompletableFuture` to a `Publisher`.
     */
    init {
      stage = future
      this.callbackExecutor = callbackExecutor
    }
    /* == `Future`/`ListenableFuture` Interface Compliance == */
    /** @inheritDoc
     */
    override fun subscribe(subscriber: Subscriber<in T>) {
      Objects.requireNonNull(subscriber, "Subscriber cannot be null")
      subscriber.onSubscribe(CompletableFutureSubscription(future, subscriber, callbackExecutor))
    }

    /** @inheritDoc
     */
    override fun addListener(runnable: Runnable, executor: Executor) {
      future.thenRunAsync(runnable, executor)
    }

    /** @inheritDoc
     */
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
      return future.cancel(mayInterruptIfRunning)
    }

    /** @inheritDoc
     */
    override fun isCancelled(): Boolean {
      return future.isCancelled
    }

    /** @inheritDoc
     */
    override fun isDone(): Boolean {
      return future.isDone
    }

    /** @inheritDoc
     */
    @Throws(InterruptedException::class, ExecutionException::class)
    override fun get(): T {
      return future.get()
    }

    /** @inheritDoc
     */
    @Throws(InterruptedException::class, ExecutionException::class, TimeoutException::class)
    override fun get(timeout: Long, unit: TimeUnit): T {
      return future[timeout, unit]
    }
    /* == `CompletionStage` Interface Compliance == */
    /** @inheritDoc
     */
    override fun <U> thenApply(fn: Function<in T, out U>): CompletionStage<U> {
      return stage.thenApply(fn)
    }

    /** @inheritDoc
     */
    override fun <U> thenApplyAsync(fn: Function<in T, out U>): CompletionStage<U> {
      return stage.thenApplyAsync(fn)
    }

    /** @inheritDoc
     */
    override fun <U> thenApplyAsync(fn: Function<in T, out U>, executor: Executor): CompletionStage<U> {
      return stage.thenApplyAsync(fn, executor)
    }

    /** @inheritDoc
     */
    override fun thenAccept(action: Consumer<in T>): CompletionStage<Void> {
      return stage.thenAccept(action)
    }

    /** @inheritDoc
     */
    override fun thenAcceptAsync(action: Consumer<in T>): CompletionStage<Void> {
      return stage.thenAcceptAsync(action)
    }

    /** @inheritDoc
     */
    override fun thenAcceptAsync(action: Consumer<in T>, executor: Executor): CompletionStage<Void> {
      return stage.thenAcceptAsync(action, executor)
    }

    /** @inheritDoc
     */
    override fun thenRun(action: Runnable): CompletionStage<Void> {
      return stage.thenRun(action)
    }

    /** @inheritDoc
     */
    override fun thenRunAsync(action: Runnable): CompletionStage<Void> {
      return stage.thenRunAsync(action)
    }

    /** @inheritDoc
     */
    override fun thenRunAsync(action: Runnable, executor: Executor): CompletionStage<Void> {
      return stage.thenRunAsync(action, executor)
    }

    /** @inheritDoc
     */
    override fun <U, V> thenCombine(
      other: CompletionStage<out U?>,
      fn: BiFunction<in T, in U?, out V>
    ): CompletionStage<V> {
      return stage.thenCombine(other, fn)
    }

    /** @inheritDoc
     */
    override fun <U, V> thenCombineAsync(
      other: CompletionStage<out U?>,
      fn: BiFunction<in T, in U?, out V>
    ): CompletionStage<V> {
      return stage.thenCombineAsync(other, fn)
    }

    /** @inheritDoc
     */
    override fun <U, V> thenCombineAsync(
      other: CompletionStage<out U?>,
      fn: BiFunction<in T, in U?, out V>,
      executor: Executor
    ): CompletionStage<V> {
      return stage.thenCombineAsync(other, fn, executor)
    }

    /** @inheritDoc
     */
    override fun <U> thenAcceptBoth(
      other: CompletionStage<out U?>,
      action: BiConsumer<in T, in U?>
    ): CompletionStage<Void> {
      return stage.thenAcceptBoth(other, action)
    }

    /** @inheritDoc
     */
    override fun <U> thenAcceptBothAsync(
      other: CompletionStage<out U?>,
      action: BiConsumer<in T, in U?>
    ): CompletionStage<Void> {
      return stage.thenAcceptBothAsync(other, action)
    }

    /** @inheritDoc
     */
    override fun <U> thenAcceptBothAsync(
      other: CompletionStage<out U?>,
      action: BiConsumer<in T, in U?>,
      executor: Executor
    ): CompletionStage<Void> {
      return stage.thenAcceptBothAsync(other, action, executor)
    }

    /** @inheritDoc
     */
    override fun runAfterBoth(other: CompletionStage<*>?, action: Runnable): CompletionStage<Void> {
      return stage.runAfterBoth(other, action)
    }

    /** @inheritDoc
     */
    override fun runAfterBothAsync(other: CompletionStage<*>?, action: Runnable): CompletionStage<Void> {
      return stage.runAfterBothAsync(other, action)
    }

    /** @inheritDoc
     */
    override fun runAfterBothAsync(
      other: CompletionStage<*>?,
      action: Runnable,
      executor: Executor
    ): CompletionStage<Void> {
      return stage.runAfterBothAsync(other, action, executor)
    }

    /** @inheritDoc
     */
    override fun <U> applyToEither(other: CompletionStage<out T>, fn: Function<in T, U>): CompletionStage<U> {
      return stage.applyToEither(other, fn)
    }

    /** @inheritDoc
     */
    override fun <U> applyToEitherAsync(other: CompletionStage<out T>, fn: Function<in T, U>): CompletionStage<U> {
      return stage.applyToEitherAsync(other, fn)
    }

    /** @inheritDoc
     */
    override fun <U> applyToEitherAsync(
      other: CompletionStage<out T>,
      fn: Function<in T, U>,
      executor: Executor
    ): CompletionStage<U> {
      return stage.applyToEitherAsync(other, fn, executor)
    }

    /** @inheritDoc
     */
    override fun acceptEither(
      other: CompletionStage<out T>,
      action: Consumer<in T>
    ): CompletionStage<Void> {
      return stage.acceptEither(other, action)
    }

    /** @inheritDoc
     */
    override fun acceptEitherAsync(
      other: CompletionStage<out T>,
      action: Consumer<in T>
    ): CompletionStage<Void> {
      return stage.acceptEitherAsync(other, action)
    }

    /** @inheritDoc
     */
    override fun acceptEitherAsync(
      other: CompletionStage<out T>,
      action: Consumer<in T>,
      executor: Executor
    ): CompletionStage<Void> {
      return stage.acceptEitherAsync(other, action, executor)
    }

    /** @inheritDoc
     */
    override fun runAfterEither(other: CompletionStage<*>?, action: Runnable): CompletionStage<Void> {
      return stage.runAfterEither(other, action)
    }

    /** @inheritDoc
     */
    override fun runAfterEitherAsync(other: CompletionStage<*>?, action: Runnable): CompletionStage<Void> {
      return stage.runAfterEitherAsync(other, action)
    }

    /** @inheritDoc
     */
    override fun runAfterEitherAsync(
      other: CompletionStage<*>?,
      action: Runnable,
      executor: Executor
    ): CompletionStage<Void> {
      return stage.runAfterEitherAsync(other, action, executor)
    }

    /** @inheritDoc
     */
    override fun <U> thenCompose(fn: Function<in T, out CompletionStage<U>>): CompletionStage<U> {
      return stage.thenCompose(fn)
    }

    /** @inheritDoc
     */
    override fun <U> thenComposeAsync(fn: Function<in T, out CompletionStage<U>>): CompletionStage<U> {
      return stage.thenComposeAsync(fn)
    }

    /** @inheritDoc
     */
    override fun <U> thenComposeAsync(
      fn: Function<in T, out CompletionStage<U>>,
      executor: Executor
    ): CompletionStage<U> {
      return stage.thenComposeAsync(fn, executor)
    }

    /** @inheritDoc
     */
    override fun <U> handle(fn: BiFunction<in T, Throwable, out U>): CompletionStage<U> {
      return stage.handle(fn)
    }

    /** @inheritDoc
     */
    override fun <U> handleAsync(fn: BiFunction<in T, Throwable, out U>): CompletionStage<U> {
      return stage.handleAsync(fn)
    }

    /** @inheritDoc
     */
    override fun <U> handleAsync(
      fn: BiFunction<in T, Throwable, out U>,
      executor: Executor
    ): CompletionStage<U> {
      return stage.handleAsync(fn, executor)
    }

    /** @inheritDoc
     */
    override fun whenComplete(action: BiConsumer<in T, in Throwable>): CompletionStage<T> {
      return stage.whenComplete(action)
    }

    /** @inheritDoc
     */
    override fun whenCompleteAsync(action: BiConsumer<in T, in Throwable>): CompletionStage<T> {
      return stage.whenCompleteAsync(action)
    }

    /** @inheritDoc
     */
    override fun whenCompleteAsync(
      action: BiConsumer<in T, in Throwable>,
      executor: Executor
    ): CompletionStage<T> {
      return stage.whenCompleteAsync(action, executor)
    }

    /** @inheritDoc
     */
    override fun exceptionally(fn: Function<Throwable, out T>): CompletionStage<T> {
      return stage.exceptionally(fn)
    }

    /** @inheritDoc
     */
    override fun toCompletableFuture(): CompletableFuture<T> {
      return stage.toCompletableFuture()
    }

    /**
     * Models a Reactive Java [Subscription], which is responsible for propagating events from a
     * Concurrent Java [CompletableFuture] to a [Subscriber].
     *
     *
     * This object is generally used internally by the [CompletableFuturePublisher], once a [Subscriber]
     * attaches itself to a [Publisher] that is actually a wrapped [CompletableFuture]. Error (exception)
     * events and value events are both propagated. Subscribers based on this wrapping will only ever receive a maximum
     * of **one value** or **one error**.
     */
    @Immutable
    @ThreadSafe
    public inner class CompletableFutureSubscription internal constructor(
      future: CompletableFuture<T>,
      subscriber: Subscriber<in T>?,
      executor: Executor
    ) : Subscription {
      private val completed = AtomicBoolean(false)
      private val subscriber: Subscriber<in T>
      private val future: CompletableFuture<T>
      private val executor: Executor

      /**
       * Private constructor, meant for use by `CompletableFuturePublisher` only.
       */
      init {
        this.future = Objects.requireNonNull(future)
        this.subscriber = Objects.requireNonNull(subscriber)!!
        this.executor = Objects.requireNonNull(executor)
      }

      /**
       * Request the specified number of items from the underlying [Subscription]. This must **always be
       * <pre>1</pre>**.
       *
       * @param n Number of elements to request to the upstream (must always be <pre>1</pre>).
       * @throws IllegalArgumentException If any value other than <pre>1</pre> is passed in.
       */
      @Synchronized
      @Suppress("TooGenericExceptionCaught")
      override fun request(n: Long) {
        if (n == 1L && !completed.get()) {
          try {
            val future = this.future
            future.thenAcceptAsync({
              var value: T? = null
              var err: Throwable? = null
              try {
                value = future.get()
              } catch (exc: Exception) {
                err = exc
              }
              if (completed.compareAndSet(false, true)) {
                if (err != null) {
                  subscriber.onError(err)
                } else {
                  if (value != null) {
                    subscriber.onNext(value)
                  }
                  subscriber.onComplete()
                }
              }
            }, executor)
          } catch (e: Exception) {
            subscriber.onError(e)
          }
        } else if (n != 1L) {
          val ex = IllegalArgumentException(
            "Cannot request more or less than 1 item from a ReactiveFuture-wrapped publisher."
          )
          subscriber.onError(ex)
        }
      }

      /**
       * Request the publisher to stop sending data and clean up resources.
       */
      @Synchronized
      override fun cancel() {
        if (completed.compareAndSet(false, true)) {
          subscriber.onComplete()
          future.cancel(false)
        }
      }
    }
  }

  /**
   * Structure that adapts Guava's [ListenableFuture] to a Reactive Java [Publisher], which publishes one
   * item - either the result of the computation, or an error.
   *
   *
   * This object is used in the specific circumstance that a [ListenableFuture] is wrapped by a
   * [ReactiveFuture], and then used within the Reactive Java ecosystem as a [Publisher]. We simply set a
   * callback for the future value, upon item-request (one cycle is allowed), and propagate any events received to the
   * publisher.
   *
   * @param <T> Emit type for this adapter. Matches the publisher it wraps.
  </T> */
  public class ListenableFuturePublisher<T>
  /**
   * Wrap a [ListenableFuture]. Private constructor for use by [ReactiveFuture] only.
   *
   * @param future The future to convert or wait on.
   * @param callbackExecutor Executor to run the callback on.
   */ internal constructor(
    @field:Nonnull
    @param:Nonnull
    private val future: ListenableFuture<T>,

    @field:Nonnull
    @param:Nonnull
    private val callbackExecutor: Executor
  ) : Publisher<T> {
    override fun subscribe(subscriber: Subscriber<in T>) {
      Objects.requireNonNull(subscriber, "Subscriber cannot be null")
      subscriber.onSubscribe(
        ListenableFutureSubscription(
          future,
          subscriber,
          callbackExecutor,
        )
      )
    }

    /**
     * Models a Reactive Java [Subscription], which is responsible for propagating events from a
     * [ListenableFuture] to a [Subscriber].
     *
     *
     * This object is generally used internally by the [ListenableFuturePublisher], once a [Subscriber]
     * attaches itself to a [Publisher] that is actually a wrapped [ListenableFuture]. Error (exception)
     * events and value events are both propagated. Subscribers based on this wrapping will only ever receive a maximum
     * of **one value** or **one error**.
     */
    @Immutable
    @ThreadSafe
    public inner class ListenableFutureSubscription internal constructor(
      future: ListenableFuture<T>,
      subscriber: Subscriber<in T>?,
      executor: Executor
    ) : Subscription {
      private val completed = AtomicBoolean(false)
      private val subscriber: Subscriber<in T>

      // to allow cancellation
      private val future: ListenableFuture<T>

      // executor to use when dispatching the callback
      private val executor: Executor

      /**
       * Private constructor, meant for use by `ListenableFuturePublisher` only.
       */
      init {
        this.future = Objects.requireNonNull(future)
        this.subscriber = Objects.requireNonNull(subscriber)!!
        this.executor = Objects.requireNonNull(executor)
      }

      /**
       * Request the specified number of items from the underlying [Subscription]. This must **always be
       * <pre>1</pre>**.
       *
       * @param n Number of elements to request to the upstream (must always be <pre>1</pre>).
       * @throws IllegalArgumentException If any value other than <pre>1</pre> is passed in.
       */
      @Synchronized
      @Suppress("TooGenericExceptionCaught")
      override fun request(n: Long) {
        if (n == 1L && !completed.get()) {
          try {
            val future = this.future
            future.addListener({
              var value: T? = null
              var err: Throwable? = null
              try {
                value = this.future.get()
              } catch (exc: Exception) {
                err = exc
              }
              if (completed.compareAndSet(false, true)) {
                if (err != null) {
                  subscriber.onError(err)
                } else {
                  if (value != null) {
                    subscriber.onNext(value)
                  }
                  subscriber.onComplete()
                }
              }
            }, executor)
          } catch (e: Exception) {
            subscriber.onError(e)
          }
        } else if (n != 1L) {
          val ex = IllegalArgumentException(
            "Cannot request more or less than 1 item from a ReactiveFuture-wrapped publisher."
          )
          subscriber.onError(ex)
        }
      }

      /**
       * Request the publisher to stop sending data and clean up resources.
       */
      @Synchronized
      override fun cancel() {
        if (completed.compareAndSet(false, true)) {
          subscriber.onComplete()
          future.cancel(false)
        }
      }
    }
  }

  public companion object {
    // -- Public API -- //
    /**
     * Wrap a Reactive Java [Publisher] in a universal [ReactiveFuture], such that it may be used with any
     * interface requiring a supported async or future value.
     *
     * The resulting object is usable as any of [ListenableFuture], [Publisher], or [ApiFuture]. See
     * class docs for more information.
     *
     * **Note:** to use a [Publisher] as a [Future] (or any descendent thereof), the [Publisher]
     * may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class
     * and accessed as a [Future], to prevent silently dropping intermediate values on the floor.
     *
     * @param publisher Reactive publisher to wrap.
     * @param R Return or emission type of the publisher.
     * @return Wrapped reactive future object.
     * @throws IllegalArgumentException If the passed `publisher` is `null`.
     */
    public fun <R> wrap(publisher: Publisher<R>?): ReactiveFuture<R> {
      requireNotNull(publisher) { "Cannot wrap `null` publisher." }
      return ReactiveFuture(publisher)
    }

    /**
     * Wrap a regular Java [CompletableFuture] in a universal [ReactiveFuture], such that it may be used with
     * any interface requiring support for that class.
     *
     * The resulting object is usable as any of [ListenableFuture], [Publisher], or [ApiFuture]. See
     * class docs for more information.
     *
     * **Note:** to use a [Publisher] as a [Future] (or any descendent thereof), the [Publisher]
     * may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class
     * and accessed as a [Future], to prevent silently dropping intermediate values on the floor.
     *
     * **Warning:** this method uses [MoreExecutors.directExecutor] for callback execution. You should only
     * do this if the callbacks associated with your future are lightweight and exit quickly. Otherwise, it is heavily
     * recommended to use the variants of `wrap` that accept an [Executor]. For instance, the corresponding
     * method to this one is [.wrap].
     *
     * @param future Completable future to wrap.
     * @param R Return or emission type of the future.
     * @return Wrapped reactive future object.
     */
    public fun <R> wrap(future: CompletableFuture<R>?): ReactiveFuture<R> {
      requireNotNull(future) { "Cannot wrap `null` publisher." }
      return wrap(future, MoreExecutors.directExecutor())
    }

    /**
     * Wrap a regular Java [CompletableFuture] in a universal [ReactiveFuture], such that it may be used with
     * any interface requiring support for that class.
     *
     * The resulting object is usable as any of [ListenableFuture], [Publisher], or [ApiFuture]. See
     * class docs for more information.
     *
     * **Note:** to use a [Publisher] as a [Future] (or any descendent thereof), the [Publisher]
     * may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class
     * and accessed as a [Future], to prevent silently dropping intermediate values on the floor.
     *
     * @param future Completable future to wrap.
     * @param executor Executor to use.
     * @param R Return or emission type of the future.
     * @return Wrapped reactive future object.
     */
    public fun <R> wrap(future: CompletableFuture<R>?, executor: Executor?): ReactiveFuture<R> {
      requireNotNull(future) { "Cannot wrap `null` future." }
      requireNotNull(executor) { "Cannot wrap future with `null` executor." }
      return ReactiveFuture(future, executor)
    }

    /**
     * Wrap a Guava [ListenableFuture] in a universal [ReactiveFuture], such that it may be used with any
     * interface requiring a supported async or future value.
     *
     * **Warning:** this method uses [MoreExecutors.directExecutor] for callback execution. You should only
     * do this if the callbacks associated with your future are lightweight and exit quickly. Otherwise, it is heavily
     * recommended to use the variants of `wrap` that accept an [Executor]. For instance, the corresponding
     * method to this one is [.wrap].
     *
     * The resulting object is usable as any of [ListenableFuture], [Publisher], or [ApiFuture]. See
     * class docs for more information.
     *
     * **Note:** to use a [Publisher] as a [Future] (or any descendent thereof), the [Publisher]
     * may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class
     * and accessed as a [Future], to prevent silently dropping intermediate values on the floor.
     *
     * @param future Future value to wrap.
     * @param R Return value type for the future.
     * @return Wrapped reactive future object.
     * @throws IllegalArgumentException If the passed `future` is `null`.
     */
    public fun <R> wrap(future: ListenableFuture<R>?): ReactiveFuture<R> {
      return wrap(future, MoreExecutors.directExecutor())
    }

    /**
     * Wrap a Guava [ListenableFuture] in a universal [ReactiveFuture], such that it may be used with any
     * interface requiring a supported async or future value.
     *
     * The resulting object is usable as any of [ListenableFuture], [Publisher], or [ApiFuture]. See
     * class docs for more information.
     *
     * **Note:** to use a [Publisher] as a [Future] (or any descendent thereof), the [Publisher]
     * may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class
     * and accessed as a [Future], to prevent silently dropping intermediate values on the floor.
     *
     * @param future Future value to wrap.
     * @param executor Executor to dispatch callbacks with.
     * @param R Return value type for the future.
     * @return Wrapped reactive future object.
     * @throws IllegalArgumentException If the passed `future` is `null`.
     */
    public fun <R> wrap(future: ListenableFuture<R>?, executor: Executor?): ReactiveFuture<R> {
      requireNotNull(future) { "Cannot wrap `null` future." }
      requireNotNull(executor) { "Cannot wrap future with `null` executor." }
      return ReactiveFuture(future, executor)
    }

    /**
     * Wrap a Google APIs [ApiFuture] in a universal [ReactiveFuture], such that it may be used with any
     * interface requiring a supported async or future value.
     *
     * The resulting object is usable as any of [ListenableFuture], [Publisher], or [ApiFuture]. See
     * class docs for more information.
     *
     * **Note:** to use a [Publisher] as a [Future] (or any descendent thereof), the [Publisher]
     * may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class
     * and accessed as a [Future], to prevent silently dropping intermediate values on the floor.
     *
     * @param apiFuture API future to wrap.
     * @param executor Executor to run callbacks with.
     * @param R Return value type for the future.
     * @return Wrapped reactive future object.
     * @throws IllegalArgumentException If the passed `apiFuture` is `null`.
     */
    public fun <R> wrap(apiFuture: ApiFuture<R>?, executor: Executor?): ReactiveFuture<R> {
      requireNotNull(apiFuture) { "Cannot wrap `null` API future." }
      return wrap(ApiFutureToListenableFuture(apiFuture), executor)
    }

    /**
     * Wrap a Google APIs [ApiFuture] in a universal [ReactiveFuture], such that it may be used with any
     * interface requiring a supported async or future value.
     *
     * **Warning:** this method uses [MoreExecutors.directExecutor] for callback execution. You should only
     * do this if the callbacks associated with your future are lightweight and exit quickly. Otherwise, it is heavily
     * recommended to use the variants of `wrap` that accept an [Executor]. For instance, the corresponding
     * method to this one is [wrap].
     *
     * The resulting object is usable as any of [ListenableFuture], [Publisher], or [ApiFuture]. See
     * class docs for more information.
     *
     * **Note:** to use a [Publisher] as a [Future] (or any descendent thereof), the [Publisher]
     * may only emit one value, and no more. Emitting multiple items is considered an error when wrapped in this class
     * and accessed as a [Future], to prevent silently dropping intermediate values on the floor.
     *
     * @param apiFuture API future to wrap.
     * @param R Return value type for the future.
     * @return Wrapped reactive future object.
     * @throws IllegalArgumentException If the passed `apiFuture` is `null`.
     */
    public fun <R> wrap(apiFuture: ApiFuture<R>?): ReactiveFuture<R> {
      return wrap(apiFuture, MoreExecutors.directExecutor())
    }

    /**
     * Create an already-resolved future, wrapping the provided value. The future will present as done as soon as it is
     * returned from this method.
     *
     * Under the hood, this is simply a [ReactiveFuture] wrapping a call to
     * [Futures.immediateFuture].
     *
     * @param value Value to wrap in an already-completed future.
     * @param R Return value generic type.
     * @return Reactive future wrapping a finished value.
     */
    public fun <R> done(value: R): ReactiveFuture<R> {
      return wrap(Futures.immediateFuture(value))
    }

    /**
     * Create an already-failed future, wrapping the provided exception instance. The future will present as one as soon
     * as it is returned from this method.
     *
     *
     * Calling [Future.get] or [Future.get] on a failed future will surface the
     * associated exception where invocation occurs. Under the hood, this is simply a [ReactiveFuture] wrapping a
     * call to [Futures.immediateFailedFuture].
     *
     * @param error Error to wrap in an already-failed future.
     * @param R Return value generic type.
     * @return Reactive future wrapping a finished value.
     */
    public fun <R> failed(error: Throwable): ReactiveFuture<R> {
      return wrap(Futures.immediateFailedFuture(error))
    }

    /**
     * Create an already-cancelled future. The future will present as both done and cancelled as soon as it is returned
     * from this method.
     *
     * Under the hood, this is simply a [ReactiveFuture] wrapping a call to [Futures.immediateCancelledFuture].
     *
     * @param R Return value generic type.
     * @return Reactive future wrapping a cancelled operation.
     */
    public fun <R> cancelled(): ReactiveFuture<R> {
      return wrap(Futures.immediateCancelledFuture())
    }
  }
}

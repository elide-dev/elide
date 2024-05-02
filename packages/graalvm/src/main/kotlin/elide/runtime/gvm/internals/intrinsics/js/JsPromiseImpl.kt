package elide.runtime.gvm.internals.intrinsics.js

import org.graalvm.polyglot.Value
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import elide.runtime.intrinsics.js.JsPromise
import elide.vm.annotations.Polyglot

/**
 * TBD.
 */
internal class JsPromiseImpl<T> private constructor (
  private val ready: AtomicBoolean = AtomicBoolean(false),
  private val latch: CountDownLatch? = null,
  private val producer: () -> T,
  private val value: AtomicReference<T> = AtomicReference(),
  private val err: AtomicReference<Throwable> = AtomicReference(),
) : JsPromise<T> {
  // Whether the promise has started executing.
  private val started: AtomicBoolean = AtomicBoolean(false)

  // Whether the promise has executed.
  private val executed: AtomicBoolean = AtomicBoolean(false)

  // Whether the promise failed.
  private val didThrow: AtomicBoolean = AtomicBoolean(false)

  // Functions to call when the promise is resolved.
  private val nextFns: MutableList<(T) -> Unit> = mutableListOf()

  // Functions to call if the promise fails.
  private val catchFns: MutableList<(Throwable) -> Unit> = mutableListOf()

  private fun invokeAwait() {
    // short circuit: if already resolved, invoke next and continue
    if (ready.get()) {
      invokeThen()
      return
    }

    // run the fn
    if (started.compareAndSet(false, true)) {
      try {
        value.set(producer.invoke())
      } catch (e: Throwable) {
        didThrow.set(true)
        err.set(e)
      } finally {
        executed.set(true)
        ready.set(true)
        latch?.countDown()
      }
    }

    // invoke followups
    invokeThen()
  }

  private fun invokeThen() {
    if (didThrow.get()) {
      catchFns.forEach { it.invoke(err.get()) }
    } else {
      nextFns.forEach { it.invoke(value.get()) }
    }
  }

  companion object {
    @JvmStatic fun <T> of(producer: Supplier<T>): JsPromise<T> = JsPromiseImpl(producer = { producer.get() })
    @JvmStatic fun <T> of(promise: Future<T>): JsPromise<T> = JsPromiseImpl(producer = { promise.get() })

    @JvmStatic fun <T> of(latch: CountDownLatch, producer: Supplier<T>): JsPromise<T> =
      JsPromiseImpl(latch = latch, producer = { producer.get() })

    @JvmStatic inline fun <T> of(crossinline callable: () -> T): JsPromise<T> =
      JsPromiseImpl(producer = { callable.invoke() })

    @JvmStatic fun <T> resolved(value: T): JsPromise<T> = JsPromiseImpl(
      ready = AtomicBoolean(true),
      value = AtomicReference(value),
      producer = { value },
    )

    @JvmStatic fun <T> rejected(err: Throwable): JsPromise<T> = JsPromiseImpl(
      ready = AtomicBoolean(true),
      err = AtomicReference(err),
      producer = { throw err },
    )
  }

  override fun then(onFulfilled: (T) -> Unit, onCatch: ((Throwable) -> Unit)?): JsPromise<T> = apply {
    nextFns.add(onFulfilled)
    onCatch?.let { catchFns.add(it) }
    invokeAwait()
  }

  @Polyglot override fun then(onFulfilled: Value, onCatch: Value?): JsPromise<T> = then({
    onFulfilled.execute(it)
  }, {
    // if a catch function was provided, defer to that. otherwise, throw.
    when (onCatch) {
      null -> throw it
      else -> onCatch.execute(it)
    }
  })

  override fun catch(onRejected: (Throwable) -> Unit): JsPromise<T> = apply {
    catchFns.add(onRejected)
    invokeAwait()
  }

  @Polyglot override fun catch(onRejected: Value): JsPromise<T> = catch { err ->
    onRejected.execute(err)
  }
}

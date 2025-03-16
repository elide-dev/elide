/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
@file:Suppress("TooGenericExceptionCaught")

package elide.runtime.gvm.internals.intrinsics.js

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.graalvm.polyglot.Value
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import elide.runtime.exec.GuestExecutor
import elide.runtime.intrinsics.js.JsPromise
import elide.vm.annotations.Polyglot

/**
 * TBD.
 */
internal class JsPromiseImpl<T> private constructor (
  private val ready: AtomicBoolean = AtomicBoolean(false),
  private val latch: CountDownLatch? = null,
  private val producer: () -> T,
  private val future: ListenableFuture<T>,
  private val value: AtomicReference<T> = AtomicReference(),
  private val err: AtomicReference<Throwable> = AtomicReference(),
) : ListenableFuture<T> by future, JsPromise<T> {
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
    @JvmStatic fun <T> wrap(promise: ListenableFuture<T>): JsPromise<T> = JsPromiseImpl(
      producer = { promise.get() },
      future = promise,
    )

    @JvmStatic fun <T> GuestExecutor.spawn(promise: () -> T): JsPromise<T> = JsPromiseImpl(
      producer = promise,
      future = submit<T>(promise),
    )

    @JvmStatic fun <T> GuestExecutor.latched(latch: CountDownLatch, promise: () -> T): JsPromise<T> = JsPromiseImpl(
      producer = promise,
      latch = latch,
      future = submit<T>(promise),
    )

    @JvmStatic fun <T> GuestExecutor.spawnSuspending(promise: suspend () -> T): JsPromise<T> = JsPromiseImpl(
      producer = {
        runBlocking { promise.invoke() }
      },
      future = submit<T> {
        runBlocking(this) {
          promise.invoke()
        }
      },
    )

    @JvmStatic fun <T> wrapping(value: ListenableFuture<T>): JsPromise<T> = JsPromiseImpl<T>(
      ready = AtomicBoolean(true),
      producer = { value.get() },
      future = value,
    )

    @JvmStatic fun <T> resolved(value: T): JsPromise<T> = JsPromiseImpl(
      ready = AtomicBoolean(true),
      value = AtomicReference(value),
      producer = { value },
      future = Futures.immediateFuture(value),
    )

    @JvmStatic fun <T> rejected(err: Throwable): JsPromise<T> = JsPromiseImpl(
      ready = AtomicBoolean(true),
      err = AtomicReference(err),
      producer = { throw err },
      future = Futures.immediateFailedFuture(err),
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

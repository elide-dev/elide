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

package elide.runtime.gvm.internals.intrinsics.js

import org.graalvm.polyglot.Value
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.gvm.internals.intrinsics.js.JsPromiseImpl.Token.*
import elide.runtime.intrinsics.js.CompletableJsPromise
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.PromiseRejectedException
import elide.vm.annotations.Polyglot

/**
 * Implementation of a JavaScript Promise object that can be completed at will by host code.
 *
 * ### Semantics of Promise instances
 *
 * Promise objects should be viewed as 'slots' where future values will be stored, rather than a direct encapsulation
 * of async code. The semantics of this implementation are closer to that of Kotlin's
 * [CompletableDeferred][kotlinx.coroutines.CompletableDeferred], with the completable API only being available to the
 * host, since the promise itself does not invoke or track the execution of any code, but is instaed used by other code
 * to notify consumers about completion.
 *
 * ### Concurrency and guest callbacks
 *
 * Guest code may register callbacks on a promise from any thread, which can cause issues where a Promise attempts to
 * invoke a callback on a guest context that is in use at the time of completion. This problem will be solved by the
 * guest executor API, which will provide ways to pin the execution of code to a specific guest context.
 */
internal class JsPromiseImpl<T> private constructor(token: Token) : CompletableJsPromise<T> {
  /** Construct a new unresolved promise. */
  internal constructor() : this(Pending)

  /** Represents a state of a promise object. */
  private sealed interface Token {
    /** The promise has not been resolved nor rejected. */
    data object Pending : Token

    /** The promise has been resolved with a [value]. */
    @JvmInline value class Resolved(val value: Any?) : Token

    /** The promise has been rejected with the given [reason]. */
    @JvmInline value class Rejected(val reason: Any?) : Token
  }

  /** Thread-safe token representing the state of this promise. */
  private val token = AtomicReference(token)

  /** Callbacks to be invoked on successful resolution. */
  private val onResolve: MutableList<(T) -> Unit> = mutableListOf()

  /** Callbacks to be invoked on promise rejection. */
  private val onReject: MutableList<(Any?) -> Unit> = mutableListOf()

  /** The promise is considered as 'closed' once the [token] is no longer [Pending]. */
  override val isClosed: Boolean get() = token.get() != Pending

  @Suppress("UNCHECKED_CAST")
  override fun then(onFulfilled: (T) -> Unit, onCatch: ((Any?) -> Unit)?): JsPromise<T> = apply {
    when (val snap = token.get()) {
      // cast should be safe because only host code can complete the promise, so it is subject to static type checks
      is Resolved -> onFulfilled(snap.value as T)
      is Rejected -> onCatch?.let { it(snap.reason) }
      else -> {
        onResolve.add(onFulfilled)
        onCatch?.let { onReject.add(it) }
      }
    }
  }

  @Polyglot override fun then(onFulfilled: Value, onCatch: Value?): JsPromise<T> = then(
    onFulfilled = { onFulfilled.execute(it) },
    onCatch = {
      // if a catch function was provided, defer to that. otherwise, throw.
      when (onCatch) {
        null -> throw (it as? Throwable ?: PromiseRejectedException(it))
        else -> onCatch.execute(it)
      }
    },
  )

  override fun catch(onRejected: (Any?) -> Unit): JsPromise<T> = apply {
    when (val snap = token.get()) {
      Pending -> onReject.add(onRejected)
      is Rejected -> onRejected(snap.reason)
      is Resolved -> Unit // no-op
    }
  }

  @Polyglot override fun catch(onRejected: Value): JsPromise<T> = catch { err ->
    onRejected.execute(err)
  }

  override fun resolve(value: T): Boolean {
    if (!token.compareAndSet(Pending, Resolved(value))) return false
    onResolve.forEach { it(value) }
    return true
  }

  override fun reject(reason: Any?): Boolean {
    if (!token.compareAndSet(Pending, Rejected(reason))) return false
    onReject.forEach { it(reason) }
    return true
  }

  companion object {
    /** Create a new promise instance already resolved with the given [value]. */
    @JvmStatic fun <T> resolved(value: T): JsPromise<T> = JsPromiseImpl(Resolved(value))

    /** Create a new promise instance already rejected with the given [reason]. */
    @JvmStatic fun <T> rejected(reason: Any? = null): JsPromise<T> = JsPromiseImpl(Rejected(reason))
  }
}

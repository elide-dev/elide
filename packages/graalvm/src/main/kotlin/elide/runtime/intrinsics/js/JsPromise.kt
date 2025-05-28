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
package elide.runtime.intrinsics.js

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.util.concurrent.Future
import java.util.function.Supplier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import elide.runtime.exec.GuestExecutor
import elide.runtime.gvm.internals.intrinsics.js.JsPromiseImpl
import elide.runtime.intrinsics.js.err.TypeError
import elide.vm.annotations.Polyglot

/** Exception thrown when a non-throwable reason is used to reject a promise. */
public class PromiseRejectedException(public val reason: Any? = null) : RuntimeException() {
  override val message: String get() = "Promise was rejected with reason: $reason"
}

/**
 * A JavaScript Promise object, used to represent deferred values that can be subscribed to. Callbacks for promise
 * completion and rejection can be registered from both guest and host code.
 *
 * Promises are typically used to represent async operations; this can be done from host code by creating a new
 * [CompletableJsPromise], launching the operation in the background, and completing the promise when it finishes.
 *
 * Note that for the purposes of interoperability, _any_ value with a compatible `then` method is treated by the
 * JavaScript engine as a Promise-like object (called a _"thenable"_ value); this interface is used to standardize this
 * behavior from the host side.
 *
 * @see CompletableJsPromise
 */
public interface JsPromise<out T> : ProxyObject {
  /** Whether the promise has been resolved or rejected. */
  public val isDone: Boolean

  /**
   * Register a function to be called when the promise is fulfilled, and optionally, another callback to handle
   * rejection. If the promise is already [closed][isDone], the callbacks may be invoked immediately.
   *
   * @return this promise object.
   */
  public fun then(onFulfilled: (T) -> Unit, onCatch: ((Any?) -> Unit)? = null): JsPromise<T>

  /**
   * Register a guest value to be called when the promise is fulfilled, and optionally, another callback to handle
   * rejection. If the promise is already [closed][isDone], the callbacks may be invoked immediately.
   *
   * @return this promise object.
   */
  @Polyglot public fun then(onFulfilled: Value, onCatch: Value? = null): JsPromise<T>

  /**
   * Register a function to be called when this promise is rejected. If the promise has already been rejected, the
   * callback may be invoked immediately.
   *
   * @return this promise object.
   */
  public fun catch(onRejected: (Any?) -> Unit): JsPromise<T>

  /**
   * Register an executable guest value to be called when this promise is rejected. If the promise has already been
   * rejected, the callback may be invoked immediately.
   *
   * @return this promise object.
   */
  @Polyglot public fun catch(onRejected: Value): JsPromise<T>

  // proxy object members

  override fun getMemberKeys(): Array<String> = jsPromiseKeys
  override fun hasMember(key: String): Boolean = key in jsPromiseKeys
  override fun putMember(key: String?, value: Value?) {
    throw UnsupportedOperationException("Cannot set member on a Promise object")
  }

  override fun getMember(key: String): Any = when (key) {
    THEN_SYMBOL -> ProxyExecutable { args -> then(args[0], args.getOrNull(1)) }
    CATCH_SYMBOL -> ProxyExecutable { args -> catch(args[0]) }
    else -> throw IllegalArgumentException("Unknown member key: $key")
  }

  public companion object {
    // Keys available for object access on a JavaScript promise.
    private const val THEN_SYMBOL = "then"
    private const val CATCH_SYMBOL = "catch"
    private val jsPromiseKeys = arrayOf(THEN_SYMBOL, CATCH_SYMBOL)

    /** Create a new promise object resolved with the given [value]. */
    @JvmStatic public fun <T> resolved(value: T): JsPromise<T> = JsPromiseImpl.resolved(value)

    /** Create a new promise object rejected with the given [reason]. */
    @JvmStatic public fun <T> rejected(reason: Any? = null): JsPromise<T> = JsPromiseImpl.rejected(reason)

    /** Create a new promise wrapping the given listenable future. */
    @JvmStatic public fun <T> wrap(future: ListenableFuture<T>): JsPromise<T> {
      val promise = JsPromiseImpl<T>()
      future.addListener(
        /* listener = */
        {
          when (future.state()) {
            Future.State.SUCCESS -> promise.resolve(future.resultNow())
            Future.State.FAILED -> promise.reject(future.exceptionNow())
            Future.State.CANCELLED -> promise.reject()
            else -> error("Future cannot be running when listener is invoked")
          }
        },
        /* executor = */ MoreExecutors.directExecutor(),
      )
      return promise
    }

    /** Create a new promise encapsulating the result of launching an async operation on this guest executor. */
    @JvmStatic public fun <T> GuestExecutor.spawn(fn: () -> T): JsPromise<T> = wrap(submit(fn))

    /** Create a new promise wrapping the given supplier's getter. */
    @JvmStatic public fun <T> GuestExecutor.of(supplier: Supplier<T>): JsPromise<T> = wrap(submit(supplier::get))

    @JvmStatic public fun <T> wrap(
      value: Value,
      unwrapFulfilled: (Value?) -> T,
      unwrapRejected: (Value?) -> Any? = { it },
    ): JsPromise<T> {
      return wrapOrNull(value, unwrapFulfilled, unwrapRejected)
        ?: throw TypeError.create("Value $value is not a valid promise-like")
    }

    @JvmStatic public fun <T> wrapOrNull(
      value: Value,
      unwrapFulfilled: (Value?) -> T,
      unwrapRejected: (Value?) -> Any? = { it },
    ): JsPromise<T>? {
      return if (value.isProxyObject) runCatching { value.asProxyObject<JsPromise<T>>() }.getOrNull()
      else if (!value.canInvokeMember(THEN_SYMBOL)) null
      else object : JsPromise<T> {
        override val isDone: Boolean get() = false

        override fun then(onFulfilled: (T) -> Unit, onCatch: ((Any?) -> Unit)?): JsPromise<T> {
          return then(
            Value.asValue(ProxyExecutable { onFulfilled(unwrapFulfilled(it.firstOrNull())) }),
            Value.asValue(ProxyExecutable { onCatch?.invoke(unwrapRejected(it.firstOrNull())) }),
          )
        }

        override fun then(onFulfilled: Value, onCatch: Value?): JsPromise<T> {
          return wrap(
            value.invokeMember(THEN_SYMBOL, onFulfilled, onCatch),
            unwrapFulfilled,
            unwrapRejected,
          )
        }

        override fun catch(onRejected: (Any?) -> Unit): JsPromise<T> {
          return catch(Value.asValue(ProxyExecutable { onRejected.invoke(unwrapRejected(it.firstOrNull())) }))
        }

        override fun catch(onRejected: Value): JsPromise<T> {
          return wrap(value.invokeMember(CATCH_SYMBOL, onRejected), unwrapFulfilled, unwrapRejected)
        }
      }
    }
  }
}

/**
 * An extension of the basic [JsPromise] contract, allowing host code to complete or reject a promise when needed. This
 * enables the use of promise not just as an encapsulation of an ongoing operation but as a token representing a
 * deferred value.
 *
 * Long-lived promises representing the state of an object (such as in web stream readers) are a common use case for
 * explicit completion.
 */
public interface CompletableJsPromise<T> : JsPromise<T> {
  /**
   * Resolve the promise with [value]. This will invoke all registered resolution callbacks and complete the promise,
   * preventing further [resolve] or [reject] calls.
   *
   * @return `true` if the promise was resolved as a result of this call, `false` otherwise.
   */
  public fun resolve(value: T): Boolean

  /**
   * Reject the promise with [reason]. This will invoke all registered rejection callbacks and complete the promise,
   * preventing further [resolve] or [reject] calls.
   *
   * @return `true` if the promise was rejected as a result of this call, `false` otherwise.
   */
  public fun reject(reason: Any? = null): Boolean
}

/**
 * Create a new unresolved completable promise. Use [CompletableJsPromise.resolve] or [CompletableJsPromise.reject] to
 * control the outcome.
 */
@Suppress("FunctionName")
public fun <T> JsPromise(): CompletableJsPromise<T> = JsPromiseImpl()

/**
 * Returns a [Deferred] value that completes when this promise is resolve or rejected.
 */
public inline fun <reified T> JsPromise<T>.asDeferred(): Deferred<T> {
  val deferred = CompletableDeferred<T>()

  // complete the deferred when the promise is resolved/rejected
  then(
    onFulfilled = deferred::complete,
    onCatch = {
      if (it is Throwable) deferred.completeExceptionally(it)
      else deferred.completeExceptionally(PromiseRejectedException(it))
    },
  )

  // if supported, reject the promise when the deferred is cancelled
  if (this is CompletableJsPromise) deferred.invokeOnCompletion(::reject)

  return deferred
}

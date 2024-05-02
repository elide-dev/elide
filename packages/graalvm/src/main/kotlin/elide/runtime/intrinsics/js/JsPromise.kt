/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.function.Supplier
import elide.runtime.gvm.internals.intrinsics.js.JsPromiseImpl
import elide.vm.annotations.Polyglot

private const val THEN_SYMBOL = "then"
private const val CATCH_SYMBOL = "catch"

// Keys available for object access on a JavaScript promise.
private val jsPromiseKeys = arrayOf(
  THEN_SYMBOL,
  CATCH_SYMBOL,
)

/**
 * TBD.
 */
public interface JsPromise<T> : ProxyObject {
  /**
   * TBD.
   */
  public fun then(onFulfilled: (T) -> Unit, onCatch: ((Throwable) -> Unit)? = null): JsPromise<T>

  /**
   * TBD.
   */
  @Polyglot public fun then(onFulfilled: Value, onCatch: Value? = null): JsPromise<T>

  /**
   * TBD.
   */
  public fun catch(onRejected: (Throwable) -> Unit): JsPromise<T>

  /**
   * TBD.
   */
  @Polyglot public fun catch(onRejected: Value): JsPromise<T>

  override fun getMemberKeys(): Array<String> = jsPromiseKeys
  override fun hasMember(key: String): Boolean = key in jsPromiseKeys
  override fun putMember(key: String?, value: Value?) {
    throw UnsupportedOperationException("Cannot set member on a Promise object")
  }

  override fun getMember(key: String): Any = when (key) {
    THEN_SYMBOL -> ProxyExecutable { args ->
      then(args[0], args.getOrNull(1))
    }
    CATCH_SYMBOL -> ProxyExecutable { args ->
      catch(args[0])
    }
    else -> throw IllegalArgumentException("Unknown member key: $key")
  }

  public companion object {
    @JvmStatic public fun <T> of(latch: CountDownLatch, producer: Supplier<T>): JsPromise<T> =
      JsPromiseImpl.of(latch, producer)

    @JvmStatic public fun <T> of(producer: Supplier<T>): JsPromise<T> = JsPromiseImpl.of(producer)

    @JvmStatic public fun <T> of(promise: Future<T>): JsPromise<T> = JsPromiseImpl.of(promise)

    @JvmStatic public fun <T> resolved(value: T): JsPromise<T> = JsPromiseImpl.resolved(value)

    @JvmStatic public fun <T> rejected(err: Throwable): JsPromise<T> = JsPromiseImpl.rejected(err)
  }
}

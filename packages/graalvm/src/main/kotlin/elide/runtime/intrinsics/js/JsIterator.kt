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

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyIterable
import org.graalvm.polyglot.proxy.ProxyIterator
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.JsIterator.JsIteratorResult
import elide.runtime.intrinsics.js.err.Error
import elide.vm.annotations.Polyglot

/**
 * # JS: Iterator
 *
 * Implements a standards-compliant JavaScript iterator, which either provides streamed values as it is polled, or fails
 * upon value access if an error is encountered.
 *
 * @see ProxyIterator for GraalVM's Truffle iterator interface.
 * @see JsIteratorResult for a result type that can be returned by an iterator.
 */
public interface JsIterator<T> : Iterator<JsIteratorResult<T>>, ProxyIterator, ProxyIterable {
  /** Represents an inner iterator value. */
  public class JsIteratorResult<T> private constructor (
    private val held: T?,
    @Polyglot public val done: Boolean,
    private val err: Throwable? = null,
  ) : ProxyObject {
    /** Fetch the held value or throw the held error. */
    @get:Polyglot public val value: T? get() = if (err != null) {
      throw err
    } else {
      held
    }

    override fun hasMember(key: String): Boolean = key == "value" || key == "done"

    override fun getMemberKeys(): Array<String> = arrayOf(
      "value",
      "done",
    )

    override fun getMember(key: String?): Any? {
      return when (key) {
        "value" -> value
        "done" -> done
        else -> throw IllegalArgumentException("Unknown member key: $key")
      }
    }

    override fun putMember(key: String?, value: Value?) {
      // no-op
    }

    override fun removeMember(key: String?): Boolean {
      return false
    }

    internal companion object {
      /** @return Paired [value] and [done] flag. */
      @JvmStatic fun <T> of(value: T?, done: Boolean): JsIteratorResult<T> =
        JsIteratorResult(value, done)

      /** @return Error constructor. */
      @JvmStatic fun <T> ofErr(err: Error): JsIteratorResult<T> =
        JsIteratorResult(null, true, err)
    }
  }

  /**
   * Default implementation of a JavaScript-compatible iterator, via [JsIterator] of [T] and [ProxyIterator].
   *
   * @see JsIterator for the Elide-specific interface for JavaScript iterators.
   * @see ProxyIterator for the interface provided by Truffle to mimic iterators.
   * @see ProxyIterable for the interface provided by Truffle to mimic iterable objects.
   */
  public class JsIteratorImpl<T> (private val iter: Iterator<T>) :
    JsIterator<T>,
    ProxyIterator,
    ProxyIterable,
    ProxyObject {
    @Polyglot override fun hasNext(): Boolean = iter.hasNext()

    @Polyglot override fun next(): JsIteratorResult<T> = try {
      JsIteratorResult.of(iter.next(), !hasNext())
    } catch (err: Throwable) {
      JsIteratorResult.ofErr(JsError.wrap(err))
    }

    override fun getIterator(): Any = this

    override fun getMemberKeys(): Array<String> = arrayOf(
      "next",
      "hasNext",
    )

    override fun hasMember(key: String): Boolean = key in memberKeys

    override fun getMember(key: String): Any = when (key) {
      "next" -> ProxyExecutable { next() }
      "hasNext" -> ProxyExecutable { hasNext() }
      else -> throw IllegalArgumentException("Unknown member key: $key")
    }

    override fun putMember(key: String?, value: Value?) {
      // no-op
    }

    override fun removeMember(key: String?): Boolean {
      return false
    }
  }

  /**
   * ## JS: Iterator Factory
   *
   * Factory helper for creating JavaScript iterators. Static constructor methods for iterators are available on this
   * object (host-side).
   */
  public object JsIteratorFactory {
    /** Wrap the provided [iterator] in a JS iterator proxy. */
    @JvmStatic public fun <T> forIterator(iterator: Iterator<T>): JsIterator<T> = JsIteratorImpl(iterator)
  }

  /**
   * Iterator: Terminate and return with the provided [value].
   *
   * Finishes the current iterator. The [value] is returned as the final value.
   *
   * @param value The value to return.
   * @return The final iterator result.
   */
  @Polyglot public fun `return`(value: T): JsIteratorResult<T> = JsIteratorResult.of(
    value,
    true,
  )

  /**
   * Iterator: Terminate with the provided [err].
   *
   * Finishes the current iterator, throwing the provided [err] when `value` is accessed.
   *
   * @return The final iterator result.
   */
  @Polyglot public fun <T, E: Error> `throw`(err: E): JsIteratorResult<T> = JsIteratorResult.ofErr(
    err,
  )

  /**
   * Fetch the next value from the current iterator.
   *
   * If a value is present, it is returned. If the iterator was already finished and no value is available to return,
   * an error is thrown.
   *
   * @return The next value from the iterator.
   */
  @Polyglot override fun next(): JsIteratorResult<T>

  @Polyglot override fun getNext(): T? = next().value

  override fun getIterator(): Any = this
}

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
@file:Suppress("WildcardImport", "TooManyFunctions")

package elide.runtime.gvm.js

import org.graalvm.polyglot.Value
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import elide.runtime.intrinsics.js.err.*
import elide.runtime.intrinsics.js.err.JsError

/** Utility for wrapping JavaScript error types. */
@Suppress("unused") public object JsError {
  // Wrap a caught `Throwable` in the provided JS error `type`.
  @Suppress("UNCHECKED_CAST")
  private fun <E: AbstractJsException> wrapped(error: Throwable, type: KClass<out E>): E {
    return (type.companionObjectInstance as AbstractJsException.ErrorFactory<E>).create(error)
  }

  // Wrap a string `message` and optional `Throwable` `cause` in the provided JS error `type`.
  @Suppress("UNCHECKED_CAST")
  private fun <E: AbstractJsException> wrapped(message: String, cause: Throwable? = null, type: KClass<out E>): E {
    return (type.companionObjectInstance as AbstractJsException.ErrorFactory<E>).create(message, cause)
  }

  /**
   * Manufacture a generic JavaScript error with the provided [msg] and optional cause.
   *
   * @param msg Message to enclose for the error.
   * @param errno Optional error number to include.
   * @param extraProps Extra properties to mount on the error.
   * @return Constructed JS exception type.
   */
  public fun of(
    msg: String,
    errno: Int? = null,
    vararg extraProps: Pair<String, Any>,
  ): Error = object : JsError, Error(extraProps.map { it.first to Value.asValue(it.second) }.toTypedArray()) {
    override val message: String get() = msg
    override val errno: Int? get() = errno
    override val name: String get() = cause?.javaClass?.simpleName ?: "Error"
  }

  /**
   * Manufacture a generic JavaScript error with the provided [msg] and optional [cause].
   *
   * @param msg Message to enclose for the error.
   * @param cause Caught error to wrap, if any.
   * @param errno Optional error number to include.
   * @param extraProps Extra properties to mount on the error.
   * @return Constructed JS exception type.
   */
  public fun of(
    msg: String,
    cause: Throwable?,
    errno: Int? = null,
    vararg extraProps: Pair<String, Any>,
  ): Error = object : Error(extraProps.map { it.first to Value.asValue(it.second) }.toTypedArray()) {
    override val message: String get() = msg
    override val errno: Int? get() = errno
    override val name: String get() = cause?.javaClass?.simpleName ?: "Error"
  }

  /**
   * Manufacture a generic JavaScript error with the provided [msg] and optional [cause].
   *
   * @param msg Message to enclose for the error.
   * @param cause Caught error to wrap, if any.
   * @return Constructed JS exception type.
   */
  @Suppress("NOTHING_TO_INLINE")
  public inline fun error(
    msg: String,
    cause: Throwable? = null,
    errno: Int? = null,
    vararg extraProps: Pair<String, Any>,
  ): Nothing = throw of(msg, cause, errno = errno, *extraProps)

  /**
   * Wrap a caught [throwable] in a JavaScript error; if no error type is specified, a [ValueError] will be raised.
   *
   * @param throwable Caught error to wrap.
   * @return Constructed JS exception type.
   */
  public fun wrap(throwable: Throwable, type: KClass<out Error>? = null): Error = if (type == null) {
    wrapped(throwable, TypeError::class)
  } else {
    wrapped(throwable, type)
  }

  /**
   * [TypeError] convenience function: wrap the provided [error] and optionally [raise].
   *
   * @param error Throwable error to wrap as a [TypeError].
   * @param raise Whether to throw the error after wrapping. Defaults to `false`.
   * @return Wrapped [TypeError], ready to be raised.
   */
  @Throws(TypeError::class)
  public fun typeError(error: Throwable, raise: Boolean = false): TypeError {
    val exc = wrapped(error, TypeError::class)
    if (raise) throw exc
    return exc
  }

  /**
   * [TypeError] convenience function: wrap the provided [message] and optional [cause]; then, optionally [raise].
   *
   * @param message String message for this error.
   * @param cause Throwable error to wrap and consider the cause of this error.
   * @param raise Whether to throw the error after wrapping. Defaults to `false`.
   * @return Wrapped [TypeError], ready to be raised.
   */
  @Throws(TypeError::class)
  public fun typeError(message: String, cause: Throwable? = null, raise: Boolean = false): TypeError {
    val exc = wrapped(message, cause, TypeError::class)
    if (raise) throw exc
    return exc
  }

  /**
   * [RangeError] convenience function: wrap the provided [message] and optional [cause]; then, optionally [raise].
   *
   * @param message String message for this error.
   * @param cause Throwable error to wrap and consider the cause of this error.
   * @param raise Whether to throw the error after wrapping. Defaults to `false`.
   * @return Wrapped [RangeError], ready to be raised.
   */
  @Throws(RangeError::class)
  public fun rangeError(message: String, cause: Throwable? = null, raise: Boolean = false): RangeError {
    val exc = wrapped(message, cause, RangeError::class)
    if (raise) throw exc
    return exc
  }

  /**
   * [ValueError] convenience function: wrap the provided [error] and optionally [raise].
   *
   * @param error Throwable error to wrap as a [ValueError].
   * @param raise Whether to throw the error after wrapping. Defaults to `false`.
   * @return Wrapped [ValueError], ready to be raised.
   */
  @Throws(ValueError::class)
  public fun valueError(error: Throwable, raise: Boolean = false): ValueError {
    val exc = wrapped(error, ValueError::class)
    if (raise) throw exc
    return exc
  }

  /**
   * [ValueError] convenience function: wrap the provided [error] and optionally [raise].
   *
   * @param message String message for this error.
   * @param cause Throwable error to wrap and consider the cause of this error.
   * @param raise Whether to throw the error after wrapping. Defaults to `false`.
   * @return Wrapped [ValueError], ready to be raised.
   */
  @Throws(ValueError::class)
  public fun valueError(message: String, cause: Throwable? = null, raise: Boolean = false): ValueError {
    val exc = wrapped(message, cause, ValueError::class)
    if (raise) throw exc
    return exc
  }

  /**
   * TBD.
   */
  @Throws(Error::class)
  public fun <R: Any> jsErrors(op: () -> R): R {
    return try {
      op.invoke()
    } catch (value: IllegalArgumentException) {
      // typically, these should raise as `ValueError`.
      throw wrapped(value, ValueError::class)
    } catch (state: ClassCastException) {
      // typically, these should raise as `TypeError`.
      throw wrapped(state, TypeError::class)
    }
  }
}

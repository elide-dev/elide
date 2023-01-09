package elide.runtime.gvm.internals.intrinsics.js

import elide.runtime.intrinsics.js.err.AbstractJSException
import elide.runtime.intrinsics.js.err.Error
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.err.ValueError
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

/** Utility for wrapping JavaScript error types. */
@Suppress("unused") internal object JsError {
  // Wrap a caught `Throwable` in the provided JS error `type`.
  @Suppress("UNCHECKED_CAST")
  private fun <E: AbstractJSException> wrapped(error: Throwable, type: KClass<out E>): E {
    return (type.companionObjectInstance as AbstractJSException.ErrorFactory<E>).create(error)
  }

  // Wrap a string `message` and optional `Throwable` `cause` in the provided JS error `type`.
  @Suppress("UNCHECKED_CAST")
  private fun <E: AbstractJSException> wrapped(message: String, cause: Throwable? = null, type: KClass<out E>): E {
    return (type.companionObjectInstance as AbstractJSException.ErrorFactory<E>).create(message, cause)
  }

  /**
   * Wrap a caught [throwable] in a JavaScript error; if no error type is specified, a [ValueError] will be raised.
   *
   * @param throwable Caught error to wrap.
   * @return Constructed JS exception type.
   */
  fun wrap(throwable: Throwable, type: KClass<out Error>? = null): Error {
    return if (type == null) {
      wrapped(throwable, TypeError::class)
    } else {
      wrapped(throwable, type)
    }
  }

  /**
   * [TypeError] convenience function: wrap the provided [error] and optionally [raise].
   *
   * @param error Throwable error to wrap as a [TypeError].
   * @param raise Whether to throw the error after wrapping. Defaults to `false`.
   * @return Wrapped [TypeError], ready to be raised.
   */
  @Throws(TypeError::class)
  fun typeError(error: Throwable, raise: Boolean = false): TypeError {
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
  fun typeError(message: String, cause: Throwable? = null, raise: Boolean = false): TypeError {
    val exc = wrapped(message, cause, TypeError::class)
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
  fun valueError(error: Throwable, raise: Boolean = false): ValueError {
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
  fun valueError(message: String, cause: Throwable? = null, raise: Boolean = false): ValueError {
    val exc = wrapped(message, cause, ValueError::class)
    if (raise) throw exc
    return exc
  }

  /**
   * TBD.
   */
  @Throws(elide.runtime.intrinsics.js.err.Error::class)
  fun <R: Any> jsErrors(op: () -> R): R {
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

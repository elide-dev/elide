package elide.runtime.intrinsics.js

import elide.annotations.core.Polyglot
import elide.runtime.intrinsics.js.err.Error

/**
 * # JS: Iterator
 *
 * Implements a standards-compliant JavaScript iterator, which either provides streamed values as it is polled, or fails
 * upon value access if an error is encountered.
 *
 * @see JsIteratorResult for a result type that can be returned by an iterator.
 */
public interface JsIterator<T> : Iterator<JsIterator.JsIteratorResult<T>> {
  /** Represents an inner iterator value. */
  public class JsIteratorResult<T> private constructor (
    private val held: T?,
    @Polyglot public val done: Boolean,
    private val err: Throwable? = null,
  ) {
    /** Fetch the held value or throw the held error. */
    @get:Polyglot public val value: T? get() = if (err != null) {
      throw err
    } else {
      held
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
   * ## JS: Iterator Factory
   *
   * Factory helper for creating JavaScript iterators. Static constructor methods for iterators are available on this
   * object (host-side).
   */
  public object JsIteratorFactory {
    /** Wrap the provided [iterator] in a JS iterator proxy. */
    @JvmStatic public fun <T> forIterator(iterator: Iterator<T>): JsIterator<T> = object: JsIterator<T> {
      /** @inheritDoc */
      @Polyglot override fun hasNext(): Boolean = iterator.hasNext()

      /** @inheritDoc */
      @Polyglot override fun next(): JsIteratorResult<T> = JsIteratorResult.of(
        value = iterator.next(),
        done = !iterator.hasNext(),
      )
    }
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
}

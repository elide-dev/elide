package elide.runtime.intrinsics.js

import elide.annotations.core.Polyglot

/**
 * TBD.
 */
public interface JsIterator<T: Any> : Iterator<JsIterator.JsIteratorResult<T>> {
  /** Represents an inner iterator value. */
  public class JsIteratorResult<T: Any> private constructor (
    @Polyglot public val value: T?,
    @Polyglot public val done: Boolean,
  ) {
    internal companion object {
      /** @return Paired [value] and [done] flag. */
      @JvmStatic fun <T: Any> of(value: T?, done: Boolean): JsIteratorResult<T> =
        JsIteratorResult(value, done)
    }
  }

  /**
   * TBD.
   */
  public object JsIteratorFactory {
    /** Wrap the provided [iterator] in a JS iterator proxy. */
    @JvmStatic public fun <T: Any> forIterator(iterator: Iterator<T>): JsIterator<T> = object: JsIterator<T> {
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
   * TBD.
   */
  @Polyglot public fun `return`(value: T): JsIteratorResult<T> = JsIteratorResult.of(
    value,
    true,
  )

  /**
   * TBD.
   */
  @Polyglot public fun <E: Any> `throw`(context: E? = null): JsIteratorResult<E> = JsIteratorResult.of(
    value = context,
    done = true,
  )

  /**
   * TBD.
   */
  @Polyglot override fun next(): JsIteratorResult<T>
}

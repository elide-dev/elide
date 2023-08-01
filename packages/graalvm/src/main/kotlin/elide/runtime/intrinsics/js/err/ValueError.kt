package elide.runtime.intrinsics.js.err

/**
 * # JavaScript: `ValueError`
 *
 * This type implements the API surface of a `ValueError` exception raised within the context of an executing JavaScript
 * guest. `ValueError` instances are typically raised when a value is passed to a function or operation that is not
 * valid or legal, although the type of the value is legal.
 *
 * An example of a `ValueError` would be the `port` property on a [elide.runtime.intrinsics.js.URL] object: if a value
 * is provided which is a valid [Int], but outside the range of valid port numbers (`1-65535`), a [ValueError] is raised
 * instead of a [TypeError].
 *
 * &nbsp;
 *
 * ## Further reading
 *
 * For more information about the expected behavior and API surface of a [ValueError], see the following resources:
 * - [MDN: `ValueError`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/ValueError)
 *
 * @see AbstractJSException for the host base interface type of all JavaScript exceptions.
 * @see Error for the top-most guest-exposed base class for all JavaScript errors.
 */
public abstract class ValueError : AbstractJSException, Error() {
  /** @inheritDoc */
  override val name: String get() = "ValueError"

  /**
   * ## Factory: `ValueError`
   *
   * Public factory for [ValueError] types. Java-style exceptions can be wrapped using the [create] method, or a string
   * message and cause can be provided, a-la Java exceptions.
   */
  public companion object Factory : AbstractJSException.ErrorFactory<ValueError> {
    /** @inheritDoc */
    override fun create(error: Throwable): ValueError {
      return object : ValueError() {
        override val message: String get() = error.message ?: "An error occurred"
      }
    }

    /** @inheritDoc */
    override fun create(message: String, cause: Throwable?): ValueError {
      return object : ValueError() {
        override val message: String get() = message
        override val cause: Error? get() = if (cause != null) {
          create(cause)
        } else {
          null
        }
      }
    }
  }
}

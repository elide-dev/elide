package elide.runtime.intrinsics.js.err

/**
 * # JavaScript: Abstract Exception
 *
 * This sealed interface serves as the base type for all known JavaScript guest exception types (not counting user code
 * which creates such types). Implementations or interfaces which extend this class implement core JavaScript exception
 * types which are surfaced from intrinsics.
 */
public sealed interface AbstractJSException {
  /**
   * ## JavaScript: Error Factory
   *
   * All JavaScript exception intrinsics are expected to implement this interface on their companion object; this allows
   * for dynamic extension/implementation of a given JS exception, based on an originating Java exception.
   *
   * When constructing a JS error from a Java exception, Elide will attempt to use an appropriate JS error type, and
   * will provide the maximum amount of developer ergonomics possible to identify where the error took place.
   *
   * @param T type of JavaScript error implemented by this factory. Must implement [AbstractJSException].
   * @see AbstractJSException for the regular object interface implemented for each JS exception type.
   */
  public interface ErrorFactory<T : AbstractJSException> {
    /**
     * ## Interface: Create from [Throwable]
     *
     * Create a JavaScript-environment error of type [T] (an [AbstractJSException]) which wraps the provided [Throwable]
     * [error]. Any non-private fields and information on the provided [Throwable] will be included with the JS error.
     *
     * @param error Error to wrap in a JS error.
     * @return JS error instance [T].
     */
    public fun create(error: Throwable): T

    /**
     * ## Interface: Create from and [message] and optional [cause]
     *
     * Create a JavaScript-environment error of type [T] (an [AbstractJSException]) which wraps the provided [message]
     * string and optional [Throwable] [cause]. Any non-private fields and information on the provided [Throwable], if
     * present, will be included with the JS error.
     *
     * @param message Error message to include.
     * @param cause Error cause, if applicable/known. Optional; defaults to `null`.
     * @return JS error instance [T].
     */
    public fun create(message: String, cause: Throwable? = null): T
  }
}

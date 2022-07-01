package elide.model.err

/**
 * Defines a class of exceptions which can be encountered when interacting with persistence tools, including internal
 * (built-in) data adapters.
 */
public abstract class PersistenceException
  protected constructor(message: String?, cause: Throwable?): RuntimeException(message, cause) {
  protected constructor(message: String): this(message, null)
  protected constructor(throwable: Throwable): this(null, throwable)
}

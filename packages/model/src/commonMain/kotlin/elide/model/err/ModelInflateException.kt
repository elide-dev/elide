package elide.model.err

/**
 * Describes an error that occurred while de-serializing a model.
 */
public class ModelInflateException(message: String?, cause: Throwable?): PersistenceException(message, cause) {
  public constructor(message: String): this(message, null)
  public constructor(throwable: Throwable): this(null, throwable)
}

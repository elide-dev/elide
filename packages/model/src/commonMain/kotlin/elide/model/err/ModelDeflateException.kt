package elide.model.err

/**
 * Describes an error that occurred while serializing a model.
 */
public class ModelDeflateException(message: String?, cause: Throwable?): PersistenceException(message, cause) {
  public constructor(message: String): this(message, null)
  public constructor(throwable: Throwable): this(null, throwable)
}

package elide.model.cfg

/**
 * Specifies options related to generic query capabilities expressed via Elide's built-in model layer; drivers may
 * override this interface and provide their own concrete options.
 */
public interface QueryOptions: FetchOptions, OperationOptions {
  public companion object {
    /** Default set of query operation options.  */
    public val DEFAULTS: QueryOptions = object : QueryOptions {}
  }
}

package elide.model.cfg

/** Describes options specifically involved with updating existing model entities. */
public interface UpdateOptions: CacheOptions, WriteOptions, OperationOptions {
  public companion object {
    /** Default set of update operation options. */
    public val DEFAULTS: UpdateOptions = object : UpdateOptions {}
  }
}

package elide.model.cfg

/** Describes options specifically involved with deleting existing model entities. */
public interface DeleteOptions: CacheOptions, OperationOptions {
  public companion object {
    /** Default set of delete operation options.  */
    public val DEFAULTS: DeleteOptions = object : DeleteOptions {}
  }
}

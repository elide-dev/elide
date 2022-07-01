package elide.model.cfg

/** Describes options involved with operations to persist model entities. */
public interface WriteOptions: OperationOptions {
  /** Enumerates write attitudes with regard to existing record collisions.  */
  public enum class WriteDisposition {
    /** We don't care. Just write it.  */
    BLIND,

    /** The record must exist for the write to proceed (an *update* operation).  */
    MUST_EXIST,

    /** The record must **not** exist for the write to proceed (a *create* operation).  */
    MUST_NOT_EXIST
  }

  public companion object {
    /** Default set of write operation options. */
    public val DEFAULTS: WriteOptions = object : WriteOptions {}
  }

  /** @return Specifies the write mode for an operation. Overridden by some methods (for instance, `create`). */
  public fun writeMode(): WriteDisposition? {
    return null
  }

  /** @return Write prefix specified for this operation, if any, otherwise, `null`. */
  public fun writePrefix(): String? {
    return null
  }
}

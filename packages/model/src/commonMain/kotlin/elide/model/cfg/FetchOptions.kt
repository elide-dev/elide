package elide.model.cfg

import elide.model.FieldMask

/**
 * Specifies options which may be applied, generically, to model instance fetch operations implemented through the main
 * `PersistenceDriver` interface.
 */
public interface FetchOptions : CacheOptions, OperationOptions {
  /** Enumerates ways the `FieldMask` may be applied.  */
  public enum class MaskMode {
    /** Only include fields mentioned in the field mask.  */
    INCLUDE,

    /** Omit fields mentioned in the field mask.  */
    EXCLUDE,

    /** Treat the field mask as a projection, for query purposes only.  */
    PROJECTION
  }

  /** @return Field mask to apply when fetching properties. Fields not mentioned in the mask will be omitted. */
  public fun fieldMask(): FieldMask? {
    return null
  }

  /** @return Mode to operate in when applying the affixed field mask, if any. */
  public fun fieldMaskMode(): MaskMode {
    return MaskMode.INCLUDE
  }

  /** @return Read snapshot time, if applicable.
   */
  public fun snapshot(): Long? {
    return null
  }

  public companion object {
    /** Default set of fetch options.  */
    public val DEFAULTS: FetchOptions = object : FetchOptions {}
  }
}

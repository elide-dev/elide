package elide.model.cfg

/**
 * Specifies operational options related to caching; these options are usable alongside to the main [OperationOptions]
 * tree.
 *
 * See below for a description of each configurable property.
 *
 * **Cache configuration (**defaults** in parens):
 ** *
 *  * [enableCache] (`true`): Whether to allow caching at all.
 *  * [cacheTimeoutMilliseconds] (`2000L`): Amount of time to give the cache before falling back to storage.
 *  * [cacheDefaultTTLMilliseconds] (`60000L`): Default amount of time to let things stick around in the cache.
 *  * [cacheEvictionMode]: Eviction mode to operate in.
 */
@Suppress("MemberVisibilityCanBePrivate")
public interface CacheOptions : OperationOptions {
  /** Describes operating modes with regard to cache eviction.  */
  public enum class EvictionMode(
    /** Pretty label for this mode.  */
    public val label: String
  ) {
    /** Flag to enable TTL enforcement.  */
    TTL("Time-to-Live"),

    /** Least-Frequently-Used mode for cache eviction.  */
    LFU("Least-Frequently Used"),

    /** Least-Recently-Used mode for cache eviction.  */
    LRU("Least-Recently Used");

    override fun toString(): String {
      return "EvictionMode($name - $label)"
    }
  }

  /** @return Whether the cache should be enabled, if installed. Defaults to `true`. */
  public fun enableCache(): Boolean {
    return true
  }

  /** @return Value to apply to the cache timeout. If left unspecified, the global default is used. */
  public fun cacheTimeoutMilliseconds(): Long? {
    return 2L * 1000L  // two seconds
  }

  /** @return Default amount of time, in milliseconds, to let things remain in the cache. */
  public fun cacheDefaultTTLMilliseconds(): Long? {
    return 60L * 60L * 1000L  // one hour
  }

  /** @return Specifier describing the cache eviction mode to apply, if any. */
  public fun cacheEvictionMode(): EvictionMode {
    return EvictionMode.TTL
  }
}

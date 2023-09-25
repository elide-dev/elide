package elide.runtime.core

import java.util.concurrent.ConcurrentSkipListMap
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess.ALLOW_NONE

/**
 * This class acts as the root of the engine configuration DSL, allowing plugins to be
 * [installed][PluginRegistry.install] and exposing general features such as
 * [enabling support for specific languages][enableLanguage].
 *
 * Instances of this class cannot be created manually, instead, they are provided by the [PolyglotEngine] method, which
 * serves as entry point for the DSL.
 */
@DelicateElideApi public abstract class PolyglotEngineConfiguration internal constructor() : PluginRegistry {
  /**
   * Enumerates the access privileges that can be conceded to guest code over host resources, such as environment
   * variables, or file system (IO).
   *
   * @see hostAccess
   */
  public enum class HostAccess {
    /** Allow guest code to access the host file system instead of an embedded, in-memory VFS. */
    ALLOW_IO,

    /** Allow guest code to access environment variables from the host. */
    ALLOW_ENV,

    /** Allow both filesystem and environment access. */
    ALLOW_ALL,

    /** Restrict all access to the host environment. */
    ALLOW_NONE,
  }

  /** Information about the platform hosting the runtime. */
  public val hostPlatform: HostPlatform = HostPlatform.resolve()

  /** The access granted to guest code over host resources, such as environment variables and the file system. */
  public var hostAccess: HostAccess = ALLOW_NONE

  /** Environment to apply to the context. */
  public val environment: MutableMap<String, String> = ConcurrentSkipListMap()

  /** Enables support for the specified [language] on all contexts created by the engine. */
  public abstract fun enableLanguage(language: GuestLanguage)
}

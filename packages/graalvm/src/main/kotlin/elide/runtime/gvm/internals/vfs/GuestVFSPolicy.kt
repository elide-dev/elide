package elide.runtime.gvm.internals.vfs

import io.micronaut.context.annotation.ConfigurationProperties

/**
 * Micronaut-compatible configuration for guest virtual file-system (VFS) security policy.
 *
 * @param mode Base operating mode for I/O - options are [Mode.GUEST] (virtualized), [Mode.CLASSPATH] (guest + access to
 *   host app classpath), or [Mode.HOST] (full access to host machine I/O).
 * @param readOnly Whether to force the guest VM to operate in read-only mode.
 */
@ConfigurationProperties("elide.gvm.vfs.policy")
public data class GuestVFSPolicy(
  var mode: Mode? = DEFAULT_MODE,
  var readOnly: Boolean = DEFAULT_READ_ONLY,
) {
  /** Enumerates supported operating modes for VM guest I/O. */
  @Suppress("unused") public enum class Mode {
    /** Virtualized I/O operations via a guest file-system. */
    GUEST,

    /** Access to resources from the host app classpath. */
    CLASSPATH,

    /** Access to host I/O (i.e. regular I/O on the host machine). */
    HOST,
  }

  public companion object {
    /** Default settings. */
    @JvmStatic public val DEFAULTS: GuestVFSPolicy = GuestVFSPolicy()

    /** Default value for the `readOnly` setting. */
    public const val DEFAULT_READ_ONLY: Boolean = true

    /** Default operating mode. */
    public val DEFAULT_MODE: Mode = Mode.GUEST
  }
}

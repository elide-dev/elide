package elide.runtime.gvm.internals.vfs

import io.micronaut.context.annotation.ConfigurationProperties

/**
 * Micronaut-compatible configuration for guest virtual file-system (VFS) security policy.
 */
@ConfigurationProperties("elide.gvm.vfs.policy")
public interface GuestVFSPolicy {
  public companion object {
    /** Default settings. */
    @JvmStatic public val DEFAULTS: GuestVFSPolicy = object : GuestVFSPolicy {}

    /** Default value for the `readOnly` setting. */
    public const val DEFAULT_READ_ONLY: Boolean = true
  }

  /**
   * @return Whether to force the guest VM to operate in read-only mode.
   */
  public val readOnly: Boolean? get() = DEFAULT_READ_ONLY

  /** @return Response for an access check against the provided [request]. */
  @Suppress("UNUSED_PARAMETER")
  public fun evaluateForPath(request: AccessRequest): AccessResponse {
    // TODO(sgammon): temporarily allow all
    return AccessResponse.allow()
  }
}

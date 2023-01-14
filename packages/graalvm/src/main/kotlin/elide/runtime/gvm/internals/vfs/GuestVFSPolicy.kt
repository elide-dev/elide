package elide.runtime.gvm.internals.vfs

import io.micronaut.context.annotation.ConfigurationProperties

/**
 * Micronaut-compatible configuration for guest virtual file-system (VFS) security policy.
 *
 * @param readOnly Whether to force the guest VM to operate in read-only mode.
 */
@ConfigurationProperties("elide.gvm.vfs.policy")
public data class GuestVFSPolicy(
  var readOnly: Boolean = DEFAULT_READ_ONLY,
) {
  public companion object {
    /** Default settings. */
    @JvmStatic public val DEFAULTS: GuestVFSPolicy = GuestVFSPolicy()

    /** Default value for the `readOnly` setting. */
    public const val DEFAULT_READ_ONLY: Boolean = true
  }

  /** @return Response for an access check against the provided [request]. */
  @Suppress("UNUSED_PARAMETER") internal fun evaluateForPath(request: AccessRequest): AccessResponse {
    // TODO(sgammon): temporarily allow all
    return AccessResponse.allow()
  }
}

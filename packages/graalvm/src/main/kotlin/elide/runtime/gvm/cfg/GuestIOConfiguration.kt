package elide.runtime.gvm.cfg

import elide.runtime.gvm.cfg.GuestIOConfiguration.Mode
import elide.runtime.gvm.internals.vfs.AbstractBaseVFS
import elide.runtime.gvm.internals.vfs.GuestVFSPolicy
import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable

/**
 * Micronaut configuration for the guest VM virtual file-system (VFS).
 *
 * @param enabled Whether to enable I/O support for guest VMs at all.
 * @param bundle Path to the VFS bundle to use for guest VMs, as applicable.
 * @param policy Security policies to apply to guest I/O operations. If none is provided, sensible defaults are used.
 * @param mode Base operating mode for I/O - options are [Mode.GUEST] (virtualized), [Mode.CLASSPATH] (guest + access to
 *   host app classpath), or [Mode.HOST] (full access to host machine I/O).
 * @param caseSensitive Whether to treat the file-system as case sensitive. Defaults to `true`.
 * @param symlinks Whether to enable (or expect) symbolic link support. Defaults to `true`.
 * @param root Root path for the file-system. Defaults to `/`.
 * @param workingDirectory Working directory to initialize the VFS with, as applicable. Defaults to `/`.
 */
@Suppress("MemberVisibilityCanBePrivate")
@ConfigurationProperties("elide.gvm.vfs")
internal class GuestIOConfiguration(
  var enabled: Boolean = DEFAULT_ENABLED,
  var bundle: String? = null,
  var policy: GuestVFSPolicy = DEFAULT_POLICY,
  var mode: Mode? = DEFAULT_MODE,
  var caseSensitive: Boolean = DEFAULT_CASE_SENSITIVE,
  var symlinks: Boolean = DEFAULT_SYMLINKS,
  var root: String = DEFAULT_ROOT,
  var workingDirectory: String = DEFAULT_WORKING_DIRECTORY,
) : Toggleable {
  /** Enumerates supported operating modes for VM guest I/O. */
  @Suppress("unused") public enum class Mode {
    /** Virtualized I/O operations via a guest file-system. */
    GUEST,

    /** Access to resources from the host app classpath. */
    CLASSPATH,

    /** Access to host I/O (i.e. regular I/O on the host machine). */
    HOST,
  }

  internal companion object {
    /** Default enablement status. */
    const val DEFAULT_ENABLED: Boolean = true

    /** Default case-sensitivity status. */
    const val DEFAULT_CASE_SENSITIVE: Boolean = true

    /** Default symbolic links enablement status. */
    const val DEFAULT_SYMLINKS: Boolean = true

    /** Default root path. */
    const val DEFAULT_ROOT: String = AbstractBaseVFS.ROOT_SYSTEM_DEFAULT

    /** Default working directory path. */
    const val DEFAULT_WORKING_DIRECTORY: String = AbstractBaseVFS.DEFAULT_CWD

    /** Default policy to apply if none is specified. */
    val DEFAULT_POLICY = GuestVFSPolicy.DEFAULTS

    /** Default operating mode. */
    val DEFAULT_MODE: Mode = Mode.GUEST
  }
}

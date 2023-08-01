package elide.runtime.gvm.cfg

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable
import elide.runtime.gvm.internals.vfs.AbstractBaseVFS
import elide.runtime.gvm.internals.vfs.GuestVFSPolicy

/**
 * Configuration for the guest VM virtual file-system (VFS).
 */
@Suppress("MemberVisibilityCanBePrivate")
@ConfigurationProperties("elide.gvm.vfs")
internal interface GuestIOConfiguration : Toggleable {
  /** Enumerates supported operating modes for VM guest I/O. */
  @Suppress("unused")
  enum class Mode {
    /** Virtualized I/O operations via a guest file-system. */
    GUEST,

    /** Access to resources from the host app classpath. */
    CLASSPATH,

    /** Access to host I/O (i.e. regular I/O on the host machine). */
    HOST,
  }

  companion object {
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

  /**
   * @return Path to the VFS bundle to use for guest VMs, as applicable.
   */
  val bundle: String? get() = null

  /**
   * @return Security policies to apply to guest I/O operations. If none is provided, sensible defaults are used.
   */
  val policy: GuestVFSPolicy get() = DEFAULT_POLICY

  /**
   * @return Base operating mode for I/O - options are [Mode.GUEST] (virtualized), [Mode.CLASSPATH] (guest + access to
   *   host app classpath), or [Mode.HOST] (full access to host machine I/O).
   */
  val mode: Mode get() = DEFAULT_MODE

  /**
   * @return Whether to treat the file-system as case-sensitive. Defaults to `true`.
   */
  val caseSensitive: Boolean? get() = DEFAULT_CASE_SENSITIVE

  /**
   * @return Whether to enable (or expect) symbolic link support. Defaults to `true`.
   */
  val symlinks: Boolean? get() = DEFAULT_SYMLINKS

  /**
   * @return Root path for the file-system. Defaults to `/`.
   */
  val root: String? get() = DEFAULT_ROOT

  /**
   * @return Working directory to initialize the VFS with, as applicable. Defaults to `/`.
   */
  val workingDirectory: String? get() = DEFAULT_WORKING_DIRECTORY
}

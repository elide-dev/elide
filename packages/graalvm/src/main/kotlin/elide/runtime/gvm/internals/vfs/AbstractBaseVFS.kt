package elide.runtime.gvm.internals.vfs

import elide.runtime.Logger
import elide.runtime.gvm.internals.GuestVFS
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.*
import java.util.EnumSet
import kotlin.jvm.Throws

/**
 * # VFS: Guest Base
 *
 * This base class describes the expected API for guest VFS implementations, including their builders ([VFSBuilder]) and
 * factories ([VFSFactory]). Defaults are provided for the root system path and current-working-directory, both of which
 * default to `/`.
 *
 * Guest VFS implementations have their own subclass API, with which the abstract FS implementation can query for policy
 * status, file membership, and so on. See the methods [enforce] and [checkPolicy] for more information.
 *
 * @see GuestVFS for the public API of guest VFS implementations.
 * @see AbstractDelegateVFS for a concrete implementation of a guest VFS backed by a real file system.
 * @param VFS Concrete virtual file system type under implementation.
 * @param config Effective guest VFS configuration to apply.
 */
internal abstract class AbstractBaseVFS<VFS> protected constructor (
  internal val config: EffectiveGuestVFSConfig,
) : GuestVFS where VFS: AbstractBaseVFS<VFS> {
  internal companion object {
    /** Root system default value. */
    const val ROOT_SYSTEM_DEFAULT = "/"

    /** Default current-working-directory. */
    const val DEFAULT_CWD = "/"
  }

  /**
   * ## VFS: Builder
   *
   * Models the interface expected for VFS implementation builders, which should accept a uniform set of baseline
   * settings (such as [readOnly] status, or the [root] path). Specialized FS implementations may extend this interface
   * with additional settings.
   *
   * @param VFS Concrete virtual file system type under implementation.
   */
  interface VFSBuilder<VFS> where VFS: AbstractBaseVFS<VFS> {
    /**
     * ### Read-only status
     *
     * Whether the backing file-system should be considered read-only. In this mode, all writes are rejected without
     * consultation of the backing file-system (as applicable).
     *
     * This setting can be set as a property, or as a builder method.
     *
     * @see setReadOnly for the builder-method equivalent.
     */
    var readOnly: Boolean

    /**
     * ### Case-sensitivity
     *
     * Whether the backing file-system should be considered case-sensitive. In this mode, two paths with different
     * casing are considered two separate files.
     *
     * This setting can be set as a property, or as a builder method.
     *
     * @see setCaseSensitive for the builder-method equivalent.
     */
    var caseSensitive: Boolean

    /**
     * ### Symlink support
     *
     * Whether the backing file-system supports symbolic links, or is expected to support symbolic links. If this flag
     * is passed as `false`, then symbolic link read and write operations will be rejected without consulting the
     * backing file-system implementation (as applicable).
     *
     * This setting can be set as a property, or as a builder method.
     *
     * @see setEnableSymlinks for the builder-method equivalent.
     */
    var enableSymlinks: Boolean

    /**
     * ### Root path
     *
     * Specifies the root path for the file-system, which is typically set to `/`. On non-Unix and non-Linux systems,
     * this may be customized to, say, "C:/".
     *
     * This setting can be set as a property, or as a builder method.
     *
     * @see setRoot for the builder-method equivalent.
     */
    var root: String

    /**
     * ### Working directory
     *
     * Specifies the current-working-directory to apply when the file-system is initialized. This is typically set to
     * the root path (`/`).
     *
     * This setting can be set as a property, or as a builder method.
     *
     * @see setWorkingDirectory for the builder-method equivalent.
     */
    var workingDirectory: String

    /**
     * ### Guest I/O policy
     *
     * Specifies security policies, and allowed access rights, for a guest's use of this file-system. If a requested
     * guest I/O operation is not permitted by the attached [policy], it is rejected without consultation of the backing
     * file-system (as applicable).
     *
     * This setting can be set as a property, or as a builder method.
     *
     * @see setPolicy for the builder-method equivalent.
     */
    var policy: GuestVFSPolicy

    /**
     * Set the [readOnly] status of the file-system managed by this VFS implementation.
     *
     * @see readOnly to set this value as a property.
     * @param readOnly Whether the file-system should be considered read-only.
     * @return This builder.
     */
    fun setReadOnly(readOnly: Boolean): VFSBuilder<VFS> {
      this.readOnly = readOnly
      return this
    }

    /**
     * Set the [caseSensitive] status of the file-system managed by this VFS implementation.
     *
     * @see caseSensitive to set this value as a property.
     * @param caseSensitive Whether the file-system should be considered case-sensitive.
     * @return This builder.
     */
    fun setCaseSensitive(caseSensitive: Boolean): VFSBuilder<VFS> {
      this.caseSensitive = caseSensitive
      return this
    }

    /**
     * Set the [enableSymlinks] setting of the file-system managed by this VFS implementation.
     *
     * @see enableSymlinks to set this value as a property.
     * @param enableSymlinks Whether the file-system should expect symbolic link support.
     * @return This builder.
     */
    fun setEnableSymlinks(enableSymlinks: Boolean): VFSBuilder<VFS> {
      this.enableSymlinks = enableSymlinks
      return this
    }

    /**
     * Set the [root] path of the file-system managed by this VFS implementation.
     *
     * @see root to set this value as a property.
     * @param root Root file path to apply.
     * @return This builder.
     */
    fun setRoot(root: String): VFSBuilder<VFS> {
      this.root = root
      return this
    }

    /**
     * Set the initial [workingDirectory] path of the file-system managed by this VFS implementation.
     *
     * @see workingDirectory to set this value as a property.
     * @param workingDirectory Current-working-directory file path to apply.
     * @return This builder.
     */
    fun setWorkingDirectory(workingDirectory: String): VFSBuilder<VFS> {
      this.workingDirectory = workingDirectory
      return this
    }

    /**
     * Set the active [policy] for guest I/O operations.
     *
     * @see policy to set this value as a property.
     * @param policy Policy to apply.
     * @return This builder.
     */
    fun setPolicy(policy: GuestVFSPolicy): VFSBuilder<VFS> {
      this.policy = policy
      return this
    }

    /**
     * Collect all the current settings, apply defaults where needed, and construct the requested VFS implementation.
     *
     * @return Built VFS implementation.
     */
    fun build(): VFS
  }

  /**
   * ## VFS Builder: Static Interface
   *
   * Specifies the expected API surface of a VFS builder's static interface. This interface is used to construct new
   * builders, regardless of implementation (see [newBuilder]).
   *
   * @see newBuilder to create an empty VFS implementation builder, or to clone an existing builder.
   */
  interface VFSBuilderFactory<VFS, Builder> where VFS: AbstractBaseVFS<VFS>, Builder: VFSBuilder<VFS> {
    /**
     * Create a new VFS implementation builder, of type [VFS].
     *
     * @return Empty VFS implementation builder.
     */
    fun newBuilder(): Builder

    /**
     * Clone the provided [builder] to create a new builder.
     *
     * @return Cloned VFS implementation builder.
     */
    fun newBuilder(builder: Builder): Builder
  }

  /**
   * ## VFS: Factory
   *
   * Specifies the expected API surface of a VFS implementation's companion object, which should be equipped to create
   * and resolve VFS implementations using the [VFSBuilder] and [VFSBuilderFactory].
   */
  interface VFSFactory<VFS, Builder> where VFS: AbstractBaseVFS<VFS>, Builder: VFSBuilder<VFS> {
    /**
     * Create a [VFS] implementation with no backing data, and configured with defaults.
     *
     * @return Empty and default-configured [VFS] implementation.
     */
    fun create(): VFS

    /**
     * Create a [VFS] implementation configured with the provided [configurator].
     *
     * After [configurator] runs, the resulting builder is built and returned.
     *
     * @param configurator Function to execute, in the context of [Builder], which prepares the VFS implementation.
     * @return Build implementation of type [VFS].
     */
    fun create(configurator: Builder.() -> Unit): VFS

    /**
     * Create a [VFS] implementation configured with the provided effective [config].
     *
     * @param config Configuration to provide to the VFS implementation.
     * @return VFS implementation built with the provided [config].
     */
    fun create(config: EffectiveGuestVFSConfig): VFS

    /**
     * Create a [VFS] implementation from the provided [builder].
     *
     * @param builder Builder to create the VFS implementation from.
     * @return VFS implementation built with the provided [builder].
     */
    fun create(builder: VFSBuilder<VFS>): VFS
  }

  /**
   * Subclass API: Logging.
   *
   * @return [Logger] that should be used for debug messages emitted from this VFS implementation.
   */
  protected abstract fun logging(): Logger

  /**
   * Subclass API: Enforcement.
   *
   * This method is the protected entrypoint for enforcement of guest I/O policy. Calls to this method first check the
   * sanity of a given request (for example, writes can be refused outright if the file-system is read-only), and then
   * evaluate any attached policy to determine whether the request should be allowed.
   *
   * If the request is not allowed, an I/O exception is raised and the call is forbidden. If the request is allowed, it
   * is passed on to the backing implementation, as applicable.
   *
   * @param type Type(s) of access which are being requested.
   * @param domain Access domain for this request: is it coming from the guest, or the host?
   * @param path Path to the file or directory being accessed.
   * @param scope Whether this path is known to be a file, or directory, or it is not known.
   * @return Response from the policy check, indicating whether the request is allowed.
   */
  @Suppress("UNUSED_PARAMETER") protected fun enforce(
    type: EnumSet<AccessType>,
    domain: AccessDomain,
    path: Path,
    scope: AccessScope = AccessScope.UNSPECIFIED,
  ): AccessResponse = checkPolicy(
    type,
    domain,
    path,
    scope,
  )

  /**
   * Subclass API: Enforcement.
   *
   * This method is the protected entrypoint for enforcement of guest I/O policy. Calls to this method first check the
   * sanity of a given request (for example, writes can be refused outright if the file-system is read-only), and then
   * evaluate any attached policy to determine whether the request should be allowed.
   *
   * If the request is not allowed, an I/O exception is raised and the call is forbidden. If the request is allowed, it
   * is passed on to the backing implementation, as applicable.
   *
   * @see enforce to request multiple [AccessType]s in one go.
   * @param type Type of access which are being requested.
   * @param domain Access domain for this request: is it coming from the guest, or the host?
   * @param path Path to the file or directory being accessed.
   * @param scope Whether this path is known to be a file, or directory, or it is not known.
   * @return Response from the policy check, indicating whether the request is allowed.
   */
  protected fun enforce(
    type: AccessType,
    domain: AccessDomain,
    path: Path,
    scope: AccessScope = AccessScope.UNSPECIFIED,
  ): AccessResponse = enforce(
    EnumSet.of(type),
    domain,
    path,
    scope,
  )

  /**
   * Subclass API: Policy check.
   *
   * This method is defined by a given subclass implementation of the VFS system, and is charged with evaluating guest
   * I/O policy for a given request. This method is called by [enforce] when the request is deemed sane, and the policy
   * check is the only remaining step.
   *
   * @param type Type of access which are being requested.
   * @param domain Access domain for this request: is it coming from the guest, or the host?
   * @param path Path to the file or directory being accessed.
   * @param scope Whether this path is known to be a file, or directory, or it is not known.
   * @return Response from the policy check, indicating whether the request is allowed.
   */
  internal fun checkPolicy(
    type: AccessType,
    domain: AccessDomain,
    path: Path,
    scope: AccessScope = AccessScope.UNSPECIFIED,
  ): AccessResponse = checkPolicy(AccessRequest(
    type = setOf(type),
    domain = domain,
    scope = scope,
    path = path,
  ))

  /**
   * Subclass API: Policy check (multiple [type]s).
   *
   * This method is defined by a given subclass implementation of the VFS system, and is charged with evaluating guest
   * I/O policy for a given request. This method is called by [enforce] when the request is deemed sane, and the policy
   * check is the only remaining step.
   *
   * @param type Multiple types of access which are being requested.
   * @param domain Access domain for this request: is it coming from the guest, or the host?
   * @param path Path to the file or directory being accessed.
   * @param scope Whether this path is known to be a file, or directory, or it is not known.
   * @return Response from the policy check, indicating whether the request is allowed.
   */
  internal fun checkPolicy(
    type: Set<AccessType>,
    domain: AccessDomain,
    path: Path,
    scope: AccessScope = AccessScope.UNSPECIFIED,
  ): AccessResponse = checkPolicy(AccessRequest(
    type = type,
    domain = domain,
    scope = scope,
    path = path,
  ))

  /**
   * Subclass API: Policy check.
   *
   * This method is defined by a given subclass implementation of the VFS system, and is charged with evaluating guest
   * I/O policy for a given request. This method is called by [enforce] when the request is deemed sane, and the policy
   * check is the only remaining step.
   *
   * @param request Materialized I/O policy check request.
   * @return Response from the policy check, indicating whether the request is allowed.
   */
  protected abstract fun checkPolicy(request: AccessRequest): AccessResponse

  /**
   * Subclass API: Parse a path.
   *
   * This method is used to parse a series of path [segments] into a path which is compatible with the backing file
   * system. The file system in question is responsible for parsing the path and ensuring validity. Paths returned from
   * this method may be specialized to a given file-system implementation.
   *
   * @param segments Path segments to parse.
   * @return Parsed path.
   */
  internal abstract fun getPath(vararg segments: String): Path

  /**
   * Subclass API: Read a file.
   *
   * This method is used to read an arbitrary file from the embedded file-system, as a shortcut to be used outside the
   * regular guest VM context. In this case, the read is considered a "host read," which is not subject to guest I/O
   * policy restrictions.
   *
   * Host reads are still subject to any policy restrictions and isolation: for example, if an embedded VFS backed by a
   * file or jailed directory is used, the host reads are still scoped to these policies. To control host reads within
   * the scope of the VFS system, a sub-class may override this method or [checkPolicy].
   *
   * @param path Path of the file to read.
   * @param options Open options to use when reading the file.
   * @return Input stream for the file in question.
   * @throws IOException if the read operation cannot be completed.
   * @throws AccessDeniedException if the read operation cannot be completed because of a policy violation.
   */
  @Throws(IOException::class)
  internal abstract fun readStream(path: Path, vararg options: OpenOption): InputStream

  /**
   * Subclass API: Write a file.
   *
   * This method is used to write to an arbitrary file within the embedded file-system, as a shortcut to be used outside
   * the regular guest VM context. In this case, the write operation is considered a "host write," which is not subject
   * to guest I/O policy restrictions.
   *
   * Host writes are still subject to any policy restrictions and isolation: for example, if an embedded VFS backed by a
   * file or jailed directory is used, the host writes are still scoped to these policies. To control host writes within
   * the scope of the VFS system, a sub-class may override this method or [checkPolicy].
   *
   * @param path Path of the file we intend to write to.
   * @param options Open options to use when writing to the file.
   * @return Output stream for the file in question.
   * @throws IOException if the write operation cannot be completed.
   * @throws AccessDeniedException if the write operation cannot be completed because of a policy violation.
   */
  internal abstract fun writeStream(path: Path, vararg options: OpenOption): OutputStream
}

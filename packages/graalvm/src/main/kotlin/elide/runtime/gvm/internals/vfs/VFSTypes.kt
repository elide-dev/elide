package elide.runtime.gvm.internals.vfs

import java.nio.file.Path


// -- Basic: Files, Directories, Trees -- //

/**
 * TBD.
 */
internal typealias FileRecord = tools.elide.vfs.File

/**
 * TBD.
 */
internal typealias DirectoryRecord = tools.elide.vfs.Directory

/**
 * TBD.
 */
internal typealias FileTreeEntry = tools.elide.vfs.TreeEntry

/**
 * TBD.
 */
internal typealias FilesystemInfo = tools.elide.vfs.Filesystem


// -- Policy: Access Types, Domains, Scopes -- //

/** TBD. */
internal enum class AccessType {
  /** TBD. */
  READ,

  /** TBD. */
  WRITE,

  /** TBD. */
  DELETE,

  /** TBD. */
  EXECUTE,
}

/** TBD. */
internal enum class AccessDomain {
  /** TBD. */
  HOST,

  /** TBD. */
  GUEST,
}

/** TBD. */
internal enum class AccessScope {
  /** TBD. */
  UNSPECIFIED,

  /** TBD. */
  FILE,

  /** TBD. */
  DIRECTORY,
}

/** TBD. */
internal data class AccessRequest(
  /** TBD. */
  val type: Set<AccessType>,

  /** TBD. */
  val domain: AccessDomain,

  /** TBD. */
  val scope: AccessScope,

  /** TBD. */
  val path: Path,
) {
  /** Whether this operation constitutes a read, with no writes. */
  val isRead: Boolean get() = type.contains(AccessType.READ) && !isWrite

  /** Whether this operation constitutes an execution. */
  val isExecute: Boolean get() = type.contains(AccessType.EXECUTE)

  /** Whether this operation constitutes a write (or delete). */
  val isWrite: Boolean get() = type.contains(AccessType.WRITE) || type.contains(AccessType.DELETE)
}

/** TBD. */
internal enum class AccessResult {
  /** TBD. */
  ALLOW,

  /** TBD. */
  DENY,
}

/** TBD. */
internal class AccessResponse private constructor (
  /** TBD. */
  val policy: AccessResult,

  /** TBD. */
  val reason: String? = null,

  /** TBD. */
  val err: Throwable? = null,
) {
  internal companion object {
    private val ALLOW_DEFAULT: AccessResponse = AccessResponse(AccessResult.ALLOW)
    private val DENY_DEFAULT: AccessResponse = AccessResponse(AccessResult.DENY)

    /** TBD. */
    @JvmStatic fun allow(reason: String? = null): AccessResponse = if (reason.isNullOrBlank()) {
      ALLOW_DEFAULT
    } else {
      AccessResponse(AccessResult.ALLOW, reason)
    }

    /** TBD. */
    @JvmStatic fun deny(reason: String? = null): AccessResponse = if (reason.isNullOrBlank()) {
      DENY_DEFAULT
    } else {
      AccessResponse(AccessResult.DENY, reason)
    }

    /** TBD. */
    @JvmStatic fun unsupported(err: Throwable? = null, reason: String? = null): AccessResponse = AccessResponse(
      policy = AccessResult.DENY,
      reason = reason,
      err = err,
    )
  }
}

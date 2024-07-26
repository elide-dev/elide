package elide.runtime.intrinsics.js.node.childProcess

import elide.annotations.API

/**
 * ## Process Options (Identity-Enabled)
 *
 * Describes [ProcOptions], but enabled with POSIX identity attributes, such as [uid] (the user ID of an operation) and
 * [gid] (the group ID of an operation).
 *
 * @property uid User ID to run the process as.
 * @property gid Group ID to run the process as.
 */
@API public sealed interface IdentityProcOptions : ProcOptions {
  /** User ID to run a process as. */
  public val uid: Int?

  /** Group ID to run a process as. */
  public val gid: Int?
}

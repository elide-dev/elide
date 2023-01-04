package elide.runtime.gvm.internals.vfs

import java.io.File
import java.nio.file.Path

/**
 * # Guest IO Error: Access denied.
 *
 * This error is thrown when a guest I/O policy disallows a given I/O call at runtime. This error may also be thrown if
 * the backing guest file system is read-only, and the call represents a write operation.
 */
internal class GuestIOAccessDenied(
  internal val types: Set<AccessType>,
  file: File,
  message: String? = null,
  cause: Throwable? = null
) : GuestIOException(
  message = message ?: "Access denied.",
  cause = cause ?: AccessDeniedException(file)
) {
  internal companion object {
    /**
     * Spawn a [GuestIOAccessDenied] exception from the provided inputs.
     *
     * @param path Path that was denied for access.
     * @param types Type(s) of access that were requested, and denied.
     * @param message Message explaining the error in detail, if any.
     * @param cause Cause for the error, if any.
     * @return [GuestIOAccessDenied] exception, wrapping an [AccessDeniedException] (unless another cause was provided).
     */
    @JvmStatic fun forPath(path: Path, types: Set<AccessType>, message: String? = null, cause: Throwable? = null) =
      GuestIOAccessDenied(types, File(path.toString()), message, cause)
  }
}

package elide.runtime.gvm.internals

import org.graalvm.polyglot.io.FileSystem
import java.io.Closeable

/**
 * TBD.
 */
public interface GuestVFS : FileSystem, Closeable, AutoCloseable {
  /**
   * ## Guest VFS: Host file access.
   *
   * Indicates whether this VFS implementation allows access to host-side I/O for regular files on-disk. Unless I/O for
   * guest->host access is enabled, this is always `false`.
   *
   * @return Whether host file access is supported, or allowed, by this VFS implementation.
   */
  public fun allowsHostFileAccess(): Boolean

  /**
   * ## Guest VFS: Host socket access.
   *
   * Indicates whether this VFS implementation allows access to host-side I/O for native sockets. Unless I/O for
   * guest->host access is enabled, this is always `false`.
   *
   * @return Whether host socket access is supported, or allowed, by this VFS implementation.
   */
  public fun allowsHostSocketAccess(): Boolean
}

package elide.runtime.gvm.internals

import org.graalvm.polyglot.io.FileSystem
import java.io.Closeable

/**
 * TBD.
 */
internal interface GuestVFS :
  FileSystem,
  Closeable,
  AutoCloseable

package elide.runtime.gvm.internals.vfs

import elide.runtime.gvm.internals.GuestVFS
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.file.*
import java.nio.file.attribute.FileAttribute

/**
 * TBD.
 */
internal abstract class AbstractGuestVFS protected constructor (
  private val readOnly: Boolean = true,
  private val caseSensitive: Boolean = true,
  private val supportsSymbolicLinks: Boolean = false,
) : GuestVFS {
  /** @inheritDoc */
  override fun close() {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun parsePath(uri: URI): Path {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun parsePath(path: String): Path {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun checkAccess(path: Path, modes: MutableSet<out AccessMode>, vararg linkOptions: LinkOption) {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun createDirectory(dir: Path, vararg attrs: FileAttribute<*>) {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun delete(path: Path) {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun newByteChannel(
    path: Path,
    options: MutableSet<out OpenOption>,
    vararg attrs: FileAttribute<*>
  ): SeekableByteChannel {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun newDirectoryStream(dir: Path, filter: DirectoryStream.Filter<in Path>): DirectoryStream<Path> {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun toAbsolutePath(path: Path): Path {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun toRealPath(path: Path, vararg linkOptions: LinkOption): Path {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun readAttributes(path: Path, attributes: String, vararg options: LinkOption): MutableMap<String, Any> {
    TODO("Not yet implemented")
  }
}

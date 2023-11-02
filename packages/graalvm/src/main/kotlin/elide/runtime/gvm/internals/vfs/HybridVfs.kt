package elide.runtime.gvm.internals.vfs

import org.graalvm.polyglot.io.FileSystem
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.file.*
import java.nio.file.DirectoryStream.Filter
import java.nio.file.attribute.FileAttribute
import kotlin.io.path.pathString

/**
 * A hybrid [FileSystem] implementation using two layers: an in-memory [overlay], which takes priority for reads but
 * ignores writes, and a [backing] layer that can be written to, and which handles any requests not satisfied by the
 * overlay.
 *
 * Note that the current implementation is designed with a specific combination in mind: a JIMFS-backed embedded VFS
 * as the overlay (using [EmbeddedGuestVFSImpl]), and a host-backed VFS as the base layer (using [HostVFSImpl]).
 *
 * Instances of this class can be acquired using [HybridVfs.acquire].
 */
internal class HybridVfs private constructor(
  private val backing: FileSystem,
  private val overlay: FileSystem,
) : FileSystem {
  /**
   * Convert this path to one that can be used by the [overlay] vfs.
   *
   * Because of incompatible types used by the underlying JIMFS and the platform-default file system, paths created
   * using, for example, [Path.of], will not be recognized properly, and they must be transformed before use.
   */
  private fun Path.forEmbedded(): Path {
    return overlay.parsePath(pathString)
  }

  override fun parsePath(uri: URI?): Path {
    return backing.parsePath(uri)
  }

  override fun parsePath(path: String?): Path {
    return backing.parsePath(path)
  }

  override fun toAbsolutePath(path: Path?): Path {
    return backing.toAbsolutePath(path)
  }

  override fun toRealPath(path: Path?, vararg linkOptions: LinkOption?): Path {
    return backing.toRealPath(path, *linkOptions)
  }

  override fun createDirectory(dir: Path?, vararg attrs: FileAttribute<*>?) {
    return backing.createDirectory(dir, *attrs)
  }

  override fun delete(path: Path?) {
    backing.delete(path)
  }

  override fun checkAccess(path: Path, modes: MutableSet<out AccessMode>, vararg linkOptions: LinkOption) {
    // if only READ is requested, try the in-memory vfs first
    if (modes.size == 0 || (modes.size == 1 && modes.contains(AccessMode.READ))) runCatching {
      // ensure the path is compatible with the embedded vfs before passing it
      overlay.checkAccess(path.forEmbedded(), modes, *linkOptions)
      return
    }

    // if WRITE or EXECUTE were requested, or if the in-memory vfs denied access,
    // try using the host instead
    backing.checkAccess(path, modes, *linkOptions)
  }

  override fun newByteChannel(
    path: Path,
    options: MutableSet<out OpenOption>,
    vararg attrs: FileAttribute<*>,
  ): SeekableByteChannel {
    // if only READ is requested, try the in-memory vfs first
    if (options.size == 0 || (options.size == 1 && options.contains(StandardOpenOption.READ))) runCatching {
      // ensure the path is compatible with the embedded vfs before passing it
      return overlay.newByteChannel(path.forEmbedded(), options, *attrs)
    }

    // if write-related options were set, or the in-memory vfs failed to open the file
    // (e.g. because it doesn't exist in the bundle), try using the host instead
    return backing.newByteChannel(path, options, *attrs)
  }

  override fun newDirectoryStream(dir: Path, filter: Filter<in Path>?): DirectoryStream<Path> {
    // try the in-memory vfs first
    runCatching {
      // ensure the path is compatible with the embedded vfs before passing it
      return overlay.newDirectoryStream(dir.forEmbedded(), filter)
    }

    // if the in-memory vfs failed to open the directory, try using the host instead
    return backing.newDirectoryStream(dir, filter)
  }

  override fun readAttributes(path: Path, attributes: String?, vararg options: LinkOption): MutableMap<String, Any> {
    // try the in-memory vfs first
    runCatching {
      // ensure the path is compatible with the embedded vfs before passing it
      return overlay.readAttributes(path.forEmbedded(), attributes, *options)
    }

    // if the in-memory vfs failed to read the file attributes, try using the host instead
    return backing.readAttributes(path, attributes, *options)
  }

  companion object {
    /**
     * Configures a new [HybridVfs] using an in-memory VFS containing the provided [overlay] as [overlay], and the
     * host file system as [backing] layer.
     *
     * @param overlay A list of bundles to be unpacked into the in-memory fs.
     * @param writable Whether to allow writes to the backing layer.
     * @return A new [HybridVfs] instance.
     */
    fun acquire(writable: Boolean, overlay: List<URI>): HybridVfs {
      // configure an in-memory vfs with the provided bundles as overlay
      val inMemory = EmbeddedGuestVFSImpl.Builder.newBuilder()
        .setBundlePaths(overlay)
        .setReadOnly(false)
        .build()

      // use the host fs as backing layer
      val host = HostVFSImpl.Builder.newBuilder()
        .setReadOnly(!writable)
        .build()

      return HybridVfs(
        backing = host,
        overlay = inMemory,
      )
    }
  }
}
package elide.runtime.plugins.vfs.internal

import org.graalvm.polyglot.io.FileSystem
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.file.*
import java.nio.file.DirectoryStream.Filter
import java.nio.file.attribute.FileAttribute
import kotlin.io.path.pathString

internal class HybridVfs(
  private val host: FileSystem,
  private val inMemory: FileSystem,
) : FileSystem {
  private fun Path.forEmbedded(): Path {
    return inMemory.parsePath(pathString)
  }

  override fun parsePath(uri: URI?): Path {
    return host.parsePath(uri)
  }

  override fun parsePath(path: String?): Path {
    return host.parsePath(path)
  }

  override fun toAbsolutePath(path: Path?): Path {
    return host.toAbsolutePath(path)
  }

  override fun toRealPath(path: Path?, vararg linkOptions: LinkOption?): Path {
    return host.toRealPath(path, *linkOptions)
  }

  override fun createDirectory(dir: Path?, vararg attrs: FileAttribute<*>?) {
    // write operations are handled directly by the host fs
    return host.createDirectory(dir, *attrs)
  }

  override fun delete(path: Path?) {
    // write operations are handled directly by the host fs
    host.delete(path)
  }

  override fun checkAccess(path: Path, modes: MutableSet<out AccessMode>, vararg linkOptions: LinkOption) {
    // if only READ is requested, try the in-memory vfs first
    if (modes.size == 0 || (modes.size == 1 && modes.contains(AccessMode.READ))) runCatching {
      // ensure the path is compatible with the embedded vfs before passing it
      inMemory.checkAccess(path.forEmbedded(), modes, *linkOptions)
      return
    }

    // if WRITE or EXECUTE were requested, or if the in-memory vfs denied access,
    // try using the host instead
    host.checkAccess(path, modes, *linkOptions)
  }

  override fun newByteChannel(
    path: Path,
    options: MutableSet<out OpenOption>,
    vararg attrs: FileAttribute<*>,
  ): SeekableByteChannel {
    // if only READ is requested, try the in-memory vfs first
    if (options.size == 0 || (options.size == 1 && options.contains(StandardOpenOption.READ))) runCatching {
      // ensure the path is compatible with the embedded vfs before passing it
      return inMemory.newByteChannel(path.forEmbedded(), options, *attrs)
    }

    // if write-related options were set, or the in-memory vfs failed to open the file
    // (e.g. because it doesn't exist in the bundle), try using the host instead
    return host.newByteChannel(path, options, *attrs)
  }

  override fun newDirectoryStream(dir: Path, filter: Filter<in Path>?): DirectoryStream<Path> {
    // try the in-memory vfs first
    runCatching {
      // ensure the path is compatible with the embedded vfs before passing it
      return inMemory.newDirectoryStream(dir.forEmbedded(), filter)
    }

    // if the in-memory vfs failed to open the directory, try using the host instead
    return host.newDirectoryStream(dir, filter)
  }

  override fun readAttributes(path: Path, attributes: String?, vararg options: LinkOption): MutableMap<String, Any> {
    // try the in-memory vfs first
    runCatching {
      // ensure the path is compatible with the embedded vfs before passing it
      return inMemory.readAttributes(path.forEmbedded(), attributes, *options)
    }

    // if the in-memory vfs failed to read the file attributes, try using the host instead
    return host.readAttributes(path, attributes, *options)
  }
}
package elide.runtime.gvm.internals.vfs

import elide.runtime.LogLevel
import elide.runtime.gvm.internals.GuestVFS
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.attribute.FileAttribute
import java.util.EnumSet

/**
 * # VFS: Backed Implementation
 *
 * This class implements a virtual-file-system for guest use, backed by another "[backing]" file-system, to which calls
 * are proxied from the guest. Before proxying each call, I/O security policy is checked and enforced, and logging is
 * performed.
 *
 * The provided [config] is expected to be fully loaded; that is, it should have a loaded filesystem tree and data-bag
 * from which to pull file system information. The [backing] file system used by this class is closed when this class
 * is closed.
 *
 * @param VFS Concrete virtual file system type under implementation.
 * @param config Effective guest VFS configuration to apply.
 * @param backing Backing file-system instance which implements the FS to use.
 */
internal abstract class AbstractBackedGuestVFS<VFS> protected constructor (
  protected val config: EffectiveGuestVFSConfig,
  private val backing: FileSystem,
) : GuestVFS, AbstractGuestVFS<VFS>(config) where VFS: AbstractGuestVFS<VFS> {
  internal companion object {
    /** Translate an [AccessMode] to an [AccessType]. */
    fun AccessMode.toAccessType(): AccessType = when (this) {
      AccessMode.READ -> AccessType.READ
      AccessMode.WRITE -> AccessType.WRITE
      AccessMode.EXECUTE -> AccessType.EXECUTE
    }
  }

  // Debug log messages for the current VFS implementation.
  private fun debugLog(message: () -> String) {
    val logger = logging()
    if (logger.isEnabled(LogLevel.DEBUG)) {
      logger.debug("VFS: ${message()}")
    }
  }

  /** @inheritDoc */
  override fun close() = backing.close()

  /** @inheritDoc */
  override fun getSeparator(): String = backing.separator

  /** @inheritDoc */
  override fun getPathSeparator(): String = backing.separator

  /** @inheritDoc */
  override fun parsePath(uri: URI): Path = Path.of(uri)

  /** @inheritDoc */
  override fun parsePath(path: String): Path = Path.of(path)

  /** @inheritDoc */
  override fun toAbsolutePath(path: Path): Path = path.toAbsolutePath()

  /** @inheritDoc */
  override fun toRealPath(path: Path, vararg linkOptions: LinkOption): Path {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun checkAccess(path: Path, modes: MutableSet<out AccessMode>, vararg linkOptions: LinkOption) {
    debugLog {
      "Checking access to path: $path, modes: $modes, linkOptions: $linkOptions"
    }
    enforce(
      path = path,
      type = EnumSet.copyOf(modes.map { it.toAccessType() }),
      domain = AccessDomain.GUEST,
    )
  }

  /** @inheritDoc */
  override fun createDirectory(dir: Path, vararg attrs: FileAttribute<*>) {
    debugLog {
      "Creating directory at path: '$dir'"
    }
    enforce(
      type = AccessType.WRITE,
      domain = AccessDomain.GUEST,
      scope = AccessScope.DIRECTORY,
      path = dir,
    )
    backing.provider().createDirectory(
      dir,
      *attrs
    )
  }

  /** @inheritDoc */
  override fun newByteChannel(
    path: Path,
    options: MutableSet<out OpenOption>,
    vararg attrs: FileAttribute<*>
  ): SeekableByteChannel {
    debugLog {
      "Opening byte channel for file at path: '$path'"
    }
    enforce(
      type = AccessType.READ,
      domain = AccessDomain.GUEST,
      scope = AccessScope.FILE,
      path = path,
    )
    return backing.provider().newByteChannel(
      path,
      options,
      *attrs
    )
  }

  /** @inheritDoc */
  override fun newDirectoryStream(dir: Path, filter: DirectoryStream.Filter<in Path>): DirectoryStream<Path> {
    debugLog {
      "Streaming directory entries at path: '$dir'"
    }
    enforce(
      type = AccessType.READ,
      domain = AccessDomain.GUEST,
      scope = AccessScope.DIRECTORY,
      path = dir,
    )
    return backing.provider().newDirectoryStream(
      dir,
      filter,
    )
  }

  /** @inheritDoc */
  override fun readAttributes(path: Path, attributes: String, vararg options: LinkOption): MutableMap<String, Any> {
    debugLog {
      "Reading attributes for file at path: '$path'"
    }
    enforce(
      type = AccessType.READ,
      domain = AccessDomain.GUEST,
      path = path,
    )
    return backing.provider().readAttributes(
      path,
      attributes,
      *options
    )
  }

  /** @inheritDoc */
  override fun setAttribute(path: Path, attribute: String, value: Any, vararg options: LinkOption) {
    debugLog {
      "Setting attribute '$attribute' for file at path: '$path' (value: '$value', options: '$options')"
    }
    enforce(
      type = AccessType.WRITE,
      domain = AccessDomain.GUEST,
      path = path,
    )
    return backing.provider().setAttribute(
      path,
      attribute,
      value,
      *options
    )
  }

  /** @inheritDoc */
  override fun copy(source: Path, target: Path, vararg options: CopyOption?) {
    debugLog {
      "Copying from '$source' -> '$target' (options: $options)"
    }
    enforce(
      type = AccessType.READ,
      domain = AccessDomain.GUEST,
      path = source,
    )
    enforce(
      type = AccessType.WRITE,
      domain = AccessDomain.GUEST,
      path = target,
    )
    backing.provider().copy(
      source,
      target,
      *options
    )
  }

  /** @inheritDoc */
  override fun move(source: Path, target: Path, vararg options: CopyOption?) {
    debugLog {
      "Moving from '$source' -> '$target' (options: $options)"
    }
    enforce(
      type = EnumSet.of(AccessType.READ, AccessType.DELETE),
      domain = AccessDomain.GUEST,
      path = source,
    )
    enforce(
      type = AccessType.WRITE,
      domain = AccessDomain.GUEST,
      path = target,
    )
    backing.provider().move(
      source,
      target,
      *options
    )
  }

  /** @inheritDoc */
  override fun delete(path: Path) {
    debugLog {
      "Deleting filesystem entry at path: '$path'"
    }
    enforce(
      type = AccessType.DELETE,
      domain = AccessDomain.GUEST,
      path = path,
    )
    backing.provider().delete(path)
  }

  /** @inheritDoc */
  override fun createLink(link: Path, existing: Path) {
    debugLog {
      "Creating hard-link from '$link' -> '$existing'"
    }
    enforce(
      type = AccessType.WRITE,
      domain = AccessDomain.GUEST,
      path = link,
    )
    return backing.provider().createLink(
      link,
      existing,
    )
  }

  /** @inheritDoc */
  override fun createSymbolicLink(link: Path, target: Path, vararg attrs: FileAttribute<*>?) {
    debugLog {
      "Creating soft-link from '$link' -> '$target'"
    }
    enforce(
      type = AccessType.WRITE,
      domain = AccessDomain.GUEST,
      path = link,
    )
    return backing.provider().createLink(
      link,
      target,
    )
  }

  /** @inheritDoc */
  override fun readSymbolicLink(link: Path): Path {
    debugLog {
      "Reading soft-link at '$link'"
    }
    enforce(
      type = AccessType.READ,
      domain = AccessDomain.GUEST,
      path = link,
    )
    return backing.provider().readSymbolicLink(link)
  }

  /** @inheritDoc */
  override fun setCurrentWorkingDirectory(currentWorkingDirectory: Path) {
    debugLog {
      "Setting CWD to: '$currentWorkingDirectory'"
    }
    TODO("not yet implemented")
  }

  /** @inheritDoc */
  override fun getMimeType(path: Path?): String {
    TODO("not yet implemented")
  }

  /** @inheritDoc */
  override fun getEncoding(path: Path): Charset {
    debugLog {
      "Fetching encoding for path: '$path'"
    }
    return StandardCharsets.UTF_8  // TODO(sgammon): make this configurable or resolve from tree
  }

  /** @inheritDoc */
  override fun getTempDirectory(): Path {
    debugLog {
      "Fetching temp directory path"
    }
    TODO("not yet implemented")
  }

  /** @inheritDoc */
  override fun isSameFile(path1: Path, path2: Path, vararg options: LinkOption): Boolean {
    debugLog {
      "Checking if '$path1' and '$path2' are the same file"
    }
    enforce(
      type = AccessType.READ,
      domain = AccessDomain.GUEST,
      path = path1,
    )
    enforce(
      type = AccessType.READ,
      domain = AccessDomain.GUEST,
      path = path2,
    )
    // @TODO(sgammon): what to do about `options` here?
    return backing.provider().isSameFile(
      path1,
      path2,
    )
  }
}

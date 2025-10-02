/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.gvm.internals.vfs

import org.graalvm.polyglot.io.FileSystem
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.file.*
import java.nio.file.DirectoryStream.Filter
import java.nio.file.attribute.FileAttribute
import kotlin.io.path.pathString
import elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl.Settings
import elide.runtime.vfs.GuestVFS

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
) : GuestVFS {
  /**
   * Convert this path to one that can be used by the [overlay] vfs.
   *
   * Because of incompatible types used by the underlying JIMFS and the platform-default file system, paths created
   * using, for example, [Path.of], will not be recognized properly, and they must be transformed before use.
   */
  private fun Path.forEmbedded(): Path {
    return overlay.parsePath(pathString)
  }

  override val writable: Boolean get() = true
  override val deletable: Boolean get() = true
  override val virtual: Boolean get() = true
  override val host: Boolean get() = true
  override val compound: Boolean get() = true
  override val supportsSymlinks: Boolean get() = true

  override fun allowsHostFileAccess(): Boolean = true
  override fun allowsHostSocketAccess(): Boolean = false

  override fun getTempDirectory(): Path? {
    return backing.tempDirectory
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

  override fun close() {
    // noop
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
    fun acquire(writable: Boolean, overlay: List<URI>, deferred: Boolean = Settings.DEFAULT_DEFERRED_READS): HybridVfs {
      // configure an in-memory vfs with the provided bundles as overlay
      val inMemory = EmbeddedGuestVFSImpl.Builder.newBuilder()
        .setBundlePaths(overlay)
        .setReadOnly(true)
        .setDeferred(deferred)
        .build()

      // suppress nfe for the inner class; it needs to fall-back
      inMemory.suppressNotFoundErr()

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

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
package elide.runtime.gvm.vfs

import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.file.*
import java.nio.file.DirectoryStream.Filter
import java.nio.file.attribute.FileAttribute
import elide.runtime.gvm.internals.GuestVFS

/**
 * # Virtual File Systems: Overlays
 *
 * This module provides utilities for creating simple overlay file systems from regular Java NIO file systems; VFS
 * instances created by this factory can then be used to overlay a host file system with additional content.
 *
 * &nbsp;
 *
 * ## Usage: Zip Overlays
 *
 * Zip overlays use the JDK's built-in `zipfs` provider to create a new file system from a zip file. This is useful for
 * creating a read-only overlay on top of an existing file system, or for creating a temporary file system from a zip
 * archive. Elide uses overlay file systems internally to load internal resources.
 */
public object OverlayVFS {
  /**
   * Zip Overlay
   *
   * Create a "zip overlay" file system using the archive at the provided [path]; this file system will be read-only.
   *
   * @param path Path to the zip archive.
   * @return New file system instance.
   */
  public fun zipOverlay(path: Path): GuestVFS = FileSystems.newFileSystem(
    URI.create("jar:file:$path"),
    emptyMap<String, Any?>()
  ).let { vfs ->
    object : GuestVFS {
      override val writable: Boolean get() = false
      override val deletable: Boolean get() = false
      override val virtual: Boolean get() = true
      override val host: Boolean get() = false
      override val compound: Boolean get() = false
      override val supportsSymlinks: Boolean get() = false

      override fun allowsHostFileAccess(): Boolean = false
      override fun allowsHostSocketAccess(): Boolean = false

      override fun parsePath(uri: URI): Path = vfs.provider().getPath(uri)
      override fun parsePath(path: String): Path = vfs.getPath(path)
      override fun toAbsolutePath(path: Path): Path = path
      override fun toRealPath(path: Path, vararg linkOptions: LinkOption?): Path = path

      override fun checkAccess(path: Path?, modes: MutableSet<out AccessMode>?, vararg linkOptions: LinkOption?) {
        // no-op
      }

      override fun createDirectory(dir: Path?, vararg attrs: FileAttribute<*>?) =
        throw UnsupportedOperationException("Zip overlay is read-only")

      override fun delete(path: Path?) =
        throw UnsupportedOperationException("Zip overlay is read-only")

      override fun newByteChannel(
        path: Path,
        options: MutableSet<out OpenOption>,
        vararg attrs: FileAttribute<*>
      ): SeekableByteChannel = vfs.provider().newByteChannel(
        path,
        options,
        *attrs,
      )

      override fun newDirectoryStream(dir: Path, filter: Filter<in Path>): DirectoryStream<Path> =
        vfs.provider().newDirectoryStream(dir, filter)

      override fun readAttributes(
        path: Path,
        attributes: String,
        vararg options: LinkOption
      ): MutableMap<String, Any> = vfs.provider().readAttributes(
        path,
        attributes,
        *options,
      )

      override fun close() {
        vfs.close()
      }
    }
  }
}

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

import org.apache.commons.compress.utils.BoundedSeekableByteChannelInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.charset.Charset
import java.nio.file.*
import java.nio.file.DirectoryStream.Filter
import java.nio.file.attribute.FileAttribute
import elide.runtime.Logger
import elide.runtime.vfs.GuestVFS
import elide.runtime.vfs.LanguageVFS

/**
 * TBD.
 */
internal class CompoundVFSImpl private constructor (
  config: EffectiveGuestVFSConfig,
  private val primary: AbstractDelegateVFS<*>,
  private val overlays: Array<GuestVFS>,
  private val hostAllowed: Boolean = false,
  private val hostSocketsAllowed: Boolean = false,
) : AbstractBaseVFS<CompoundVFSImpl>(config) {
  companion object {
    // Create a compound VFS implementation.
    @JvmStatic fun create(
      primary: AbstractDelegateVFS<*>,
      overlays: List<GuestVFS>,
      hostAllowed: Boolean,
      hostSocketsAllowed: Boolean
    ): CompoundVFSImpl = CompoundVFSImpl(
      config = primary.config,
      primary = primary,
      overlays = overlays.toTypedArray(),
      hostAllowed = hostAllowed,
      hostSocketsAllowed = hostSocketsAllowed
    )
  }

  private val inOrderCandidates: Collection<GuestVFS> by lazy {
    sequence {
      yield(primary)
      overlays.forEach { yield(it) }
    }.filter {
      it !is LanguageVFS
    }
    .toCollection(
      ArrayList(overlays.size + 1)
    )
  }

  private val languageVfsOverlays: Collection<LanguageVFS> by lazy {
    overlays.filterIsInstance<LanguageVFS>()
  }

  override fun allowsHostFileAccess(): Boolean = hostAllowed || overlays.any {
    it.allowsHostFileAccess()
  }

  override fun allowsHostSocketAccess(): Boolean = hostSocketsAllowed || overlays.any {
    it.allowsHostSocketAccess()
  }

  override val logging: Logger by lazy {
    primary.logging
  }

  private inline fun <reified R> delegatePrimary(action: (AbstractDelegateVFS<*>) -> R): R {
    primary.apply {
      return action.invoke(this)
    }
  }

  @Suppress("SwallowedException")
  private inline fun <reified R> firstWritable(path: Path? = null, action: (GuestVFS) -> R): R? {
    return inOrderCandidates.filter {
      it.writable && (path == null || Files.isWritable(it.parsePath(path.toString())))
    }.firstNotNullOfOrNull {
      try {
        action.invoke(it)
      } catch (ioe: IOException) {
        null
      }
    }
  }

  @Suppress("SwallowedException")
  private inline fun <reified R> firstWithPath(path: Path, action: (GuestVFS, Path) -> R): R? {
    val str = path.toString()
    val languageVfsHandler = languageVfsOverlays.firstOrNull { it.accepts(path) }

    // language resources should pre-empty
    if (languageVfsHandler != null) {
      return action.invoke(languageVfsHandler, path)
    }
    return inOrderCandidates.firstNotNullOfOrNull {
      val candidate = try {
        (it to it.parsePath(str))
      } catch (ioe: IllegalArgumentException) {
        return@firstNotNullOfOrNull null
      } catch (uoe: UnsupportedOperationException) {
        return@firstNotNullOfOrNull null
      }
      if (it.existsAny(candidate.second)) {
        candidate
      } else {
        null
      }
    }?.let { (target, path) ->
      action.invoke(target, path)
    }
  }

  private inline fun <reified R> firstForRead(path: Path, action: (GuestVFS, Path) -> R): R? {
    return firstWithPath(path) { vfs, p ->
      action.invoke(vfs, p)
    }
  }

  private inline fun <reified R> firstForWrite(path: Path, action: (GuestVFS) -> R): R? {
    return firstWritable(path) {
      action.invoke(it)
    }
  }

  @Suppress("SwallowedException")
  override fun close() {
    inOrderCandidates.forEach {
      try {
        it.close()
      } catch (ioe: IOException) {
        // swallow
      }
    }
  }

  override val compound: Boolean get() = true
  override val writable: Boolean get() = inOrderCandidates.any { it.writable }
  override val deletable: Boolean get() = inOrderCandidates.any { it.deletable }
  override val host: Boolean get() = inOrderCandidates.any { it.host }
  override val virtual: Boolean get() = inOrderCandidates.any { it.virtual }
  override val supportsSymlinks: Boolean get() = inOrderCandidates.any { it.supportsSymlinks }

  override fun getSeparator(): String = delegatePrimary { it.separator }
  override fun getPathSeparator(): String = delegatePrimary { it.pathSeparator }
  override fun parsePath(uri: URI): Path = delegatePrimary { it.parsePath(uri) }
  override fun parsePath(path: String): Path = delegatePrimary { it.parsePath(path) }
  override fun getPath(vararg segments: String): Path = delegatePrimary { it.getPath(*segments) }

  override fun toAbsolutePath(path: Path): Path = firstWithPath(path) { vfs, target ->
    vfs.toAbsolutePath(target)
  } ?: path

  override fun toRealPath(path: Path, vararg linkOptions: LinkOption): Path =
    delegatePrimary { it.toRealPath(path, *linkOptions) }

  override fun checkAccess(path: Path, modes: MutableSet<out AccessMode>, vararg linkOptions: LinkOption) {
    var preserved: GuestIOAccessDenied? = null
    inOrderCandidates.forEach {
      try {
        it.checkAccess(path, modes, *linkOptions)
      } catch (accessDenied: GuestIOAccessDenied) {
        if (preserved == null) {
          preserved = accessDenied
        }
        // continue
      }
    }
    if (preserved != null) {
      throw requireNotNull(preserved)
    }
  }

  override fun createDirectory(dir: Path, vararg attrs: FileAttribute<*>) {
    firstForWrite(dir) {
      it.createDirectory(dir, *attrs)
    }
  }

  override fun newByteChannel(
    path: Path,
    options: MutableSet<out OpenOption>,
    vararg attrs: FileAttribute<*>
  ): SeekableByteChannel {
    val languageMatch = languageVfsOverlays.firstNotNullOfOrNull {
      if (!it.accepts(path)) null else it
    }
    if (languageMatch != null) {
      return languageMatch.newByteChannel(
        path,
        options,
        *attrs,
      )
    }

    return firstForRead(path) { vfs, target ->
      vfs.newByteChannel(target, options, *attrs)
    } ?: throw NoSuchFileException(path.toString())
  }

  override fun newDirectoryStream(dir: Path, filter: Filter<in Path>): DirectoryStream<Path> {
    return firstForRead(dir) { vfs, target ->
      vfs.newDirectoryStream(target, filter)
    } ?: throw NoSuchFileException(dir.toString())
  }

  override fun readAttributes(path: Path, attributes: String, vararg options: LinkOption): MutableMap<String, Any> {
    return firstForRead(path) { vfs, target ->
      vfs.readAttributes(target, attributes, *options)
    } ?: throw NoSuchFileException(path.toString())
  }

  override fun setAttribute(path: Path, attribute: String, value: Any, vararg options: LinkOption) {
    firstForWrite(path) {
      it.setAttribute(path, attribute, value, *options)
    }
  }

  override fun copy(source: Path, target: Path, vararg options: CopyOption?) {
    firstForRead(source) { _, srcpath ->
      val src = this
      firstForWrite(target) {
        require(src === this) {
          "Source and target must be from the same VFS"
        }
        it.copy(srcpath, target, *options)
      }
    }
  }

  override fun move(source: Path, target: Path, vararg options: CopyOption?) {
    firstForRead(source) { _, srcpath ->
      val src = this
      firstForWrite(target) {
        require(src === this) {
          "Source and target must be from the same VFS"
        }
        it.move(srcpath, target, *options)
      }
    }
  }

  override fun delete(path: Path) {
    firstForWrite(path) {
      it.delete(path)
    }
  }

  override fun createLink(link: Path, existing: Path) {
    firstForWrite(link) {
      it.createLink(link, existing)
    }
  }

  override fun createSymbolicLink(link: Path, target: Path, vararg attrs: FileAttribute<*>?) {
    firstForWrite(link) {
      it.createSymbolicLink(link, target, *attrs)
    }
  }

  override fun readSymbolicLink(link: Path): Path {
    return firstForRead(link) { vfs, target ->
      vfs.readSymbolicLink(target)
    } ?: throw NoSuchFileException(link.toString())
  }

  override fun setCurrentWorkingDirectory(currentWorkingDirectory: Path) {
    firstForWrite(currentWorkingDirectory) {
      it.setCurrentWorkingDirectory(currentWorkingDirectory)
    }
  }

  override fun getMimeType(path: Path): String? {
    return delegatePrimary {
      it.getMimeType(path)
    }
  }

  override fun getEncoding(path: Path): Charset {
    return delegatePrimary { it.getEncoding(path) }
  }

  override fun getTempDirectory(): Path =
    firstWritable { tempDirectory  } ?: throw IOException("No writable VFS found")

  override fun isSameFile(path1: Path, path2: Path, vararg options: LinkOption): Boolean {
    return firstForRead(path1) { vfs, _ ->
      vfs.isSameFile(path1, path2, *options)
    } ?: throw NoSuchFileException(path1.toString())
  }

  override fun readStream(path: Path, vararg options: OpenOption): InputStream {
    return firstForRead(path) { vfs, target ->
      vfs.newByteChannel(target, mutableSetOf(*options)).let {
        BoundedSeekableByteChannelInputStream(it.position(), it.size(), it)
      }
    } ?: throw NoSuchFileException(path.toString())
  }

  override fun writeStream(path: Path, vararg options: OpenOption): OutputStream {
    return delegatePrimary {
      it.writeStream(path, *options)
    }
  }

  override fun checkPolicy(request: AccessRequest): AccessResponse {
    return delegatePrimary { it.checkPolicy(request) }
  }
}

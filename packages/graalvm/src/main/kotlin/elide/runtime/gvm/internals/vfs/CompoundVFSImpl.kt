/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.charset.Charset
import java.nio.file.*
import java.nio.file.DirectoryStream.Filter
import java.nio.file.attribute.FileAttribute
import elide.runtime.Logger

/**
 * TBD.
 */
internal class CompoundVFSImpl (
  config: EffectiveGuestVFSConfig,
  private val primary: AbstractDelegateVFS<*>,
  private val overlays: Array<AbstractDelegateVFS<*>>,
  private val hostAllowed: Boolean = false,
  private val hostSocketsAllowed: Boolean = false,
) : AbstractBaseVFS<CompoundVFSImpl>(config) {
  override fun allowsHostFileAccess(): Boolean = hostAllowed || overlays.any {
    it.allowsHostFileAccess()
  }

  override fun allowsHostSocketAccess(): Boolean = hostSocketsAllowed || overlays.any {
    it.allowsHostSocketAccess()
  }

  override fun logging(): Logger = primary.logging()

  override fun close() {
    TODO("")
  }

  override fun getSeparator(): String {
    TODO("")
  }

  override fun getPathSeparator(): String {
    TODO("")
  }

  override fun parsePath(uri: URI): Path {
    TODO("")
  }

  override fun parsePath(path: String): Path {
    TODO("")
  }

  override fun getPath(vararg segments: String): Path {
    TODO("")
  }

  override fun toAbsolutePath(path: Path): Path {
    TODO("")
  }

  override fun toRealPath(path: Path, vararg linkOptions: LinkOption): Path {
    TODO("")
  }

  override fun checkAccess(path: Path, modes: MutableSet<out AccessMode>, vararg linkOptions: LinkOption) {
    TODO("")
  }

  override fun createDirectory(dir: Path, vararg attrs: FileAttribute<*>) {
    TODO("")
  }

  override fun newByteChannel(
    path: Path,
    options: MutableSet<out OpenOption>,
    vararg attrs: FileAttribute<*>
  ): SeekableByteChannel {
    TODO("")
  }

  override fun newDirectoryStream(dir: Path, filter: Filter<in Path>): DirectoryStream<Path> {
    TODO("")
  }

  override fun readAttributes(path: Path, attributes: String, vararg options: LinkOption): MutableMap<String, Any> {
    TODO("")
  }

  override fun setAttribute(path: Path, attribute: String, value: Any, vararg options: LinkOption) {
    super.setAttribute(path, attribute, value, *options)
  }

  override fun copy(source: Path, target: Path, vararg options: CopyOption?) {
    super.copy(source, target, *options)
  }

  override fun move(source: Path, target: Path, vararg options: CopyOption?) {
    super.move(source, target, *options)
  }

  override fun delete(path: Path) {
    TODO("")
  }

  override fun createLink(link: Path, existing: Path) {
    super.createLink(link, existing)
  }

  override fun createSymbolicLink(link: Path, target: Path, vararg attrs: FileAttribute<*>?) {
    super.createSymbolicLink(link, target, *attrs)
  }

  override fun readSymbolicLink(link: Path): Path {
    return super.readSymbolicLink(link)
  }

  override fun setCurrentWorkingDirectory(currentWorkingDirectory: Path) {
    super.setCurrentWorkingDirectory(currentWorkingDirectory)
  }

  override fun getMimeType(path: Path): String? {
    return super.getMimeType(path)
  }

  override fun getEncoding(path: Path): Charset {
    return super.getEncoding(path)
  }

  override fun getTempDirectory(): Path {
    return super.getTempDirectory()
  }

  override fun isSameFile(path1: Path, path2: Path, vararg options: LinkOption): Boolean {
    return super.isSameFile(path1, path2, *options)
  }

  override fun readStream(path: Path, vararg options: OpenOption): InputStream {
    TODO("")
  }

  override fun writeStream(path: Path, vararg options: OpenOption): OutputStream {
    TODO("")
  }

  override fun checkPolicy(request: AccessRequest): AccessResponse {
    TODO("")
  }
}

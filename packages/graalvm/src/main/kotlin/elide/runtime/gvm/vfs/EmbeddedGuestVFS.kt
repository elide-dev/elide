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

import java.io.File
import java.net.URI
import java.nio.file.Path
import elide.runtime.gvm.internals.GuestVFS
import elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl

/**
 * # Virtual File-System: Embedded
 *
 * Public access to the factory for embedded guest virtual file-systems. "Embedded" VFS operating modes use in-memory
 * file system implementations. Embedded I/O can use tarballs, zip archives, or other sources for file system data.
 *
 * &nbsp;
 *
 * ## Usage
 *
 * Virtual file-systems can be created using the factory methods provided by this object, for example [empty],
 * [writable], and [forBundle]. One or more bundles can be provided; they will be decoded in the order supplied, and
 * consulted via the VFS internals with order preserved.
 *
 * Archives or sources of different types and origins can be combined in a single VFS instance. For example, in-memory
 * data can be combined with on-disk data, or with data from a remote source.
 *
 * &nbsp;
 *
 * ## Elide VFS
 *
 * Elide uses embedded virtual filesystems for built-in features like support for the Node API. VFS features may be
 * active to support these features even when Host I/O is enabled for a given run. In these cases, the VFS is used in
 * an "overlay" fashion, where the VFS is consulted first, and then the host file system is consulted if the VFS does
 * not contain the requested file (on read).
 *
 * Writable VFS instances absorb writes and changes to the VFS, but do not modify the underlying source data. This
 * allows applications to use in-memory VFS for temporary files and other data, without modifying the original source
 * data.
 *
 * Persistent data should always be written to disk using Host I/O. For disk-based persistence, VFS should be mounted
 * read-only, with Host I/O granted at runtime.
 */
public object EmbeddedGuestVFS {
  /** @return Empty embedded VFS. */
  public fun empty(): GuestVFS = EmbeddedGuestVFSImpl.create()

  /** @return Empty embedded VFS, but writable. */
  public fun writable(): GuestVFS = EmbeddedGuestVFSImpl.Builder.newBuilder()
    .setReadOnly(false)
    .build()

  /** @return Embedded VFS backed by [bundle], but writable. */
  public fun writable(bundle: URI): GuestVFS = EmbeddedGuestVFSImpl.Builder.newBuilder()
    .setBundlePaths(listOf(bundle))
    .setReadOnly(false)
    .build()

  /** @return Embedded VFS backed by a list of [bundles], but writable. */
  public fun writable(bundles: List<URI>): GuestVFS = EmbeddedGuestVFSImpl.Builder.newBuilder()
    .setBundlePaths(bundles)
    .setReadOnly(false)
    .build()

  /** @return Embedded VFS backed by [bundle], but writable. */
  public fun writable(bundle: File): GuestVFS = EmbeddedGuestVFSImpl.Builder.newBuilder()
    .setBundleFiles(listOf(bundle))
    .setReadOnly(false)
    .build()

  /** @return Embedded VFS backed by the provided [bundle] URI. */
  public fun forBundle(vararg bundle: URI): GuestVFS = EmbeddedGuestVFSImpl.Builder.newBuilder()
    .setBundlePaths(bundle.toList())
    .build()

  /** @return Embedded VFS backed by the provided [bundle] file. */
  public fun forBundle(vararg bundle: File): GuestVFS = EmbeddedGuestVFSImpl.Builder.newBuilder()
    .setBundleFiles(bundle.toList())
    .build()

  /** @return Embedded VFS backed by a list of [bundles]. */
  public fun forBundles(bundles: List<URI>): GuestVFS = EmbeddedGuestVFSImpl.Builder.newBuilder()
    .setBundlePaths(bundles)
    .setReadOnly(false)
    .build()

  /** @return Embedded VFS backed by a single [target] Zip file; this uses the JDK's built-in `zipfs` module. */
  public fun forZip(target: URI): GuestVFS = EmbeddedGuestVFSImpl.Builder.newBuilder()
    .setZipTarget(target)
    .build()

  /** @return Embedded VFS backed by a single [target] file; this uses the JDK's built-in file-based FS logic. */
  public fun forTargetFile(target: URI): GuestVFS = EmbeddedGuestVFSImpl.Builder.newBuilder()
    .setFileTarget(target)
    .build()

  /** @return Embedded VFS backed by a single [target] file; this uses the JDK's built-in file-based FS logic. */
  public fun forTargetFile(target: Path): GuestVFS = EmbeddedGuestVFSImpl.Builder.newBuilder()
    .setFileTarget(target)
    .build()
}

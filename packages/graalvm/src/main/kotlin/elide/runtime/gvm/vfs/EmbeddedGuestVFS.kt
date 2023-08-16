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

package elide.runtime.gvm.vfs

import java.io.File
import java.net.URI
import elide.runtime.gvm.internals.GuestVFS
import elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl

/** Public access to the factory for embedded guest virtual file-systems. */
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
}

package elide.runtime.gvm.vfs

import elide.runtime.gvm.internals.GuestVFS
import elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl
import java.io.File
import java.net.URI

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
}

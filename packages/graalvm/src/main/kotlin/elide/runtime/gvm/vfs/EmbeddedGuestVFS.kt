package elide.runtime.gvm.vfs

import elide.runtime.gvm.internals.GuestVFS
import elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl
import java.io.File
import java.net.URI

/** Public access to the factory for embedded guest virtual file-systems. */
public object EmbeddedGuestVFS {
  /** @return Empty embedded VFS. */
  public fun empty(): GuestVFS = EmbeddedGuestVFSImpl.create()

  /** @return Embedded VFS backed by the provided [bundle] URI. */
  public fun forBundle(bundle: URI): GuestVFS = EmbeddedGuestVFSImpl.Builder.newBuilder()
    .setBundlePath(bundle)
    .build()

  /** @return Embedded VFS backed by the provided [bundle] file. */
  public fun forBundle(bundle: File): GuestVFS = EmbeddedGuestVFSImpl.Builder.newBuilder()
    .setBundleFile(bundle)
    .build()
}

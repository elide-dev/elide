package elide.runtime.gvm.vfs

import elide.runtime.gvm.internals.GuestVFS
import elide.runtime.gvm.internals.vfs.HostVFSImpl

/** Public access to the factory for bridged host I/O access. */
public object HostVFS {
  /** @return Factory for producing new instances of [HostVFSImpl]. */
  public fun acquire(): GuestVFS = HostVFSImpl.Builder.newBuilder().build()
}

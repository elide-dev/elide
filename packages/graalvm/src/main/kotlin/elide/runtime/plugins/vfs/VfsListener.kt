package elide.runtime.plugins.vfs

import elide.runtime.vfs.GuestVFS

/** A listener for VFS events, such as initialization and configuration. */
public interface VfsListener {
  /** Called when the [fileSystem] is prepared, before being assigned to the guest engine.  */
  public fun onVfsCreated(fileSystem: GuestVFS)
}

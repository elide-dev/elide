package elide.runtime.gvm.internals.vfs

import elide.testing.annotations.TestCase

/** Tests for the host-based VFS implementation. */
@TestCase internal class HostVFSTest : AbstractVFSTest<HostVFSImpl, HostVFSImpl.Builder, HostVFSImpl.HostVFSFactory>() {
  /** @return Host VFS factory. */
  override fun factory() = HostVFSImpl.HostVFSFactory

  /** @return New builder. */
  override fun newBuilder(): HostVFSImpl.Builder = HostVFSImpl.Builder.newBuilder()

  /** @return Indication that host changes are expected. */
  override fun shouldUseHost(): Boolean = true
}

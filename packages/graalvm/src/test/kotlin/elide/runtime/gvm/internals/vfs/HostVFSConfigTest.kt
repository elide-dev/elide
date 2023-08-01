package elide.runtime.gvm.internals.vfs

import io.micronaut.test.support.TestPropertyProvider
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.annotations.Inject
import elide.runtime.gvm.internals.GuestVFS
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Test injection of a file-system implementation when `vfs.mode=HOST`. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestCase internal class HostVFSConfigTest : TestPropertyProvider {
  override fun getProperties(): MutableMap<String, String> = mutableMapOf(
    "elide.gvm.vfs.enabled" to "true",
    "elide.gvm.vfs.mode" to "HOST",
  )

  @Inject lateinit var vfs: GuestVFS

  @Test fun testInjectable() {
    assertNotNull(vfs, "should be able to inject a VFS implementation when `mode=HOST`")
  }

  @Test fun testExpectedImpl() {
    assertTrue(
      vfs is HostVFSImpl,
      "expected host VFS implementation when `mode=HOST`, instead got '${vfs::class.java.simpleName}'",
    )
  }
}

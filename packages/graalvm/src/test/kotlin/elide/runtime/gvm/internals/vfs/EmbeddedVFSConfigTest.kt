package elide.runtime.gvm.internals.vfs

import elide.annotations.Inject
import elide.runtime.gvm.internals.GuestVFS
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import io.micronaut.test.support.TestPropertyProvider
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Test injection of a file-system implementation when `vfs.mode=GUEST`. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestCase internal class EmbeddedVFSConfigTest: TestPropertyProvider {
  override fun getProperties(): MutableMap<String, String> = mutableMapOf(
    "elide.gvm.vfs.enabled" to "true",
    "elide.gvm.vfs.mode" to "GUEST",
  )

  @Inject lateinit var vfs: GuestVFS

  @Test fun testInjectable() {
    assertNotNull(vfs, "should be able to inject a VFS implementation when `mode=GUEST`")
  }

  @Test fun testExpectedImpl() {
    assertTrue(
      vfs is EmbeddedGuestVFSImpl,
      "expected embedded VFS implementation when `mode=GUEST`, instead got '${vfs::class.java.simpleName}'",
    )
  }
}

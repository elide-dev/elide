@file:Suppress("MnInjectionPoints")

package elide.runtime.gvm.internals

import elide.runtime.gvm.intrinsics.GuestIntrinsic
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Tests for the [IntrinsicsManager]. */
@MicronautTest class IntrinsicsManagerTest {
  @Inject private lateinit var manager: IntrinsicsManager
  @Inject private lateinit var intrinsics: Collection<GuestIntrinsic>

  @Test fun testInjectable() {
    assertNotNull(manager, "should be able to inject intrinsics manager")
    assertTrue(intrinsics.isNotEmpty(), "should be able to collect intrinsics via injection")
  }

  @Test fun testResolveIntrinsics() {
    val resolved = manager.resolver().resolve(GraalVMGuest.JAVASCRIPT)
    assertNotNull(resolved, "should not get `null` from intrinsics resolver")
    assertTrue(resolved.isNotEmpty(), "resolved set of intrinsics should not be empty")
  }
}

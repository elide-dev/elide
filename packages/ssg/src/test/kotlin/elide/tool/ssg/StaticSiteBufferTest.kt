@file:OptIn(ExperimentalCoroutinesApi::class)

package elide.tool.ssg

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for the [StaticSiteBuffer]. */
@MicronautTest class StaticSiteBufferTest : AbstractSSGTest() {
  @Test fun testStaticSiteBufferLifecycle() = runTest {
    // cannot consume an unsealed buffer
    assertThrows<IllegalStateException> {
      StaticSiteBuffer().consumeAsync {
        // nothing
        throw UnsupportedOperationException("failed")
      }
    }

    // cannot consume a closed buffer
    assertThrows<IllegalStateException> {
      val buf = StaticSiteBuffer()
      buf.close()
      buf.consumeAsync {
        // nothing
        throw UnsupportedOperationException("failed")
      }
    }
  }

  @Test fun testStaticSiteBufferImmutable() = runTest {
    val buf = StaticSiteBuffer()
    assertEquals(0, buf.size(), "size should be 0 for fresh site buffer")
    buf.add(staticFragment(
      endpoint(),
      "some content here",
    ))
    assertEquals(1, buf.size(), "size should be 1 after adding a fragment")
    buf.seal()
    assertThrows<IllegalStateException> {
      buf.add(staticFragment(
        endpoint(),
        "some content here",
      ))
    }
  }

  @Test fun testStaticSiteBufferClosed() = runTest {
    val buf = StaticSiteBuffer()
    assertEquals(0, buf.size(), "size should be 0 for fresh site buffer")
    buf.add(staticFragment(
      endpoint(),
      "some content here",
    ))
    assertEquals(1, buf.size(), "size should be 1 after adding a fragment")
    buf.close()
    assertThrows<IllegalStateException> {
      buf.add(staticFragment(
        endpoint(),
        "some content here",
      ))
    }
  }

  @Test fun testStaticSiteBufferFragmentCount() = runTest {
    val buf = StaticSiteBuffer()
    assertEquals(0, buf.size(), "size should be 0 for fresh site buffer")
    buf.add(staticFragment(
      endpoint(),
      "some content here",
    ))
    assertEquals(1, buf.size(), "size should be 1 after adding a fragment")
  }

  @Test fun testStaticSiteBufferReset() = runTest {
    val buf = StaticSiteBuffer()
    assertEquals(0, buf.size(), "size should be 0 for fresh site buffer")
    buf.add(staticFragment(
      endpoint(),
      "some content here",
    ))
    assertEquals(1, buf.size(), "size should be 1 after adding a fragment")
    buf.close()
    assertThrows<IllegalStateException> {
      buf.add(staticFragment(
        endpoint(),
        "some content here",
      ))
    }
    buf.reset()
    assertEquals(0, buf.size(), "after resetting buffer, size should be 0")
    assertTrue(buf.isOpen(), "after resetting buffer, it should be in open state")
    assertFalse(buf.isClosed(), "after resetting buffer, it should be in open state")
  }
}

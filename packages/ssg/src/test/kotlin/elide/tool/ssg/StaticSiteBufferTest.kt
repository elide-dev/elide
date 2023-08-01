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

@file:OptIn(ExperimentalCoroutinesApi::class)

package elide.tool.ssg

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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

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
import java.net.URL
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** Tests for data structures used by the SSG compiler. */
@MicronautTest class SSGDataStructureTests : AbstractSSGTest() {
  @Test fun testSuccessFailure() = runTest {
    val params = SiteCompilerParams(
      "1",
      "2",
      SiteCompilerParams.Output.fromParams("/yo/whats/up.tar"),
    )
    val appInfo = LoadedAppInfo(
      URL("https://example.com"),
      manifest(),
      params,
      true,
    )
    val success = SiteCompileResult.Success(
      params,
      appInfo,
      "/yo/whats/up.tar",
      StaticSiteBuffer(),
    )
    val err = IllegalStateException("sample")
    val failure = SiteCompileResult.Failure(
      params,
      err,
      -100,
    )
    assertTrue(success.success)
    assertEquals(params, success.params)
    assertEquals(appInfo, success.appInfo)
    assertNotNull(success.toString())
    assertNotNull(success.buffer)
    assertTrue(success.toString().contains("Success"))
    assertEquals(0, success.exitCode, "exit code for successful call should be 0")

    assertEquals(err, failure.err)
    assertEquals(params, failure.params)
    assertNotEquals(0, failure.exitCode, "exit code for failed call should not be 0")
    assertFalse(failure.success)
    assertNotNull(failure.toString())
    assertTrue(failure.toString().contains("Failure"))
  }

  @Test fun testFragmentWrite() = runTest {
    val frag = staticFragment(
      endpoint(),
      "some content here",
    )
    val success = AppStaticWriter.FragmentWrite.success(
      frag,
      "/some/path/here",
      123L,
    )
    assertNotNull(success)
    assertNotNull(success.toString())
    assertTrue(success.toString().contains("Success"))
    assertEquals(123L, success.size)
    assertEquals("/some/path/here", success.path)
    assertEquals("some content here", success.fragment.content.array().decodeToString())
    assertEquals(-1, success.compressed)
    assertNotNull(success.hashCode())
    assertEquals(success.hashCode(), success.hashCode())
    assertEquals(success, success)
    assertTrue(success.writeResult)

    val success2 = AppStaticWriter.FragmentWrite.success(
      frag,
      "/some/path/here",
      123L,
    )
    assertEquals(success, success2)

    val err = IllegalStateException("sample")
    val failure = AppStaticWriter.FragmentWrite.failure(
      frag,
      "/some/path/here",
      err = err,
    )
    assertNotNull(failure)
    assertNotNull(failure.toString())
    assertTrue(failure.toString().contains("Failure"))
    assertEquals(err, failure.err)
    assertFalse(failure.writeResult)
    assertEquals(failure.hashCode(), failure.hashCode())
    assertEquals("/some/path/here", failure.path)
    assertNotEquals(success, failure)
  }

  @Test fun testStaticFragment() = runTest {

  }
}

/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.server.controller

import io.micronaut.http.HttpResponse
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.assertDoesNotThrow
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Tests for the top-level [PageController] extension point. */
@MicronautTest class PageControllerTest {
  // Application bean context.
  @Inject lateinit var sample: SamplePageController

  private fun assetResponse(response: HttpResponse<*>?, contentType: String? = null) {
    assertNotNull(response)
    assertEquals(200, response.status.code)
    assertTrue(response.headers.contains("Content-Type"))
    if (contentType != null) {
      assertTrue(response.headers.get("Content-Type")!!.contains(contentType))
    }
  }

  @Test fun testInjectable() {
    assertNotNull(sample)
  }

  @Test fun testCanAcquireAppContext() {
    assertNotNull(
      sample.context(),
      "`PageController` inheritors should be able to acquire the app context"
    )
  }

  @Test fun testRenderHtmlPage() {
    assetResponse(
      assertDoesNotThrow {
        runBlocking {
          sample.indexPage()
        }
      }
    )
  }

  @Test fun testRenderTextStaticResource() {
    assetResponse(
      assertDoesNotThrow {
        runBlocking {
          sample.somethingText()
        }
      },
      "text/plain"
    )
  }

  @Test fun testNotFoundStaticFile() {
    val response = assertDoesNotThrow {
      runBlocking {
        sample.somethingMissing()
      }
    }
    assertNotNull(
      response
    )
    assertEquals(
      404,
      response.status.code
    )
  }
}

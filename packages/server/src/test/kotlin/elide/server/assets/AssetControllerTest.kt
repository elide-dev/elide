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

package elide.server.assets

import io.micronaut.http.HttpRequest
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.assertDoesNotThrow
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/** Tests for the built-in [AssetController]. */
@MicronautTest
class AssetControllerTest {
  @Inject lateinit var controller: AssetController

  @Test fun testInjectable() {
    assertNotNull(controller)
  }

  @Test fun testServeBunkAsset() {
    val response = assertDoesNotThrow {
      runBlocking {
        controller.assetGet(
          HttpRequest.GET<Any>("/something-bunk.txt"),
          "something-bunk",
          "txt",
        )
      }
    }
    assertNotNull(response)
    assertEquals(404, response.status.code)
  }

  @Test fun testServeKnownGoodAssetStyle() {
    val response = assertDoesNotThrow {
      runBlocking {
        controller.assetGet(
          HttpRequest.GET<Any>("02ade191.css"),
          "02ade191",
          "css",
        )
      }
    }
    assertNotNull(response)
    assertEquals(200, response.status.code)
    assertEquals("text/css", response.contentType.get().toString())
    assertNotNull(response.headers.get("Content-Length"))
    assertNotEquals(0, response.headers.get("Content-Length")?.toLong())
  }

  @Test fun testServeKnownGoodAssetScript() {
    val response = assertDoesNotThrow {
      runBlocking {
        controller.assetGet(
          HttpRequest.GET<Any>("c426de48.js"),
          "c426de48",
          "js",
        )
      }
    }
    assertNotNull(response)
    assertEquals(200, response.status.code)
    assertEquals("application/javascript", response.contentType.get().toString())
    assertNotNull(response.headers.get("Content-Length"))
    assertNotEquals(0, response.headers.get("Content-Length")?.toLong())
  }
}

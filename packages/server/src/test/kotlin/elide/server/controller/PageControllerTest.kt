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

package elide.server.controller

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.server.assets.AssetReference
import elide.server.assets.AssetType

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

  @Test fun testGenerateAssetReferenceInvalid() {
    assertThrows<IllegalArgumentException> {
      sample.asset("some-invalid-module")
    }
  }

  @Test fun testGenerateReferenceValid() {
    assertNotNull(
      assertDoesNotThrow {
        sample.asset("styles.base")
      },
      "acquiring a known-good asset reference should not produce `null` or throw"
    )
  }

  @Test fun testGenerateReferenceModuleIdRequired() {
    assertThrows<IllegalArgumentException> {
      sample.asset("")
    }
    assertThrows<IllegalArgumentException> {
      sample.asset(" ")
    }
    assertThrows<IllegalArgumentException> {
      sample.asset("") {
        // nothing
      }
    }
    assertThrows<IllegalArgumentException> {
      sample.asset(" ") {
        // nothing
      }
    }
    assertThrows<IllegalArgumentException> {
      sample.asset("styles.base") {
        module = ""
      }
    }
  }

  @Test fun testAssignmentToUtilities() {
    assertDoesNotThrow {
      sample.assetManager = sample.assetManager
      sample.appContext = sample.appContext
    }
  }

  @Test fun testGenerateReferenceViaHandler() {
    val ref = assertDoesNotThrow {
      sample.asset("styles.base") {
        module = "styles.base"
        inline = true
        preload = true
      }
    }

    assertNotNull(ref, "customizing an asset reference should not produce `null`")
    assertEquals(
      AssetReference(
        module = "styles.base",
        assetType = AssetType.STYLESHEET,
        href = ref.href,
        type = null,
        inline = true,
        preload = true,
      ),
      ref,
      "should be able to customize an asset reference with a handler"
    )
  }

  @Test fun testCanAcquireAssetManager() {
    assertNotNull(
      sample.assets(),
      "`PageController` inheritors should be able to acquire the asset runtime"
    )
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

  @Test fun testRenderCssDocument() {
    assetResponse(
      assertDoesNotThrow {
        runBlocking {
          sample.styles()
        }
      }
    )
  }

  @Test fun testRenderStylesheetAssetWithHandler() {
    assetResponse(
      assertDoesNotThrow {
        runBlocking {
          sample.assetStyle(HttpRequest.GET("/styles/base.css"))
        }
      }
    )
  }

  @Test fun testRenderStylesheetAssetWithKnownModule() {
    assetResponse(
      assertDoesNotThrow {
        runBlocking {
          sample.assetStyleExplicit(HttpRequest.GET("/styles/base.other.css"))
        }
      }
    )
  }

  @Test fun testRenderStylesheetAssetWithGenericCall() {
    assetResponse(
      assertDoesNotThrow {
        runBlocking {
          sample.assetGeneric(HttpRequest.GET("/styles/base.another.css"))
        }
      }
    )
  }

  @Test fun testRenderScriptAssetWithHandler() {
    assetResponse(
      assertDoesNotThrow {
        runBlocking {
          sample.assetScript(HttpRequest.GET("/scripts/ui.js"))
        }
      }
    )
  }

  @Test fun testRenderScriptAssetWithKnownModule() {
    assetResponse(
      assertDoesNotThrow {
        runBlocking {
          sample.assetScriptExplicit(HttpRequest.GET("/scripts/ui.other.js"))
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

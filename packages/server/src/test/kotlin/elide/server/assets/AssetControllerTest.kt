package elide.server.assets

import io.micronaut.http.HttpRequest
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.assertDoesNotThrow
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

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
          HttpRequest.GET<Any>("/753eb23d.css"),
          "753eb23d",
          "css",
        )
      }
    }
    assertNotNull(response)
    assertEquals(200, response.status.code)
    assertEquals(response.contentType.get().toString(), "text/css")
    assertNotNull(response.headers.get("Content-Length"))
    assertNotEquals(0, response.headers.get("Content-Length")?.toLong())
  }
}

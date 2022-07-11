package elide.server.assets

import elide.server.StreamedAsset
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.*

/** Tests general asset serving features, like ETags/conditional requests and compression variants. */
@MicronautTest class ServerAssetServingTest {
  companion object {
    private const val assetPrefix = "/_/assets"
    private const val sampleStylesheet = "$assetPrefix/753eb23d.css"
  }

  @Inject lateinit var manager: AssetManager

  private fun execute(
    request: HttpRequest<Any>,
    moduleId: String? = null
  ): HttpResponse<StreamedAsset> = runBlocking {
    return@runBlocking manager.serveAsync(
      request,
      moduleId,
    ).await()
  }

  private fun assertAsset(
    assetType: AssetType,
    response: HttpResponse<StreamedAsset>?,
    status: Int = 200,
    encoding: String? = "identity",
    more: (() -> Unit)? = null
  ) {
    assertNotNull(response)
    assertEquals(status, response.status.code)
    if (status == 200) {
      assertTrue(response.headers.contains("Content-Encoding"))
      assertTrue(response.headers.contains("Content-Length"))
      assertNotNull(response.headers.get("Content-Length")?.toLongOrNull())
      assertTrue(response.headers.get("Content-Length")!!.toLong() > 0)
      assertTrue(response.headers.contains("Content-Type"))
      assertTrue(response.headers.contains("ETag"))
      assertTrue(response.headers.get("Content-Type")!!.contains(assetType.mediaType.type))
      assertTrue(response.headers.get("Content-Type")!!.contains(assetType.mediaType.subtype))
      if (encoding != null) {
        assertEquals(response.headers.get("Content-Encoding"), encoding)
      }
    }
    more?.invoke()
  }

  @Test fun testInjectable() {
    assertNotNull(manager)
  }

  @Test fun testNonConditional() {
    val response = execute(HttpRequest.GET(sampleStylesheet))
    assertEquals(200, response.status.code)
    assertAsset(
      AssetType.STYLESHEET,
      response,
    )
  }

  @Test fun testEtags() {
    val response = execute(HttpRequest.GET(sampleStylesheet))
    assertAsset(
      AssetType.STYLESHEET,
      response,
    )
    assertTrue(response.headers.contains("ETag"))
    val etag = response.headers.get("ETag")!!
    if (etag.startsWith("W/")) {
      assertTrue(etag.drop(2).startsWith("\""))
    }
    assertTrue(etag.endsWith("\""))
  }

  @Test fun testConditionalRequestETag() {
    val response = execute(HttpRequest.GET(sampleStylesheet))
    assertAsset(
      AssetType.STYLESHEET,
      response,
    )
    val conditionNotSatisified = execute(
      HttpRequest.GET<Any>(sampleStylesheet)
        .header("If-None-Match", "some-value-that-isnt-the-etag")
    )
    assertAsset(
      AssetType.STYLESHEET,
      conditionNotSatisified,
    )
    val conditionSatisfied = execute(
      HttpRequest.GET<Any>(sampleStylesheet)
        .header("If-None-Match", response.header("ETag")!!)
    )
    assertAsset(AssetType.STYLESHEET, conditionSatisfied, status = 304) {
      assertTrue(response.headers.contains("Content-Length"))
    }
  }

  @CsvSource("gzip", "br", "deflate")
  @ParameterizedTest
  fun testCompressionSupported(encodingName: String) {
    val response = execute(HttpRequest.GET(sampleStylesheet))
    assertAsset(
      AssetType.STYLESHEET,
      response,
    )
    val compressedResponse = execute(
      HttpRequest.GET<Any?>(sampleStylesheet)
        .header("Accept-Encoding", encodingName)
    )
    assertAsset(
      AssetType.STYLESHEET,
      compressedResponse,
      encoding = encodingName,
    )
  }

  @Test fun testUnsupportedCompression() {
    val response = execute(HttpRequest.GET(sampleStylesheet))
    assertAsset(
      AssetType.STYLESHEET,
      response,
    )
    val identityResponse = execute(
      HttpRequest.GET<Any?>(sampleStylesheet)
        .header("Accept-Encoding", "some-encoding, some-other-unfamiliar-encoding, perhaps-another")
    )
    assertAsset(
      AssetType.STYLESHEET,
      identityResponse,
    )
  }
}

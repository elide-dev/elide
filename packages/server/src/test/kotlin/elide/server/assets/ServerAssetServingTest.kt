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

package elide.server.assets

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tools.elide.assets.AssetBundle.AssetContent
import tools.elide.assets.AssetBundle.GenericBundle
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlin.test.*
import elide.server.StreamedAsset

/** Tests general asset serving features, like ETags/conditional requests and compression variants. */
@Ignore
@MicronautTest class ServerAssetServingTest {
  companion object {
    private const val assetPrefix = "/_/assets"
    private const val sampleStylesheet = "$assetPrefix/753eb23d.css"
  }

  // Asset index.
  @Inject internal lateinit var index: ServerAssetIndex

  // Asset manager.
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
    encoding: String? = null,
    more: (() -> Unit)? = null
  ) {
    assertNotNull(response)
    assertEquals(status, response.status.code)
    if (status == 200) {
      assertTrue(response.headers.contains("Content-Length"))
      assertNotNull(response.headers.get("Content-Length")?.toLongOrNull())
      assertTrue(response.headers.get("Content-Length")!!.toLong() > 0)
      assertTrue(response.headers.contains("Content-Type"))
      assertTrue(response.headers.contains("ETag"))
      assertTrue(response.headers.get("Content-Type")!!.contains(assetType.mediaType.type))
      assertTrue(response.headers.get("Content-Type")!!.contains(assetType.mediaType.subtype))
      if (encoding != null) {
        assertTrue(response.headers.contains("Content-Encoding"))
        assertEquals(response.headers.get("Content-Encoding"), encoding)
      }
    }
    more?.invoke()
  }

  @Test fun testInjectable() {
    assertNotNull(manager)
    assertNotNull(index)
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

  @Test fun testResolveUnknownFromModuleId() {
    assertNull(
      assertDoesNotThrow {
        manager.resolve(HttpRequest.GET<Any>("/_/assets/some-asset.css"), "unknown-module")
      }
    )
  }

  @Test fun testResolveUnknownFromRequest() {
    assertNull(
      assertDoesNotThrow {
        manager.resolve(HttpRequest.GET<Any>("/_/assets/some-asset.css"))
      }
    )
  }

  @Test fun testResolveKnownFromModuleId() {
    assertNotNull(
      assertDoesNotThrow {
        manager.resolve(HttpRequest.GET<Any>("/_/assets/some-asset.css"), "styles.base")
      }
    )
  }

  @Test fun testResolveKnownFromRequest() {
    assertNotNull(
      assertDoesNotThrow {
        manager.resolve(HttpRequest.GET<Any>(manager.linkForAsset("styles.base")))
      }
    )
  }

  @Test fun testGenerateUnknownLink() {
    assertThrows<IllegalArgumentException> {
      manager.linkForAsset("unknown-module-id")
    }
  }

  @CsvSource("styles.base:css", "scripts.ui:js")
  @ParameterizedTest
  fun testGenerateLink(spec: String) {
    val split = spec.split(":")
    val module = split.first()
    val extension = split.last()
    val assetLink = manager.linkForAsset(module)
    assertNotNull(assetLink)
    assertTrue(assetLink.endsWith(".$extension"), "expected extension '$extension' for module '$module'")
  }

  @Test fun testGenerateLinkOverrideType() {
    val assetLink = manager.linkForAsset("styles.base")
    assertTrue(assetLink.endsWith(".css"))
    val assetLink2 = manager.linkForAsset("styles.base", overrideType = AssetType.TEXT)
    assertTrue(assetLink2.endsWith(".txt"))
  }

  @Test fun testGenerateLinkGeneric() {
    val assetLink = manager.linkForAsset("styles.base")
    assertTrue(assetLink.endsWith(".css"))
    val assetLink2 = manager.linkForAsset("styles.base", overrideType = AssetType.GENERIC)
    assertFalse(assetLink2.contains(".")) // should not have a file extension
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

  @Test fun testLocateKnownGoodModule() {
    assertNotNull(
      assertDoesNotThrow {
        manager.findAssetByModuleId("styles.base")
      }
    )
  }

  @Test fun testLocateKnownBadModule() {
    assertNull(
      assertDoesNotThrow {
        manager.findAssetByModuleId("some.unknown.module.here")
      }
    )
  }

  @Test fun testServerAssetDescriptorScript() {
    val assetType = AssetType.SCRIPT
    val bundle = index.activeManifest().bundle
    val someScript = bundle.scriptsMap[bundle.scriptsMap.keys.first()]!!
    val idx = List(
      bundle.assetList.filter {
        it.module == someScript.module
      }.size
    ) { idx ->
      idx
    }.first()

    val concrete = index.buildConcreteAsset(
      assetType,
      someScript.module,
      bundle,
      sortedSetOf(idx),
    )
    assertTrue(concrete is ServerAsset.Script, "concrete script should be a `ServerAsset.Script`")
    assertNotNull(concrete.descriptor)
    assertNotNull(concrete.assetType)
    assertEquals(assetType, concrete.assetType)
    assertEquals(someScript.module, concrete.module)
  }

  @Test fun testServerAssetDescriptorStyle() {
    val assetType = AssetType.STYLESHEET
    val bundle = index.activeManifest().bundle
    val someStylesheet = bundle.stylesMap[bundle.stylesMap.keys.first()]!!
    val idx = List(
      bundle.assetList.filter {
        it.module == someStylesheet.module
      }.size
    ) { idx ->
      idx
    }.first()

    val concrete = index.buildConcreteAsset(
      assetType,
      someStylesheet.module,
      bundle,
      sortedSetOf(idx),
    )
    assertTrue(concrete is ServerAsset.Stylesheet, "concrete script should be a `ServerAsset.Stylesheet`")
    assertNotNull(concrete.descriptor)
    assertNotNull(concrete.assetType)
    assertEquals(assetType, concrete.assetType)
    assertEquals(someStylesheet.module, concrete.module)
  }

  @Test fun testServerAssetDescriptorText() {
    val assetType = AssetType.TEXT
    val bundle = index.activeManifest().bundle
    val someText = GenericBundle.newBuilder()
      .setModule("module-text-id")
      .setToken("abc123123123123123123")
      .build()

    val newBundle = bundle.toBuilder()
      .putGeneric("module-text-id", someText)
      .addAsset(
        AssetContent.newBuilder()
          .setModule("module-text-id")
          .setToken("abc123123123123123123")
      )
      .build()

    val idx = newBundle.assetCount - 1
    val concrete = index.buildConcreteAsset(
      assetType,
      someText.module,
      newBundle,
      sortedSetOf(idx),
    )
    assertTrue(concrete is ServerAsset.Text, "concrete script should be a `ServerAsset.Text`")
    assertNotNull(concrete.descriptor)
    assertNotNull(concrete.assetType)
    assertEquals(assetType, concrete.assetType)
    assertEquals(someText.module, concrete.module)
  }
}

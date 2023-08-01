package elide.server.assets

import com.google.common.graph.ElementOrder
import com.google.common.graph.ImmutableNetwork
import com.google.common.graph.NetworkBuilder
import com.google.common.truth.extensions.proto.ProtoTruth.assertThat
import com.google.protobuf.ByteString
import elide.server.AssetModuleId
import elide.server.TestUtil
import elide.server.cfg.AssetConfig
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import tools.elide.assets.AssetBundle
import tools.elide.assets.AssetBundle.*
import tools.elide.assets.ManifestFormat
import tools.elide.crypto.HashAlgorithm
import tools.elide.data.CompressedData
import tools.elide.data.compressedData
import tools.elide.data.dataContainer
import tools.elide.data.dataFingerprint
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/** Tests for [ServerAssetIndex]. */
@Suppress("UnstableApiUsage")
@MicronautTest
class ServerAssetIndexTest {
  private fun loadSampleManifest(): AssetBundle {
    val data = TestUtil.loadBinary("/manifests/app.assets.pb")
    val baos = ByteArrayInputStream(data)
    val provider = ServerAssetManifestProvider()
    return provider.deserializeLoadManifest(
      ManifestFormat.BINARY to baos,
    ) ?: error("Failed to load embedded sample manifest")
  }

  private fun createIndexer(
    assetBundle: AssetBundle? = null,
    config: AssetConfig? = null
  ): Pair<AssetBundle, ServerAssetIndex> {
    val sample = assetBundle ?: assertNotNull(loadSampleManifest())
    return sample to ServerAssetIndex(
      config ?: object : AssetConfig {},
      object : AssetManifestLoader {
        override fun findLoadManifest(candidates: List<Pair<ManifestFormat, String>>): AssetBundle {
          return sample
        }

        override fun findManifest(candidates: List<Pair<ManifestFormat, String>>): Pair<ManifestFormat, InputStream> {
          return ManifestFormat.BINARY to ByteArrayInputStream(ByteArray(0))
        }
      }
    )
  }

  @Test fun testGenerateIndexes() {
    val (sample, indexer) = createIndexer()
    val indexes = assertDoesNotThrow {
      indexer.buildAssetIndexes(
        sample
      )
    }
    assertNotNull(indexes, "should not get `null` from `buildAssetIndexes`")
    val (depGraph, manifest) = indexes
    assertNotNull(depGraph, "should not get `null` for first item in interpreted asset manifest pair")
    assertNotNull(manifest, "should not get `null` for second item in interpreted asset manifest pair")
    assertTrue(depGraph.nodes().isNotEmpty(), "nodes list in dep graph should not be empty")
  }

  private fun identityVariant(): CompressedData {
    val data = "hello world".toByteArray(StandardCharsets.UTF_8)
    return compressedData {
      this.size = data.size.toLong()
      this.data = dataContainer {
        raw = ByteString.copyFrom(data)
        integrity.add(
          dataFingerprint {
            this.hash = HashAlgorithm.SHA256
            this.fingerprint = ByteString.copyFrom(MessageDigest.getInstance("SHA-256").digest(data))
          }
        )
      }
    }
  }

  @Test fun testGenerateFailDuplicateTags() {
    val bundle = AssetBundle.newBuilder()
    bundle.addAsset(AssetContent.newBuilder().setModule("test1").setToken("abc123").addVariant(identityVariant()))
    bundle.addAsset(AssetContent.newBuilder().setModule("test2").setToken("abc123").addVariant(identityVariant()))
    bundle.addAsset(AssetContent.newBuilder().setModule("test1").setToken("abc123").addVariant(identityVariant()))
    bundle.putScripts("test1", ScriptBundle.newBuilder().setModule("test1").build())
    bundle.putScripts("test2", ScriptBundle.newBuilder().setModule("test2").build())
    val (_, indexer) = createIndexer(bundle.build())
    assertThrows<IllegalStateException> {
      indexer.buildAssetIndexes(bundle.build())
    }
  }

  @Test fun testGenerateFailDuplicateModuleIds() {
    val bundle = AssetBundle.newBuilder()
    bundle.addAsset(AssetContent.newBuilder().setModule("test1").setToken("abc123").addVariant(identityVariant()))
    bundle.addAsset(AssetContent.newBuilder().setModule("test2").setToken("abc123").addVariant(identityVariant()))
    bundle.putScripts("test1", ScriptBundle.newBuilder().setModule("test1").build())
    bundle.putScripts("test2", ScriptBundle.newBuilder().setModule("test2").build())
    bundle.putStyles("test1", StyleBundle.newBuilder().setModule("test1").build())
    val (_, indexer) = createIndexer(bundle.build())
    assertThrows<IllegalStateException> {
      indexer.buildAssetIndexes(bundle.build())
    }
  }

  @Test fun testGenerateWithScriptDeps() {
    val bundle = AssetBundle.newBuilder()
    bundle.addAsset(AssetContent.newBuilder().setModule("test1").setToken("abc123").addVariant(identityVariant()))
    bundle.addAsset(AssetContent.newBuilder().setModule("test2").setToken("abc123").addVariant(identityVariant()))
    bundle.putScripts("test1", ScriptBundle.newBuilder().setModule("test1").build())
    bundle.putScripts(
      "test2",
      ScriptBundle
        .newBuilder()
        .setModule("test2")
        .setDependencies(
          AssetDependencies.newBuilder()
            .addDirect("test1")
        )
        .build()
    )
    val (_, indexer) = createIndexer(bundle.build())
    val indexes = assertDoesNotThrow {
      indexer.buildAssetIndexes(
        bundle.build()
      )
    }
    assertNotNull(indexes)
    val (depGraph, manifest) = indexes
    assertNotNull(depGraph)
    assertNotNull(manifest)
    assertTrue(depGraph.nodes().isNotEmpty())
    assertTrue(depGraph.nodes().contains("test1"))
    assertTrue(depGraph.nodes().contains("test2"))
    assertTrue(depGraph.edges().isNotEmpty())
    assertTrue(
      depGraph.edges().contains(
        AssetDependency(
          depender = "test2",
          dependee = "test1",
          optional = false,
        )
      )
    )
    assertTrue(
      depGraph.hasEdgeConnecting("test2", "test1")
    )
    assertTrue(
      depGraph.adjacentNodes("test2").contains("test1")
    )
    assertTrue(
      depGraph.outEdges("test2").isNotEmpty()
    )
    assertTrue(
      depGraph.outEdges("test2").contains(
        AssetDependency(
          depender = "test2",
          dependee = "test1",
          optional = false,
        )
      )
    )
    assertTrue(
      depGraph.inEdges("test1").contains(
        AssetDependency(
          depender = "test2",
          dependee = "test1",
          optional = false,
        )
      )
    )
  }

  @Test fun testGenerateWithStyleDeps() {
    val bundle = AssetBundle.newBuilder()
    bundle.addAsset(AssetContent.newBuilder().setModule("test1").setToken("abc123").addVariant(identityVariant()))
    bundle.addAsset(AssetContent.newBuilder().setModule("test2").setToken("abc123").addVariant(identityVariant()))
    bundle.putStyles("test1", StyleBundle.newBuilder().setModule("test1").build())
    bundle.putStyles(
      "test2",
      StyleBundle
        .newBuilder()
        .setModule("test2")
        .setDependencies(
          AssetDependencies.newBuilder()
            .addDirect("test1")
        )
        .build()
    )
    val (_, indexer) = createIndexer(bundle.build())
    val indexes = assertDoesNotThrow {
      indexer.buildAssetIndexes(
        bundle.build()
      )
    }
    assertNotNull(indexes)
    val (depGraph, manifest) = indexes
    assertNotNull(depGraph)
    assertNotNull(manifest)
    assertTrue(depGraph.nodes().isNotEmpty())
    assertTrue(depGraph.nodes().contains("test1"))
    assertTrue(depGraph.nodes().contains("test2"))
    assertTrue(depGraph.edges().isNotEmpty())
    assertTrue(
      depGraph.edges().contains(
        AssetDependency(
          depender = "test2",
          dependee = "test1",
          optional = false,
        )
      )
    )
    assertTrue(
      depGraph.hasEdgeConnecting("test2", "test1")
    )
    assertTrue(
      depGraph.adjacentNodes("test2").contains("test1")
    )
    assertTrue(
      depGraph.outEdges("test2").isNotEmpty()
    )
    assertTrue(
      depGraph.outEdges("test2").contains(
        AssetDependency(
          depender = "test2",
          dependee = "test1",
          optional = false,
        )
      )
    )
    assertTrue(
      depGraph.inEdges("test1").contains(
        AssetDependency(
          depender = "test2",
          dependee = "test1",
          optional = false,
        )
      )
    )
  }

  @Test fun testGenerateEtagsStrong() {
    // standard config
    val (sample, indexer) = createIndexer(
      config = object : AssetConfig {
        override fun isEnabled(): Boolean = true
        override val etags: Boolean get() = true
        override val preferWeakEtags: Boolean get() = false
      }
    )
    assertDoesNotThrow {
      indexer.initialize()
    }

    // generate an etag
    val etag = indexer.buildETagForAsset(sample.getAsset(0))
    assertNotNull(etag, "generated etag value should not be null")
    assertTrue(etag.startsWith("\""), "strong etag should start with a double-quote")
    assertTrue(etag.endsWith("\""), "strong etag should end with a double-quote")
  }

  @Test fun testGenerateEtagsWeak() {
    // standard config
    val (sample, indexer) = createIndexer(
      config = object : AssetConfig {
        override fun isEnabled(): Boolean = true
        override val etags: Boolean get() = true
        override val preferWeakEtags: Boolean get() {
          return true
        }
      }
    )
    assertDoesNotThrow {
      indexer.initialize()
    }

    // generate an etag
    val etag = indexer.buildETagForAsset(sample.getAsset(0))
    assertNotNull(etag, "generated etag value should not be null")
    assertTrue(etag.startsWith("W/\""), "weak etag should start with `W/\"`")
    assertTrue(etag.endsWith("\""), "weak etag should end with a double-quote")
  }

  @Test fun testRenderConditionalStrongETagMatch() {
    // standard config
    val cfg = object : AssetConfig {
      override fun isEnabled(): Boolean = true
      override val etags: Boolean get() = true
      override val preferWeakEtags: Boolean get() = false
    }
    val (sample, indexer) = createIndexer(config = cfg)
    assertDoesNotThrow {
      indexer.initialize()
    }

    // generate an etag
    val etag = indexer.buildETagForAsset(sample.getAsset(0))
    assertNotNull(etag, "generated etag value should not be null")
    assertTrue(etag.startsWith("\""), "strong etag should start with `\"`")
    assertTrue(etag.endsWith("\""), "strong etag should end with a double-quote")

    // forge a request with a matching etag, make sure it matches
    val req = HttpRequest.GET<Any>("/_/assets/some-asset-url.css")
      .header(HttpHeaders.IF_NONE_MATCH, etag)

    val reader = object : AssetReader {
      override suspend fun readAsync(descriptor: ServerAsset, request: HttpRequest<*>): Deferred<RenderedAsset> {
        error("should not be called")
      }

      override fun pointerTo(moduleId: AssetModuleId): AssetPointer? {
        error("should not be called")
      }

      override fun findByModuleId(moduleId: AssetModuleId): ServerAsset? {
        error("should not be called")
      }

      override fun resolve(path: String): ServerAsset? {
        error("should not be called")
      }
    }

    val descriptor = sample.stylesMap[sample.stylesMap.keys.first()]!!
    val baseStyleAsset = ServerAsset.Stylesheet(
      descriptor = descriptor,
      sortedSetOf(
        List(
          sample.assetList.filter {
            it.module == descriptor.module
          }.size
        ) { idx -> idx }.first()
      )
    )
    val response = assertDoesNotThrow {
      runBlocking {
        ServerAssetManager(cfg, indexer, reader).renderAssetAsync(
          req,
          baseStyleAsset,
        ).await()
      }
    }
    assertNotNull(response, "should not get `null` response from strong conditional etag match")
    assertEquals(304, response.status.code, "should get HTTP 200 from conditional etag match")
  }

  @Test fun testRenderConditionalStrongETagMismatch() {
    // standard config
    val cfg =object : AssetConfig {
      override fun isEnabled(): Boolean = true
      override val etags: Boolean get() = true
      override val preferWeakEtags: Boolean get() = false
    }
    val (sample, indexer) = createIndexer(config = cfg)
    assertDoesNotThrow {
      indexer.initialize()
    }

    // generate an etag
    val etag = indexer.buildETagForAsset(sample.getAsset(0))
    assertNotNull(etag, "generated etag value should not be null")
    assertTrue(etag.startsWith("\""), "strong etag should start with `\"`")
    assertTrue(etag.endsWith("\""), "strong etag should end with a double-quote")

    // forge a request with a matching etag, make sure it DOES NOT match
    val req = HttpRequest.GET<Any>("/_/assets/some-asset-url.css")
      .header(HttpHeaders.IF_NONE_MATCH, "some-other-etag-value")

    class ItMismatched : RuntimeException()

    val reader = object : AssetReader {
      override suspend fun readAsync(descriptor: ServerAsset, request: HttpRequest<*>): Deferred<RenderedAsset> {
        throw ItMismatched()
      }

      override fun pointerTo(moduleId: AssetModuleId): AssetPointer? {
        error("should not be called")
      }

      override fun findByModuleId(moduleId: AssetModuleId): ServerAsset? {
        error("should not be called")
      }

      override fun resolve(path: String): ServerAsset? {
        error("should not be called")
      }
    }

    val descriptor = sample.stylesMap[sample.stylesMap.keys.first()]!!
    val baseStyleAsset = ServerAsset.Stylesheet(
      descriptor = descriptor,
      sortedSetOf(
        List(
          sample.assetList.filter {
            it.module == descriptor.module
          }.size
        ) { idx -> idx }.first()
      )
    )
    assertThrows<ItMismatched> {
      runBlocking {
        ServerAssetManager(cfg, indexer, reader).renderAssetAsync(
          req,
          baseStyleAsset,
        ).await()
      }
    }
  }

  @Test fun testRenderConditionalWeakETagMatch() {
    // standard config
    val cfg = object : AssetConfig {
      override fun isEnabled(): Boolean = true
      override val etags: Boolean get() = true
      override val preferWeakEtags: Boolean get() = true
    }
    val (sample, indexer) = createIndexer(config = cfg)
    assertDoesNotThrow {
      indexer.initialize()
    }

    // generate an etag
    val etag = indexer.buildETagForAsset(sample.getAsset(0))
    assertNotNull(etag, "generated etag value should not be null")
    assertTrue(etag.startsWith("W/\""), "weak etag should start with `W/\"`")
    assertTrue(etag.endsWith("\""), "weak etag should end with a double-quote")

    // forge a request with a matching weak etag, make sure it matches
    val req = HttpRequest.GET<Any>("/_/assets/some-asset-url.css")
      .header(HttpHeaders.IF_NONE_MATCH, etag)

    val reader = object : AssetReader {
      override suspend fun readAsync(descriptor: ServerAsset, request: HttpRequest<*>): Deferred<RenderedAsset> {
        error("should not be called")
      }

      override fun pointerTo(moduleId: AssetModuleId): AssetPointer? {
        error("should not be called")
      }

      override fun findByModuleId(moduleId: AssetModuleId): ServerAsset? {
        error("should not be called")
      }

      override fun resolve(path: String): ServerAsset? {
        error("should not be called")
      }
    }

    val descriptor = sample.stylesMap[sample.stylesMap.keys.first()]!!
    val baseStyleAsset = ServerAsset.Stylesheet(
      descriptor = descriptor,
      sortedSetOf(
        List(
          sample.assetList.filter {
            it.module == descriptor.module
          }.size
        ) { idx -> idx }.first()
      )
    )
    val response = assertDoesNotThrow {
      runBlocking {
        ServerAssetManager(cfg, indexer, reader).renderAssetAsync(
          req,
          baseStyleAsset,
        ).await()
      }
    }
    assertNotNull(response, "should not get `null` response from strong conditional etag match")
    assertEquals(304, response.status.code, "should get HTTP 200 from conditional etag match")
  }

  @Test fun testRenderConditionalWeakETagMismatch() {
    // standard config
    val cfg = object : AssetConfig {
      override fun isEnabled(): Boolean = true
      override val etags: Boolean get() = true
      override val preferWeakEtags: Boolean get() = true
    }
    val (sample, indexer) = createIndexer(config = cfg)
    assertDoesNotThrow {
      indexer.initialize()
    }

    // generate an etag
    val etag = indexer.buildETagForAsset(sample.getAsset(0))
    assertNotNull(etag, "generated etag value should not be null")
    assertTrue(etag.startsWith("W/\""), "weak etag should start with `W/\"`")
    assertTrue(etag.endsWith("\""), "weak etag should end with a double-quote")

    // forge a request with a matching weak etag, make sure it matches
    val req = HttpRequest.GET<Any>("/_/assets/some-asset-url.css")
      .header(HttpHeaders.IF_NONE_MATCH, "W/\"123123123\"")

    class ItMismatched : RuntimeException()

    val reader = object : AssetReader {
      override suspend fun readAsync(descriptor: ServerAsset, request: HttpRequest<*>): Deferred<RenderedAsset> {
        throw ItMismatched()
      }

      override fun pointerTo(moduleId: AssetModuleId): AssetPointer? {
        error("should not be called")
      }

      override fun findByModuleId(moduleId: AssetModuleId): ServerAsset? {
        error("should not be called")
      }

      override fun resolve(path: String): ServerAsset? {
        error("should not be called")
      }
    }

    val descriptor = sample.stylesMap[sample.stylesMap.keys.first()]!!
    val baseStyleAsset = ServerAsset.Stylesheet(
      descriptor = descriptor,
      sortedSetOf(
        List(
          sample.assetList.filter {
            it.module == descriptor.module
          }.size
        ) { idx -> idx }.first()
      )
    )
    assertThrows<ItMismatched> {
      runBlocking {
        ServerAssetManager(cfg, indexer, reader).renderAssetAsync(
          req,
          baseStyleAsset,
        ).await()
      }
    }
  }

  @Test fun testRenderConditionalWeakETagBadFormat() {
    // standard config
    val cfg = object : AssetConfig {
      override fun isEnabled(): Boolean = true
      override val etags: Boolean get() = true
      override val preferWeakEtags: Boolean get() = true
    }
    val (sample, indexer) = createIndexer(config = cfg)
    assertDoesNotThrow {
      indexer.initialize()
    }

    // generate an etag
    val etag = indexer.buildETagForAsset(sample.getAsset(0))
    assertNotNull(etag, "generated etag value should not be null")
    assertTrue(etag.startsWith("W/\""), "weak etag should start with `W/\"`")
    assertTrue(etag.endsWith("\""), "weak etag should end with a double-quote")

    // forge a request with a matching weak etag, make sure it matches
    val req = HttpRequest.GET<Any>("/_/assets/some-asset-url.css")
      .header(HttpHeaders.IF_NONE_MATCH, "W/\"lololol\"")

    class ItMismatched : RuntimeException()

    val reader = object : AssetReader {
      override suspend fun readAsync(descriptor: ServerAsset, request: HttpRequest<*>): Deferred<RenderedAsset> {
        throw ItMismatched()
      }

      override fun pointerTo(moduleId: AssetModuleId): AssetPointer? {
        error("should not be called")
      }

      override fun findByModuleId(moduleId: AssetModuleId): ServerAsset? {
        error("should not be called")
      }

      override fun resolve(path: String): ServerAsset? {
        error("should not be called")
      }
    }

    val descriptor = sample.stylesMap[sample.stylesMap.keys.first()]!!
    val baseStyleAsset = ServerAsset.Stylesheet(
      descriptor = descriptor,
      sortedSetOf(
        List(
          sample.assetList.filter {
            it.module == descriptor.module
          }.size
        ) { idx -> idx }.first()
      )
    )
    assertThrows<ItMismatched> {
      runBlocking {
        ServerAssetManager(cfg, indexer, reader).renderAssetAsync(
          req,
          baseStyleAsset,
        ).await()
      }
    }
  }

  @Test fun testRenderConditionalWeakETagMatchInStrongMode() {
    val (sample, indexerWithWeakEtags) = createIndexer(
      config = object : AssetConfig {
        override fun isEnabled(): Boolean = true
        override val etags: Boolean get() = true
        override val preferWeakEtags: Boolean get() = true
      }
    )
    assertDoesNotThrow {
      indexerWithWeakEtags.initialize()
    }

    // generate an etag
    val injectedEtag = indexerWithWeakEtags.buildETagForAsset(sample.getAsset(0))
    assertNotNull(injectedEtag, "generated etag value should not be null")
    assertTrue(injectedEtag.startsWith("W/\""), "weak etag should start with `W/\"`")
    assertTrue(injectedEtag.endsWith("\""), "weak etag should end with a double-quote")

    // standard config
    val cfg = object : AssetConfig {
      override fun isEnabled(): Boolean = true
      override val etags: Boolean get() = true
      override val preferWeakEtags: Boolean get() = false  // important
    }
    val (_, indexer) = createIndexer(sample, config = cfg)
    assertDoesNotThrow {
      indexer.initialize()
    }

    // generate an etag
    val etag = indexer.buildETagForAsset(sample.getAsset(0))
    assertNotNull(etag, "generated etag value should not be null")
    assertTrue(etag.startsWith("\""), "strong etag should start with `\"`")
    assertTrue(etag.endsWith("\""), "strong etag should end with a double-quote")

    // forge a request with a matching weak etag, make sure it matches
    val req = HttpRequest.GET<Any>("/_/assets/some-asset-url.css")
      .header(HttpHeaders.IF_NONE_MATCH, injectedEtag)

    val reader = object : AssetReader {
      override suspend fun readAsync(descriptor: ServerAsset, request: HttpRequest<*>): Deferred<RenderedAsset> {
        error("should not be called")
      }

      override fun pointerTo(moduleId: AssetModuleId): AssetPointer? {
        error("should not be called")
      }

      override fun findByModuleId(moduleId: AssetModuleId): ServerAsset? {
        error("should not be called")
      }

      override fun resolve(path: String): ServerAsset? {
        error("should not be called")
      }
    }

    val descriptor = sample.stylesMap[sample.stylesMap.keys.first()]!!
    val baseStyleAsset = ServerAsset.Stylesheet(
      descriptor = descriptor,
      sortedSetOf(
        List(
          sample.assetList.filter {
            it.module == descriptor.module
          }.size
        ) { idx -> idx }.first()
      )
    )
    val response = assertDoesNotThrow {
      runBlocking {
        ServerAssetManager(cfg, indexer, reader).renderAssetAsync(
          req,
          baseStyleAsset,
        ).await()
      }
    }
    assertNotNull(response, "should not get `null` response from strong conditional etag match")
    assertEquals(304, response.status.code, "should get HTTP 200 from conditional etag match")
  }

  @Test fun testGenerateIndexesTextAssets() {
    val bundle = AssetBundle.newBuilder()
    bundle.addAsset(AssetContent.newBuilder().setModule("test1").setToken("abc123").addVariant(identityVariant()))
    bundle.putGeneric("test1", GenericBundle.newBuilder().setModule("test1").build())
    val (_, indexer) = createIndexer(bundle.build())
    val indexes = assertDoesNotThrow {
      indexer.buildAssetIndexes(
        bundle.build()
      )
    }
    assertNotNull(indexes)
    val (depGraph, manifest) = indexes
    assertNotNull(depGraph)
    assertNotNull(manifest)
    assertTrue(manifest.moduleIndex.contains("test1"))
  }

  @Test fun testFailCompletelyGenericAsset() {
    val bundle = AssetBundle.newBuilder().build()
    val (_, indexer) = createIndexer(bundle)
    val builder: ImmutableNetwork.Builder<AssetModuleId, AssetDependency> = NetworkBuilder
      .directed()
      .allowsParallelEdges(false)
      .allowsSelfLoops(false)
      .nodeOrder(ElementOrder.stable<AssetModuleId>())
      .edgeOrder(ElementOrder.stable<AssetDependency>())
      .immutable()

    assertThrows<IllegalStateException> {
      indexer.pointerForConcrete(
        AssetType.GENERIC,
        "some-module",
        AssetContent
          .newBuilder()
          .setToken("token-value-here")
          .setModule("module-id")
          .setFilename("filename.data")
          .addVariant(identityVariant())
          .build(),
        sortedSetOf(5),
        bundle,
        builder,
      )
    }
  }

  @Test fun testAssetIndexBoot() {
    val (sample, indexer) = createIndexer()
    assertNotNull(sample)
    assertNotNull(indexer)
    assertDoesNotThrow {
      indexer.initialize()
    }
    assertTrue(indexer.initialized.get())
    assertNotNull(indexer.assetManifest.get())
    assertNotNull(indexer.dependencyGraph.get())
  }

  @Test fun testAssetIndexerInitializeTwiceDoesNotThrow() {
    val (sample, indexer) = createIndexer()
    assertNotNull(sample)
    assertNotNull(indexer)
    assertDoesNotThrow {
      indexer.initialize()
    }
    assertDoesNotThrow {
      indexer.initialize()
    }
  }

  @Test fun testAssetIndexerDoesNotInitializeMoreThanOnce() {
    val firstCall = AtomicBoolean(true)
    val indexer = ServerAssetIndex(
      object : AssetConfig {},
      object : AssetManifestLoader {
        override fun findLoadManifest(candidates: List<Pair<ManifestFormat, String>>): AssetBundle? {
          if (firstCall.get()) {
            return null
          } else {
            throw IllegalStateException("FAIL")
          }
        }

        override fun findManifest(candidates: List<Pair<ManifestFormat, String>>): Pair<ManifestFormat, InputStream>? {
          if (firstCall.get()) {
            return null
          } else {
            throw IllegalStateException("FAIL")
          }
        }
      }
    )
    assertNotNull(indexer)
    assertDoesNotThrow {
      indexer.initialize()
    }
    assertDoesNotThrow {
      indexer.initialize()
    }
  }

  @Test fun testAssetIndexBootNoManifest() {
    val indexer = ServerAssetIndex(
      object : AssetConfig {},
      object : AssetManifestLoader {
        override fun findLoadManifest(candidates: List<Pair<ManifestFormat, String>>): AssetBundle? {
          return null
        }

        override fun findManifest(candidates: List<Pair<ManifestFormat, String>>): Pair<ManifestFormat, InputStream>? {
          return null
        }
      }
    )

    assertDoesNotThrow {
      indexer.initialize()
    }
    assertTrue(indexer.initialized.get())
    assertNull(indexer.assetManifest.get())
    assertNull(indexer.dependencyGraph.get())
  }

  @Test fun testInterpretedManifest() {
    val (sample, indexer) = createIndexer()
    assertDoesNotThrow {
      indexer.initialize()
    }
    val manifest = indexer.assetManifest.get()
    assertNotNull(manifest)
    assertNotNull(manifest.bundle)
    assertNotNull(manifest.moduleIndex)
    assertNotNull(manifest.tagIndex)
    assertTrue(manifest.bundle.isInitialized)
    assertTrue(manifest.moduleIndex.isNotEmpty())
    assertTrue(manifest.tagIndex.isNotEmpty())
    assertThat(sample).isEqualTo(manifest.bundle)
  }

  @Test fun testLookupAssetByTagScript() {
    val bundle = AssetBundle.newBuilder()
    bundle.addAsset(AssetContent.newBuilder().setModule("test1").setToken("abc123").addVariant(identityVariant()))
    bundle.putScripts("test1", ScriptBundle.newBuilder().setModule("test1").build())
    val target = bundle.build()
    val (_, indexer) = createIndexer(target)
    assertDoesNotThrow {
      indexer.initialize()
    }
    val resolved = assertDoesNotThrow {
      indexer.resolveByTag("abc123")
    }
    assertNotNull(
      resolved,
      "should be able to resolve known-good script by tag"
    )
    assertTrue(
      resolved is ServerAsset.Script,
      "resolved asset should be the right sub-type"
    )
  }

  @Test fun testLookupAssetByTagStyle() {
    val bundle = AssetBundle.newBuilder()
    bundle.addAsset(AssetContent.newBuilder().setModule("test1").setToken("abc123").addVariant(identityVariant()))
    bundle.putStyles("test1", StyleBundle.newBuilder().setModule("test1").build())
    val target = bundle.build()
    val (_, indexer) = createIndexer(target)
    assertDoesNotThrow {
      indexer.initialize()
    }
    val resolved = assertDoesNotThrow {
      indexer.resolveByTag("abc123")
    }
    assertNotNull(
      resolved,
      "should be able to resolve known-good stylesheet by tag"
    )
    assertTrue(
      resolved is ServerAsset.Stylesheet,
      "resolved asset should be the right sub-type"
    )
  }

  @Test fun testLookupAssetByTagText() {
    val bundle = AssetBundle.newBuilder()
    bundle.addAsset(AssetContent.newBuilder().setModule("test1").setToken("abc123").addVariant(identityVariant()))
    bundle.putGeneric("test1", GenericBundle.newBuilder().setModule("test1").build())
    val target = bundle.build()
    val (_, indexer) = createIndexer(target)
    assertDoesNotThrow {
      indexer.initialize()
    }
    val resolved = assertDoesNotThrow {
      indexer.resolveByTag("abc123")
    }
    assertNotNull(
      resolved,
      "should be able to resolve known-good text asset by tag"
    )
    assertTrue(
      resolved is ServerAsset.Text,
      "resolved asset should be the right sub-type"
    )
  }

  @Test fun testLookupAssetByTagNotFound() {
    val bundle = AssetBundle.newBuilder()
    bundle.addAsset(AssetContent.newBuilder().setModule("test1").setToken("abc123").addVariant(identityVariant()))
    bundle.putGeneric("test1", GenericBundle.newBuilder().setModule("test1").build())
    val target = bundle.build()
    val (_, indexer) = createIndexer(target)
    assertDoesNotThrow {
      indexer.initialize()
    }
    val resolved = assertDoesNotThrow {
      indexer.resolveByTag("abc124")
    }
    assertNull(
      resolved,
      "should NOT be able to resolve known-bad asset by tag"
    )
  }

  @Test fun testFailBuildConcreteGeneric() {
    val bundle = AssetBundle.newBuilder()
    bundle.addAsset(AssetContent.newBuilder().setModule("test1").setToken("abc123").addVariant(identityVariant()))
    bundle.putGeneric("test1", GenericBundle.newBuilder().setModule("test1").build())
    val target = bundle.build()
    val (_, indexer) = createIndexer(target)
    assertDoesNotThrow {
      indexer.initialize()
    }
    assertThrows<IllegalStateException> {
      indexer.buildConcreteAsset(
        AssetType.GENERIC,
        "some-module-id",
        target,
        null,
      )
    }
  }

  @Test fun testInitializeBeforeAccessingManifest() {
    val (_, indexer) = createIndexer()
    assertThrows<IllegalArgumentException> {
      indexer.activeManifest()
    }
    assertDoesNotThrow {
      assertNull(indexer.resolveByTag("sample-tag", timeoutSeconds = 1L))
    }
    assertDoesNotThrow {
      indexer.initialize()
    }
    assertDoesNotThrow {
      indexer.activeManifest()
    }
  }

  @Test fun addDirectDepsTest() {
    // build a deliberately incorrect graph
    val (_, indexer) = createIndexer()
    val builder: ImmutableNetwork.Builder<AssetModuleId, AssetDependency> = NetworkBuilder
      .directed()
      .allowsParallelEdges(false)
      .allowsSelfLoops(false)
      .nodeOrder(ElementOrder.stable<AssetModuleId>())
      .edgeOrder(ElementOrder.stable<AssetDependency>())
      .immutable()

    indexer.addDirectDeps(
      "should-be-there",
      builder,
      AssetDependencies.getDefaultInstance(),
    )
    indexer.addDirectDeps(
      "should-also-be-there",
      builder,
      AssetDependencies.getDefaultInstance(),
    )
    indexer.addDirectDeps(
      "should-not-be-there",
      builder,
      AssetDependencies.getDefaultInstance(),
    )
    indexer.addDirectDeps(
      "some-module-id",
      builder,
      AssetDependencies
        .newBuilder()
        .addDirect("should-be-there")
        .addDirect("should-also-be-there")
        .addTransitive("should-not-be-there")
        .build()
    )
    val depGraph = builder.build()
    assertTrue(depGraph.nodes().isNotEmpty())
    assertTrue(depGraph.nodes().contains("should-be-there"))
    assertTrue(depGraph.nodes().contains("should-also-be-there"))
    assertTrue(depGraph.nodes().contains("some-module-id"))
    assertTrue(depGraph.edges().contains(AssetDependency("some-module-id", "should-be-there")))
    assertTrue(depGraph.edges().contains(AssetDependency("some-module-id", "should-also-be-there")))
    assertFalse(depGraph.edges().contains(AssetDependency("some-module-id", "should-not-be-there")))
    assertTrue(depGraph.edgesConnecting("some-module-id", "should-not-be-there").isEmpty())
  }
}

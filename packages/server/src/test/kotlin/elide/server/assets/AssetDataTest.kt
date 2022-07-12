package elide.server.assets

import com.google.protobuf.ByteString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.assertThrows
import tools.elide.crypto.HashAlgorithm
import tools.elide.data.CompressionMode
import java.nio.charset.StandardCharsets
import kotlin.test.*

/** Tests for data structures and utilities which implement server-side asset serving. */
class AssetDataTest {
  @Test fun testAssetType() {
    AssetType.values().forEach {
      assertNotNull(it.name)
      if (it != AssetType.GENERIC) {
        assertNotNull(it.mediaType)
      }
    }
  }

  @Test fun testAssetPointerWithoutIndex() {
    val pointer = AssetPointer(
      moduleId = "some-module",
      type = AssetType.SCRIPT,
      token = "some-token-some-tag",
      tag = "some-tag",
      etag = "W/\"some-etag\"",
      modified = 123L,
      index = null,
    )
    assertNull(
      pointer.index,
    )
  }

  @Test fun testAssetPointer() {
    val pointer = AssetPointer(
      moduleId = "some-module",
      type = AssetType.SCRIPT,
      token = "some-token-some-tag",
      tag = "some-tag",
      etag = "W/\"some-etag\"",
      modified = 123L,
      index = sortedSetOf(5),
    )
    assertEquals("some-module", pointer.moduleId)
    assertEquals("some-token-some-tag", pointer.token)
    assertEquals("some-tag", pointer.tag)
    assertEquals(AssetType.SCRIPT, pointer.type)
    assertEquals(5, pointer.index!!.first())
    assertEquals(pointer, pointer)
    assertEquals(pointer, pointer.copy())
    assertNotNull(pointer.hashCode())
    assertEquals("W/\"some-etag\"", pointer.etag)
    assertEquals(123L, pointer.modified)
    assertTrue(pointer.toString().contains("some-module"))
  }

  @Test fun testAssetDependency() {
    val nonOptional = AssetDependency("test1", "test2", optional = false)
    assertEquals("test1", nonOptional.depender)
    assertEquals("test2", nonOptional.dependee)
    assertFalse(nonOptional.optional)
    assertEquals(nonOptional, nonOptional)
    assertEquals(nonOptional, nonOptional.copy())
    val optional = AssetDependency("test1", "test2", optional = true)
    assertEquals("test1", optional.depender)
    assertEquals("test2", optional.dependee)
    assertTrue(optional.optional)
    assertEquals(optional, optional)
    assertEquals(optional, optional.copy())
  }

  @Test fun testAssetDependencyDefault() {
    val default = AssetDependency("test1", "test2")
    assertEquals("test1", default.depender)
    assertEquals("test2", default.dependee)
    assertFalse(default.optional)
    assertEquals(default, default)
    assertEquals(default, default.copy())
  }

  @Test fun testAssetDependencyCannotReferenceItself() {
    assertThrows<IllegalArgumentException> {
      AssetDependency("test", "test", optional = false)
    }
  }

  @Test fun testRenderedAsset() {
    val asset = RenderedAsset(
      module = "some-module",
      type = AssetType.SCRIPT,
      variant = CompressionMode.GZIP,
      headers = emptyMap(),
      size = 123L,
      lastModified = 124L,
      digest = HashAlgorithm.SHA256 to ByteString.copyFrom(ByteArray(0)),
    ) { ByteString.copyFrom("hello world".toByteArray(StandardCharsets.UTF_8)) }
    assertNotNull(asset)
    assertNotNull(asset.module)
    assertNotNull(asset.type)
    assertNotNull(asset.variant)
    assertNotNull(asset.headers)
    assertNotNull(asset.size)
    assertNotNull(asset.lastModified)
    assertNotNull(asset.digest)
    assertEquals(asset, asset)
    assertNotNull(asset.hashCode())
  }

  @Test fun testAssetReferenceDefaults() {
    val ref = AssetReference(
      module = "some-module",
      assetType = AssetType.STYLESHEET,
      href = "/_/assets/some-path.css",
    )
    assertEquals(ref.module, "some-module")
    assertEquals(ref.assetType, AssetType.STYLESHEET)
    assertEquals(ref.href, "/_/assets/some-path.css")

    // type should be `null` and `inline` should be not be `null` by default.
    assertNull(ref.type)
    assertNotNull(ref.inline)
    assertNotNull(ref.preload)
  }

  @Test fun testAssetReference() {
    val ref = AssetReference(
      module = "some-module",
      assetType = AssetType.STYLESHEET,
      href = "/_/assets/some-path.css",
      type = "type-override",
      inline = true,
    )
    assertEquals(ref.module, "some-module")
    assertEquals(ref.assetType, AssetType.STYLESHEET)
    assertEquals(ref.href, "/_/assets/some-path.css")
    assertEquals(ref.type, "type-override")
    assertTrue(ref.inline)
    assertNotNull(AssetReference.serializer())
    assertEquals(ref, ref)
    assertEquals(ref, ref.copy())
    val json = Json.encodeToString(AssetReference.serializer(), ref)
    assertNotNull(json)
    val inflated = Json.decodeFromString(AssetReference.serializer(), json)
    assertNotNull(inflated)
    assertEquals(ref, inflated)
  }

  @Test fun testAssetReferenceFromPointer() {
    val pointer = AssetPointer(
      moduleId = "some-module",
      type = AssetType.SCRIPT,
      token = "some-token-some-tag",
      tag = "some-tag",
      etag = "W/\"some-etag\"",
      modified = 123L,
      index = sortedSetOf(5),
    )
    val reference = AssetReference.fromPointer(
      pointer,
      "/_/assets/some-uri.js"
    )
    assertNotNull(reference)
    assertEquals(reference.module, "some-module")
    assertEquals(reference.assetType, AssetType.SCRIPT)
  }
}

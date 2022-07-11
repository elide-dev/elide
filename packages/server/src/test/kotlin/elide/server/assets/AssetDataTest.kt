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
      index = sortedSetOf(5),
    )
    assertEquals("some-module", pointer.moduleId)
    assertEquals(AssetType.SCRIPT, pointer.type)
    assertEquals(5, pointer.index!!.first())
    assertEquals(pointer, pointer)
    assertEquals(pointer, pointer.copy())
    assertNotNull(pointer.hashCode())
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
}

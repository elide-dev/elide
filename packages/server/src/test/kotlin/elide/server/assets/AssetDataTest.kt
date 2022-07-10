package elide.server.assets

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.assertThrows
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
      index = 5,
    )
    assertEquals("some-module", pointer.moduleId)
    assertEquals(AssetType.SCRIPT, pointer.type)
    assertEquals(5, pointer.index)
    assertNotNull(AssetPointer.serializer())
    assertEquals(pointer, pointer)
    assertEquals(pointer, pointer.copy())
    assertNotNull(pointer.hashCode())
    assertTrue(pointer.toString().contains("some-module"))
    val serialized = Json.encodeToString(
      AssetPointer.serializer(),
      pointer,
    )
    val deserialized = Json.decodeFromString(
      AssetPointer.serializer(),
      serialized,
    )
    assertEquals(pointer, deserialized)
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
}

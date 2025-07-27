package sample

import kotlinx.serialization.json.Json
import kotlin.test.*

class SampleTest {
  @Test fun `greeting is expected value`() {
    val greeting = renderGreeting()
    assertNotNull(greeting)
    assertEquals("Hello World", greeting)
  }

  @Test fun `kotlinx serialization works`() {
    val data = assertNotNull(renderData()).copy(name = "test")
    val encoded = assertNotNull(Json.encodeToString(data))
    assertTrue(encoded.isNotEmpty())
    assertTrue("\"test\"" in encoded)
    val decoded = assertNotNull(Json.decodeFromString<MyCoolData>(encoded))
    assertEquals("test", decoded.name)
  }

  @Test fun `redaction works`() {
    val data = assertNotNull(renderData()).copy(name = "test")
    val stringForm = assertNotNull(data.toString())
    assertFalse("test" in stringForm)
  }
}

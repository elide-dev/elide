package sample

import kotlin.test.*
import kotlinx.serialization.json.Json

class SampleTest {
  private fun decodeJson(json: String): Message {
    return Json.decodeFromString(json)
  }

  @Test fun testGreeting() {
    val json = renderSomeJson()
    val obj = decodeJson(json)
    assertNotNull(obj)
    assertEquals("Hello, World", obj.text)
  }
}

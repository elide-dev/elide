package elide.runtime.plugins.js

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine

@OptIn(DelicateElideApi::class)
internal class JsPluginTest {
  @Test fun testExecution() {
    val engine = PolyglotEngine { install(JavaScript) }
    val context = engine.acquire()
    
    val result = context.javaScript("""
    const a = 42
    function getValue() { return a; }
    
    getValue()
    """.trimIndent())
    
    assertEquals(
      expected = 42,
      actual = result.asInt(),
      message = "should return correct value",
    )
  }
}

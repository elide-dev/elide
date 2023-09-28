package elide.runtime.plugins.python

import org.junit.jupiter.api.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine

@OptIn(DelicateElideApi::class)
internal class PythonPluginTest {
  @Test fun testExecution() {
    val engine = PolyglotEngine { install(Python) }
    val context = engine.acquire()
    
    val result = context.python("""
    a = 42
    def getValue():
      return a
    
    getValue()
    """.trimIndent())
    
    assertEquals(
      expected = 42,
      actual = result.asInt(),
      message = "should return correct value",
    )
  }
}

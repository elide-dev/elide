package elide.runtime.plugins.ruby

import org.junit.jupiter.api.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine

@OptIn(DelicateElideApi::class)
internal class RubyPluginTest {
  @Ignore @Test fun testExecution() {
    val engine = PolyglotEngine { install(Ruby) }
    val context = engine.acquire()
    
    val result = context.ruby("""
    def getValue()
      return 42
    end
    
    getValue()
    """.trimIndent())
    
    assertEquals(
      expected = 42,
      actual = result.asInt(),
      message = "should return correct value",
    )
  }
}

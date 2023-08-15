package elide.runtime.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

@OptIn(DelicateElideApi::class)
class ContextTest {
  /** Simple mock implementation for a JS language marker. */
  private object JavaScript : GuestLanguage {
    override val languageId: String = "js"
  }

  /** Simple mock implementation for a Python language marker. */
  private object Python : GuestLanguage {
    override val languageId: String = "python"
  }

  @Test fun testEnableLanguage() {
    val context = PolyglotEngine {
      // explicitly enable Python, but not JS
      enableLanguage(Python)
    }.acquire()

    // language=javascript
    val code = """
    const hello = () => "Hello"
    hello()
    """

    assertThrows<IllegalArgumentException> {
      context.execute(JavaScript, code)
    }
  }

  @Test fun testExecute() {
    val context = PolyglotEngine {
      // explicitly enable JS support
      enableLanguage(JavaScript)
    }.acquire()

    // language=javascript
    val code = """
    function getMessage(name) {
      return "Hello, " + name
    }
    
    getMessage("Elide")
    """
    
    assertEquals(
      expected = "Hello, Elide",
      actual = context.execute(JavaScript, code).asString(),
    )
  }
}
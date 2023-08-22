package elide.runtime.core

import org.graalvm.polyglot.PolyglotException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random
import kotlin.test.assertEquals

@OptIn(DelicateElideApi::class)
internal class ContextTest {
  /** Simple mock implementation for a JS language marker. */
  private object JavaScript : GuestLanguage {
    override val languageId: String = "js"
  }

  /** Simple mock implementation for a Python language marker. */
  private object Python : GuestLanguage {
    override val languageId: String = "python"
  }

  @Test fun testIntrinsicBindings() {
    val context = PolyglotEngine {
      enableLanguage(Python)
      enableLanguage(JavaScript)
    }.acquire()
    val (jsKey, jsValue) = "jsTestBinding" to Random.nextInt()

    // set a language-scoped binding
    context.bindings(language = JavaScript).putMember(jsKey, jsValue)

    assertEquals(
      expected = jsValue,
      actual = context.evaluate(language = JavaScript, source = jsKey).asInt(),
      message = "language-scoped binding should be accessible in target language",
    )

    assertThrows<PolyglotException>("language-scoped binding should not be accessible to other languages") {
      context.evaluate(language = Python, source = jsKey)
    }
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
      context.evaluate(JavaScript, code)
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
      actual = context.evaluate(JavaScript, code).asString(),
    )
  }
}

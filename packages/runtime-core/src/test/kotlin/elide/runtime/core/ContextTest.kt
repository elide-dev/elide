package elide.runtime.core

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    val (globalKey, globalValue) = "globalTestBinding" to Random.nextInt()
    val (jsKey, jsValue) = "jsTestBinding" to Random.nextInt()

    // set a global binding
    context.bindings(language = null).putMember(globalKey, globalValue)

    // set a language-scoped binding
    context.bindings(language = JavaScript).putMember(jsKey, jsValue)

    assertEquals(
      expected = jsValue,
      actual = context.execute(language = JavaScript, source = jsKey).asInt(),
      message = "language-scoped binding should be accessible in target language",
    )

    assertTrue(
      actual = context.execute(language = Python, source = jsKey).isNull,
      message = "language-scoped binding should not be accessible to other languages",
    )

    assertEquals(
      expected = globalValue,
      actual = context.execute(language = JavaScript, source = globalKey).asInt(),
      message = "global binding should be accessible in JavaScript",
    )

    assertEquals(
      expected = globalValue,
      actual = context.execute(language = Python, source = globalKey).asInt(),
      message = "global binding should be accessible in Python",
    )
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

/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.core

import org.graalvm.polyglot.PolyglotException
import org.graalvm.polyglot.Source
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

  @Test fun testIntrinsicBindingsJs() {
    val context = PolyglotEngine {
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
  }

  @Test fun testIntrinsicBindingsPy() {
    val context = PolyglotEngine {
      enableLanguage(Python)
    }.acquire()
    val (jsKey, _) = "jsTestBinding" to Random.nextInt()

    assertThrows<PolyglotException>("language-scoped binding should not be accessible to other languages") {
      context.evaluate(language = Python, source = jsKey)
    }
  }

  @Test @Disabled fun testEnableLanguage() {
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

  @Test fun testCustomEvaluator() {
    val context = PolyglotEngine { enableLanguage(JavaScript) }.acquire()

    var called = false
    val evaluator = object : GuestLanguageEvaluator {
      override fun accepts(source: Source): Boolean {
        return !called
      }

      override fun evaluate(source: Source, context: PolyglotContext): PolyglotValue {
        assertFalse(called, "should not use custom evaluator after `accepts` returns false")
        return PolyglotValue.asValue(Unit).also { called = true }
      }
    }

    context[GuestLanguageEvaluator.contextElementFor(JavaScript)] = evaluator
    context.evaluate(JavaScript, "throw new Error('should not evaluate')")
    assertTrue(called, "expected custom evaluator to be called")

    // verify the evaluator is not called when "accepts" returns false
    context.evaluate(JavaScript, "(() => 5)()")
  }

  @Test fun testCustomParser() {
    val context = PolyglotEngine { enableLanguage(JavaScript) }.acquire()

    var called = false
    val parser = object : GuestLanguageParser {
      override fun accepts(source: Source): Boolean {
        return !called
      }

      override fun parse(source: Source, context: PolyglotContext): PolyglotValue {
        assertFalse(called, "should not use custom parser after `accepts` returns false")
        return PolyglotValue.asValue(Unit).also { called = true }
      }
    }

    context[GuestLanguageParser.contextElementFor(JavaScript)] = parser
    context.parse(JavaScript, "invalid javascript (should not parse)")
    assertTrue(called, "expected custom parser to be called")

    // verify the evaluator is not called when "accepts" returns false
    context.parse(JavaScript, "() => 5")
  }
}

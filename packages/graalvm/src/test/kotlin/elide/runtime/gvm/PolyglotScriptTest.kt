package elide.runtime.gvm

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets


/** Basic tests for polyglot script functionality via Graal. */
class PolyglotScriptTest {
  @Test fun basicPolyglotInlineTest() {
    val polyglot: Context = Context.create()
    val array: Value = polyglot.eval("js", "[1,2,42,4]")
    val result: Int = array.getArrayElement(2).asInt()
    assertEquals(
      42,
      result,
      "should be able to evaluate simple array expression"
    )
  }

  @Test fun basicPolyglotSourceTest() {
    val polyglot: Context = Context.create()
    val source = Source.create(
      "js",
      """
        function hello() {
          return "Hello, Graal!";
        }
        hello();
      """.trimIndent()
    )
    val result: String = polyglot.eval(source).asString()
    assertEquals(
      "Hello, Graal!",
      result,
      "should be able to evaluate simple function return expression"
    )
  }

  @Test fun basicPolyglotSourceCallableTest() {
    val polyglot: Context = Context.create()
    val source = Source.create(
      "js",
      """
        function hello() {
          return "Hello, Graal!";
        }
        function resolver() {
          return hello;
        }
        resolver();
      """.trimIndent()
    )
    val result: Value = polyglot.eval(source)
    assertFalse(
      result.isNull,
      "function result provided by embedded resolver should not be `null`"
    )
    assertTrue(
      result.canExecute(),
      "should be able to execute function returned by embedded resolver"
    )
    val executionResult = result.execute()
    assertNotNull(
      executionResult,
      "execution result from resolved function should not be `null`"
    )
    assertFalse(
      executionResult.canExecute(),
      "result from execution should not itself be executable"
    )
    assertEquals(
      "Hello, Graal!",
      executionResult.asString(),
      "should be able to extract inner execution result"
    )
  }

  @Test fun basicPolyglotSourceCallableArgTest() {
    val polyglot: Context = Context.create()
    val source = Source.create(
      "js",
      """
        function hello(name) {
          return "Hello, " + (name || "Graal") + "!";
        }
        function resolver() {
          return hello;
        }
        resolver();
      """.trimIndent()
    )
    val result: Value = polyglot.eval(source)
    assertFalse(
      result.isNull,
      "function result provided by embedded resolver should not be `null`"
    )
    assertTrue(
      result.canExecute(),
      "should be able to execute function returned by embedded resolver"
    )
    val executionResult = result.execute(
      "Testsuite"
    )
    assertNotNull(
      executionResult,
      "execution result from resolved function should not be `null`"
    )
    assertFalse(
      executionResult.canExecute(),
      "result from execution should not itself be executable"
    )
    assertEquals(
      "Hello, Testsuite!",
      executionResult.asString(),
      "should be able to extract inner execution result"
    )
  }

  @Test fun testEvaluateEmbeddedScriptHarness() {
    val polyglot: Context = Context.create()
    val source = Source.create(
      "js",
      PolyglotScriptTest::class.java.getResourceAsStream(
        "/META-INF/elide/embedded/harness.js"
      )!!.bufferedReader(
        StandardCharsets.UTF_8
      ).readText()
    )

    val result: Value = polyglot.eval(source)
    assertFalse(
      result.isNull,
      "function result provided by embedded resolver should not be `null`"
    )
    assertTrue(
      result.canExecute(),
      "should have executable result from harness"
    )
    val executionResult = result.execute()
    assertNotNull(
      executionResult,
      "execution result from resolved function should not be `null`"
    )
    val executionOut = executionResult.asString()
    assertNotNull(
      executionOut,
      "execution result from resolved function should not be `null`"
    )
    assertEquals(
      "<div><strong>Hello, React SSR!</strong></div>",
      executionOut,
      "execution output should be expected value"
    )
  }
}

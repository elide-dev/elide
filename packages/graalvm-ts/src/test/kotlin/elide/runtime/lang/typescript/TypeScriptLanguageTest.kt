package elide.runtime.lang.typescript

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.PolyglotAccess
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.io.IOAccess
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.*

/** Basic tests for [TypeScriptLanguage]. */
class TypeScriptLanguageTest {
  private val engine: Engine = Engine.newBuilder("js", "ts").build()
  private fun ctx(): Context = Context.newBuilder("js", "ts")
    .allowInnerContextOptions(true)
    .allowPolyglotAccess(PolyglotAccess.ALL)
    .allowIO(IOAccess.ALL)
    .allowExperimentalOptions(true)
    .allowValueSharing(true)
    .allowAllAccess(true)
    .engine(engine)
    .build()

  private fun language() = TypeScriptLanguage()

  @Test fun testLoadClassInitialization() {
    assertDoesNotThrow { language() }
  }

  @Test fun testCompileRunTypeScript() {
    val jsSrc = Source.newBuilder(
      "js",
      // language=javascript
      """
        function hello() {
          return "Hello, World!";
        }
        hello();
      """.trimIndent(),
      "sample.js",
    ).build().also {
      assertNotNull(it)
    }

    val ctx = ctx()
    ctx.initialize("js")
    ctx.initialize("ts")
    ctx.enter()
    val parsed = ctx.parse(jsSrc)
    ctx.leave()
    assertNotNull(parsed)
    ctx.enter()
    val jsResult = ctx.eval(jsSrc)
    ctx.leave()
    assertNotNull(jsResult, "result should not be null from javascript compile-and-execute")

    val src = Source.newBuilder(
      "ts",
      // language=typescript
      """
        const x: number = 42;
        console.log(x);
        x;
      """.trimIndent(),
      "basic.ts",
    ).build().also {
      assertNotNull(it)
    }

    ctx.enter()
    ctx.parse(src)
    ctx.leave()
    ctx.enter()
    val result = ctx.eval(src)
    ctx.leave()
    assertNotNull(result, "result should not be null from typescript compile-and-execute")
    assertTrue(result.isNumber, "result should be a number")
    assertEquals(42, result.asInt(), "result should be 42")
  }
}

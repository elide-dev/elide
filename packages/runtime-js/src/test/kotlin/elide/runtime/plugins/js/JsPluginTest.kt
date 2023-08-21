package elide.runtime.plugins.js

import org.junit.jupiter.api.Test
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.fail
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.plugins.vfs.Vfs
import elide.runtime.plugins.vfs.include

@OptIn(DelicateElideApi::class)
internal class JsPluginTest {
  private fun resource(name: String): URL {
    return JsPluginTest::class.java.getResource("/$name") ?: fail("Resource not found")
  }

  @Test fun testExecution() {
    val engine = PolyglotEngine { install(JavaScript) }
    val context = engine.acquire()

    val result = context.javaScript(
      """
      const a = 42
      function getValue() { return a; }

      getValue()
      """
    )

    assertEquals(
      expected = 42,
      actual = result.asInt(),
      message = "should return correct value",
    )
  }

  @Test fun testEmbeddedCjs() {
    val engine = PolyglotEngine {
      install(Vfs) { include(resource("hello-world/hello.tar.gz")) }
      install(JavaScript)
    }
    val context = engine.acquire()

    val requireResult = context.javaScript(
      """
      const hello = require("hello")
      hello("Elide")
      """
    )

    assertEquals(
      expected = "👋 Hello, Elide!",
      actual = requireResult.asString(),
      message = "should return correct value",
    )
  }

  @Test fun testEmbeddedEsm() {
    val engine = PolyglotEngine {
      install(Vfs) { include(resource("hello-world/hello.tar.gz")) }
      install(JavaScript)
    }
    val context = engine.acquire()

    val importResult = context.javaScript(
      """
      import { greet } from "hello"
      export const returnValue = greet("Elide")
      """,
      esm = true,
    )

    assertEquals(
      expected = "👋 Hello, Elide!",
      actual = importResult.getMember("returnValue").asString(),
      message = "should return correct value",
    )
  }
}

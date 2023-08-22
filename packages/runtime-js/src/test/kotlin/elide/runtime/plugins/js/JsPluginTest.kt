package elide.runtime.plugins.js

import org.junit.jupiter.api.Test
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.fail
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotEngine
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.plugins.vfs.Vfs
import elide.runtime.plugins.vfs.include

@OptIn(DelicateElideApi::class)
internal class JsPluginTest {
  /** Resolve a resource [URL] by [name]. The name is treated as a path relative to the resources root. */
  private fun resource(name: String): URL {
    return JsPluginTest::class.java.getResource("/$name") ?: fail("Resource not found")
  }

  /**
   * Generate a configuration function that installs the [Vfs] plugin and includes the specified [bundles] after
   * resolving their individual [URL]s using the [resource] helper.
   *
   * This method is intended to be used for convenience in [withJsPlugin]'s `configureEngine` argument.
   */
  private fun useResourceBundle(vararg bundles: String): PolyglotEngineConfiguration.() -> Unit = {
    install(Vfs) {
      for(bundle in bundles) include(resource(bundle))
    }
  }

  /**
   * Run a block of code after configuring a [PolyglotEngine] with the [JavaScript] plugin.
   *
   * @param configureEngine Configuration block to run *before* installing the [JavaScript] plugin.
   * @param configurePlugin Configuration block to run when installing the [JavaScript] plugin.
   * @param use Code to run using a [PolyglotContext] [acquired][PolyglotEngine.acquire] from the configured engine.
   */
  private fun withJsPlugin(
    configureEngine: PolyglotEngineConfiguration.() -> Unit = { },
    configurePlugin: JavaScriptConfig.() -> Unit = { },
    use: PolyglotContext.() -> Unit,
  ) {
    val engine = PolyglotEngine {
      configureEngine()
      install(JavaScript, configurePlugin)
    }
    val context = engine.acquire()

    use(context)
  }

  @Test fun testExecution() = withJsPlugin {
    val result = javaScript(
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

  @Test fun testEmbeddedCjs() = withJsPlugin(
    configureEngine = useResourceBundle("hello-world/hello.tar.gz")
  ) {
    val requireResult = javaScript(
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

  @Test fun testEmbeddedEsm() = withJsPlugin(
    configureEngine = useResourceBundle("hello-world/hello.tar.gz")
  ) {
    val importResult = javaScript(
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

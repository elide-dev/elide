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
package elide.runtime.plugins.js

import org.junit.jupiter.api.Test
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.fail
import elide.runtime.core.*
import elide.runtime.plugins.vfs.Vfs
import elide.runtime.plugins.vfs.include
import elide.runtime.plugins.vfs.vfs

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
    configure(Vfs) {
      for(bundle in bundles) include(resource(bundle))
    }
  }

  /**
   * Generate a configuration function updating the NPM module resolution path for the JavaSript plugin. This method
   * is intended to be used for convenience in [withJsPlugin]'s `configurePlugin` argument.
   */
  private fun useModulePath(root: String): JavaScriptConfig.() -> Unit = {
    npm {
      modulesPath = root
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
      configure(JavaScript, configurePlugin)
      configureEngine()
      vfs {
        // Nothing to configure.
      }
    }

    use(engine.acquire())
  }

  @Test fun testExecution() = withJsPlugin {
    val result = javascript(
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
    configureEngine = useResourceBundle("hello-world/hello.tar.gz"),
    configurePlugin = useModulePath("/app"),
  ) {
    val requireResult = javascript(
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
    configureEngine = useResourceBundle("hello-world/hello.tar.gz"),
    configurePlugin = useModulePath("/app"),
  ) {
    val importResult = javascript(
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

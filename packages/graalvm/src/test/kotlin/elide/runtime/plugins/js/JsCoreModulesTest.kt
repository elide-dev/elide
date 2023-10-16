package elide.runtime.plugins.js

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine

@OptIn(DelicateElideApi::class)
internal class JsCoreModulesTest {
  @ParameterizedTest
  @ValueSource(strings = ["buffer", "util", "fs", "express"])
  fun testCoreModulePresent(module: String) {
    val context = PolyglotEngine { install(JavaScript) }.acquire()

    assertDoesNotThrow("core module replacement for '$module' should be present") {
      context.javascript("""require("$module");""")
    }
  }
}
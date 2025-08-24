/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test

@TestCase internal class NodeStreamPromisesBehaviorTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.stream.NodeStreamPromisesModule>() {
  override val moduleName: String get() = "stream/promises"
  override fun provide(): elide.runtime.node.stream.NodeStreamPromisesModule = elide.runtime.node.stream.NodeStreamPromisesModule()

  @Test fun `finished and pipeline resolve`() = test {
    val code = """
      const sp = require('node:stream/promises');
      await sp.finished({});
      await sp.pipeline({},{});
      'ok';
    """.trimIndent()
    executeGuest(code)
  }
}


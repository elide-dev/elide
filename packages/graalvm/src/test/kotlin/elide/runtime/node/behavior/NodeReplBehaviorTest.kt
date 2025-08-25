/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test

@TestCase internal class NodeReplBehaviorTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.repl.NodeReplModule>() {
  override val moduleName: String get() = "repl"
  override fun provide(): elide.runtime.node.repl.NodeReplModule = elide.runtime.node.repl.NodeReplModule()

  @Test fun `start returns repl-like controller`() {
    val code = """
      const repl = require('node:repl');
      const s = repl.start();
      if (typeof s !== 'object') throw new Error('bad');
      s.write('1+1');
      s.close();
      'ok';
    """.trimIndent()
    executeGuest(true) { code }
  }
}


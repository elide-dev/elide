/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test

@TestCase internal class NodeReadlineBehaviorTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.readline.NodeReadlineModule>() {
  override val moduleName: String get() = "readline"
  override fun provide(): elide.runtime.node.readline.NodeReadlineModule = elide.runtime.node.readline.NodeReadlineModule()

  @Test fun `createInterface and question`() {
    val code = """
      const rl = require('node:readline').createInterface({});
      rl.question('answer', (ans) => {});
      rl.close();
      'ok';
    """.trimIndent()
    executeGuest(true) { code }
  }
}


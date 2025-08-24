/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test

@TestCase internal class NodeReadlinePromisesBehaviorTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.readline.NodeReadlinePromisesModule>() {
  override val moduleName: String get() = "readline/promises"
  override fun provide(): elide.runtime.node.readline.NodeReadlinePromisesModule = elide.runtime.node.readline.NodeReadlinePromisesModule()

  @Test fun `createInterface and question`() = test {
    val code = """
      const rl = require('node:readline/promises').createInterface({});
      const ans = await rl.question('answer');
      if (ans !== 'answer') throw new Error('bad');
      rl.close();
      'ok';
    """.trimIndent()
    executeGuest(code)
  }
}


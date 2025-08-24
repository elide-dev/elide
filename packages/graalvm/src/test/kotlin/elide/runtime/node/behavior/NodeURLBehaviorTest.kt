/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test

@TestCase internal class NodeURLBehaviorTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.url.NodeURLModule>() {
  override val moduleName: String get() = "url"
  override fun provide(): elide.runtime.node.url.NodeURLModule = elide.runtime.node.url.NodeURLModule()

  @Test fun `fileURLToPath handles UNC`() = test {
    val code = """
      const url = require('node:url');
      const p = url.fileURLToPath('file:////server/share/f.txt');
      if (!p.startsWith('\\\\server\\share')) throw new Error('bad');
      'ok';
    """.trimIndent()
    executeGuest(code)
  }

  @Test fun `pathToFileURL handles UNC input`() = test {
    val code = """
      const url = require('node:url');
      const u = url.pathToFileURL('\\\\server\\share\\f.txt');
      if (!String(u.href || u).startsWith('file:////server/share')) throw new Error('bad');
      'ok';
    """.trimIndent()
    executeGuest(code)
  }
}


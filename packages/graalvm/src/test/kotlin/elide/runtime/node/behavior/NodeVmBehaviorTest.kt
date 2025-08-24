/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestCase internal class NodeVmBehaviorTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.vm.NodeVmModule>() {
  override val moduleName: String get() = "vm"
  override fun provide(): elide.runtime.node.vm.NodeVmModule = elide.runtime.node.vm.NodeVmModule()

  @Test fun `runInThisContext executes code`() {
    val code = """
      const vm = require('node:vm');
      const res = vm.runInThisContext('1 + 2');
      if (res !== 3) throw new Error('bad');
      'ok';
    """.trimIndent()
    executeGuest(true) { code }
  }

  @Test fun `createContext brands context`() {
    val code = """
      const vm = require('node:vm');
      const ctx = vm.createContext({a:1});
      if (!vm.isContext(ctx)) throw new Error('not context');
      'ok';
    """.trimIndent()
    executeGuest(true) { code }
  }
}


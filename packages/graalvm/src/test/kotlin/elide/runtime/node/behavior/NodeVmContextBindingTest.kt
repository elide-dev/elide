/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test

@TestCase internal class NodeVmContextBindingTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.vm.NodeVmModule>() {
  override val moduleName: String get() = "vm"
  override fun provide(): elide.runtime.node.vm.NodeVmModule = elide.runtime.node.vm.NodeVmModule()

  @Test fun `runInNewContext binds sandbox members`() = test {
    val code = """
      const vm = require('node:vm');
      const ctx = {x: 41};
      const res = vm.runInNewContext('x + 1', ctx);
      if (res !== 42) throw new Error('bad');
      'ok';
    """.trimIndent()
    executeGuest(code)
  }

  @Test fun `runInContext binds provided context members`() = test {
    val code = """
      const vm = require('node:vm');
      const ctx = vm.createContext({x: 10});
      const res = vm.runInContext('x * 2', ctx);
      if (res !== 20) throw new Error('bad');
      'ok';
    """.trimIndent()
    executeGuest(code)
  }
}


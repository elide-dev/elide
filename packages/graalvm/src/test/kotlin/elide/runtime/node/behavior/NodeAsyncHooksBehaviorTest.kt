/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test

@TestCase internal class NodeAsyncHooksBehaviorTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.async.NodeAsyncHooksModule>() {
  override val moduleName: String get() = "async_hooks"
  override fun provide(): elide.runtime.node.async.NodeAsyncHooksModule = elide.runtime.node.async.NodeAsyncHooksModule()

  @Test fun `createHook returns controller`() {
    val code = """
      const hooks = require('node:async_hooks');
      const h = hooks.createHook({init(){}});
      if (typeof h !== 'object') throw new Error('bad');
      h.enable();
      h.disable();
      'ok';
    """.trimIndent()
    executeGuest(true) { code }
  }
}


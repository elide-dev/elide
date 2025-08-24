/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test

@TestCase internal class NodePerfHooksBehaviorTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.perfHooks.NodePerformanceHooksModule>() {
  override val moduleName: String get() = "perf_hooks"
  override fun provide(): elide.runtime.node.perfHooks.NodePerformanceHooksModule = elide.runtime.node.perfHooks.NodePerformanceHooksModule()

  @Test fun `performance now`() {
    val code = """
      const ph = require('node:perf_hooks');
      if (typeof ph.performance.now() !== 'number') throw new Error('bad');
      'ok';
    """.trimIndent()
    executeGuest(true) { code }
  }
}


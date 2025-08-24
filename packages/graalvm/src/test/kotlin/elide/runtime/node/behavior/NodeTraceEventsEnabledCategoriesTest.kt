/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test

@TestCase internal class NodeTraceEventsEnabledCategoriesTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.trace.NodeTraceEventsModule>() {
  override val moduleName: String get() = "trace_events"
  override fun provide(): elide.runtime.node.trace.NodeTraceEventsModule = elide.runtime.node.trace.NodeTraceEventsModule()

  @Test fun `getEnabledCategories returns categories after enabling`() = test {
    val code = """
      const trace = require('node:trace_events');
      const a = trace.createTracing({categories:'catA'});
      a.enable();
      const b = trace.createTracing({categories:'catB,catC'});
      b.enable();
      const cats = trace.getEnabledCategories();
      if (!cats.includes('catA') || !cats.includes('catB') || !cats.includes('catC')) {
        throw new Error('bad');
      }
      a.disable();
      b.disable();
      'ok';
    """.trimIndent()
    executeGuest(code)
  }
}


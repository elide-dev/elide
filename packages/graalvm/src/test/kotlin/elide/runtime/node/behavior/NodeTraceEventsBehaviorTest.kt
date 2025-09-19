/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test

@TestCase internal class NodeTraceEventsBehaviorTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.trace.NodeTraceEventsModule>() {
  override val moduleName: String get() = "trace_events"
  override fun provide(): elide.runtime.node.trace.NodeTraceEventsModule = elide.runtime.node.trace.NodeTraceEventsModule()

  @Test fun `createTracing returns controller`() {
    val code = """
      const trace = require('node:trace_events');
      const ctrl = trace.createTracing({categories:'node.perf'});
      if (typeof ctrl !== 'object') throw new Error('bad');
      ctrl.enable();
      ctrl.disable();
      'ok';
    """.trimIndent()
    executeGuest(true) { code }
  }
}


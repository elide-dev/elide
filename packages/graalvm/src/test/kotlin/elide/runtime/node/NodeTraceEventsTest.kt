/*
 * Copyright (c) 2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.trace.NodeTraceEventsModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `trace_events` built-in module. */
@TestCase internal class NodeTraceEventsTest : NodeModuleConformanceTest<NodeTraceEventsModule>() {
  override val moduleName: String get() = "trace_events"
  override fun provide(): NodeTraceEventsModule = NodeTraceEventsModule()
  @Inject lateinit var traceEvents: NodeTraceEventsModule

  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("createTracing")
    yield("getEnabledCategories")
  }

  @Test override fun testInjectable() {
    assertNotNull(traceEvents)
  }
}


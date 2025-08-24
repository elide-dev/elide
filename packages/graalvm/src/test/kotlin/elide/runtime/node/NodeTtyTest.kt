/*
 * Copyright (c) 2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.tty.NodeTtyModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `tty` built-in module. */
@TestCase internal class NodeTtyTest : NodeModuleConformanceTest<NodeTtyModule>() {
  override val moduleName: String get() = "tty"
  override fun provide(): NodeTtyModule = NodeTtyModule()
  @Inject lateinit var tty: NodeTtyModule

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("isatty")
  }

  @Test override fun testInjectable() {
    assertNotNull(tty)
  }
}


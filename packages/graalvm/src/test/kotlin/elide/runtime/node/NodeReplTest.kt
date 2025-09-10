/*
 * Copyright (c) 2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.repl.NodeReplModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `repl` built-in module. */
@TestCase internal class NodeReplTest : NodeModuleConformanceTest<NodeReplModule>() {
  override val moduleName: String get() = "repl"
  override fun provide(): NodeReplModule = NodeReplModule()
  @Inject lateinit var repl: NodeReplModule

  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("start")
  }

  @Test override fun testInjectable() {
    assertNotNull(repl)
  }
}


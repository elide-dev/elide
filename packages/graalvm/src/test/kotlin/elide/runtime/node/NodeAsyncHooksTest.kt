/*
 * Copyright (c) 2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.async.NodeAsyncHooksModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `async_hooks` built-in module. */
@TestCase internal class NodeAsyncHooksTest : NodeModuleConformanceTest<NodeAsyncHooksModule>() {
  override val moduleName: String get() = "async_hooks"
  override fun provide(): NodeAsyncHooksModule = NodeAsyncHooksModule()
  @Inject lateinit var asyncHooks: NodeAsyncHooksModule

  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("createHook")
    yield("executionAsyncId")
    yield("triggerAsyncId")
  }

  @Test override fun testInjectable() {
    assertNotNull(asyncHooks)
  }
}


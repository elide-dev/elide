/*
 * Copyright (c) 2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.wasi.NodeWasiModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `wasi` built-in module. */
@TestCase internal class NodeWasiTest : NodeModuleConformanceTest<NodeWasiModule>() {
  override val moduleName: String get() = "wasi"
  override fun provide(): NodeWasiModule = NodeWasiModule()
  @Inject lateinit var wasi: NodeWasiModule

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("WASI")
  }

  @Test override fun testInjectable() {
    assertNotNull(wasi)
  }
}


/*
 * Copyright (c) 2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.vm.NodeVmModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `vm` built-in module. */
@TestCase internal class NodeVMTest : NodeModuleConformanceTest<NodeVmModule>() {
  override val moduleName: String get() = "vm"
  override fun provide(): NodeVmModule = NodeVmModule()
  @Inject lateinit var vm: NodeVmModule

  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("createContext")
    yield("isContext")
    yield("runInContext")
    yield("runInNewContext")
    yield("runInThisContext")
  }

  @Test override fun testInjectable() {
    assertNotNull(vm)
  }
}


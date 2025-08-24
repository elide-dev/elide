/*
 * Copyright (c) 2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.constants.NodeConstantsModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `constants` built-in module. */
@TestCase internal class NodeConstantsTest : NodeModuleConformanceTest<NodeConstantsModule>() {
  override val moduleName: String get() = "constants"
  override fun provide(): NodeConstantsModule = NodeConstantsModule()
  @Inject lateinit var constants: NodeConstantsModule

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("os")
    yield("fs")
  }

  @Test override fun testInjectable() {
    assertNotNull(constants)
  }
}


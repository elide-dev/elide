/*
 * Copyright (c) 2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import kotlin.test.Test
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `v8` built-in module. */
@TestCase internal class NodeV8Test : GenericJsModuleTest<elide.runtime.node.v8.NodeV8Module>() {
  override val moduleName: String get() = "v8"
  override fun provide(): elide.runtime.node.v8.NodeV8Module = elide.runtime.node.v8.NodeV8Module()

  // GraalJS is not V8; ensure we can require the facade shape without engine-specific behaviors.
  @Test fun `should load v8 module`() {
    require()
  }
}


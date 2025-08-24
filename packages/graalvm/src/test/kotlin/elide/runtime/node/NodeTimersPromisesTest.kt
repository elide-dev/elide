/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import elide.testing.annotations.TestCase
import kotlin.test.Test
import kotlin.test.assertNotNull

/** Shape and behavior tests for node:timers/promises */
@TestCase internal class NodeTimersPromisesTest : AbstractJsModuleTest<elide.runtime.node.timers.NodeTimersPromisesModule>() {
  override val moduleName: String get() = "timers/promises"
  override fun provide(): elide.runtime.node.timers.NodeTimersPromisesModule = elide.runtime.node.timers.NodeTimersPromisesModule()

  @Test fun `shape - exported members`() {
    val mod = import("node:timers/promises")
    assertNotNull(mod.getMember("setTimeout"))
    assertNotNull(mod.getMember("setImmediate"))
  }
}


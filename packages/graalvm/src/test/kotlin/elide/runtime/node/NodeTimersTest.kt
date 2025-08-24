/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import elide.testing.annotations.TestCase
import elide.annotations.Inject
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/** Shape and behavior tests for node:timers */
@TestCase internal class NodeTimersTest : AbstractJsModuleTest<elide.runtime.node.timers.NodeTimersModule>() {
  override val moduleName: String get() = "timers"
  override fun provide(): elide.runtime.node.timers.NodeTimersModule = elide.runtime.node.timers.NodeTimersModule()

  @Inject lateinit var timers: elide.runtime.node.timers.NodeTimersModule

  @Test override fun testInjectable() {
    assertNotNull(timers)
  }

  @Test fun `shape - exported members`() {
    val mod = import("node:timers")
    val keys = mod.memberKeys.toSet()
    setOf("setTimeout","clearTimeout","setInterval","clearInterval","setImmediate","clearImmediate").forEach {
      assertTrue(it in keys, "expected $it")
    }
  }
}


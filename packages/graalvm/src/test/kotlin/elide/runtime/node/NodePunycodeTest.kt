/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import elide.testing.annotations.TestCase
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.punycode.NodePunycodeModule

/** Conformance: node:punycode */
@TestCase internal class NodePunycodeTest : NodeModuleConformanceTest<NodePunycodeModule>() {
  override val moduleName: String get() = "punycode"
  override fun provide(): NodePunycodeModule = NodePunycodeModule()
  override fun expectCompliance(): Boolean = false

  @Inject lateinit var punycode: NodePunycodeModule

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("toASCII")
    yield("toUnicode")
    yield("encode")
    yield("decode")
  }

  @Test override fun testInjectable() {
    assertNotNull(punycode)
  }

  @Test fun smoke() {
    val mod = require("punycode")
    val ascii = mod.getMember("toASCII").execute("mañana.com").asString()
    assertTrue(ascii.startsWith("xn--"))
  }
}


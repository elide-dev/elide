/*
 * Copyright (c) 2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.tls.NodeTlsModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `tls` built-in module. */
@TestCase internal class NodeTlsTest : NodeModuleConformanceTest<NodeTlsModule>() {
  override val moduleName: String get() = "tls"
  override fun provide(): NodeTlsModule = NodeTlsModule()
  @Inject lateinit var tls: NodeTlsModule

  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("createServer")
    yield("connect")
    yield("createSecureContext")
    yield("getCiphers")
    yield("DEFAULT_MIN_VERSION")
    yield("DEFAULT_MAX_VERSION")
  }

  @Test override fun testInjectable() {
    assertNotNull(tls)
  }
}


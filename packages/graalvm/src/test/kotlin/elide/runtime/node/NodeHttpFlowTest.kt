/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import elide.runtime.node.http.NodeHttpModule
import elide.testing.annotations.TestCase
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

/** Targeted smoke test for Node `http` createServer flow. */
@TestCase
internal class NodeHttpFlowTest : NodeModuleConformanceTest<NodeHttpModule>() {
  override val moduleName: String get() = "http"
  override fun provide(): NodeHttpModule = NodeHttpModule()
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence { yield("createServer") }

  @Test fun `http - createServer basic flow`() {
    // Start a server listening on 0 (ephemeral) and set a simple handler
    val code = """
      const http = require('node:http');
      const server = http.createServer((req, res) => {
        res.setHeader('X', '1');
        res.statusCode = 200;
        res.end('ok');
      });
      server.listen(0);
      server.address();
      'ok';
    """.trimIndent()
    executeGuest(true) { code }.doesNotFail()
    // We don't perform a real HTTP client fetch here; the purpose is to ensure listen() doesn't throw and address() is callable
    val server = assertNotNull(require("node:http").getMember("createServer"))
    assertTrue(server.canExecute())
  }
}


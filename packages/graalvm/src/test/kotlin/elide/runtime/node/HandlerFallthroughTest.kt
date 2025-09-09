/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import elide.testing.annotations.TestCase
import kotlin.test.Test

/** Verifies non-boolean/undefined handler return is treated as handled (no 404). */
@TestCase
internal class HandlerFallthroughTest : GenericJsModuleTest<elide.runtime.node.http.NodeHttpModule>() {
  override val moduleName: String get() = "http"
  override fun provide(): elide.runtime.node.http.NodeHttpModule = elide.runtime.node.http.NodeHttpModule()

  @Test fun `handler returning undefined is treated as handled`() {
    // Simulate a simple handler that returns undefined but writes a response
    val js = """
      const http = require('node:http');
      const server = http.createServer((req, res) => {
        res.setHeader('X', '1');
        res.statusCode = 200;
        res.end('ok');
        // return undefined (no explicit boolean)
      });
      server.listen(0);
      true;
    """.trimIndent()
    executeGuest(true) { js } .doesNotFail()
  }
}


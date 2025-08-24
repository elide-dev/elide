/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test

@TestCase internal class NodeTlsBehaviorTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.tls.NodeTlsModule>() {
  override val moduleName: String get() = "tls"
  override fun provide(): elide.runtime.node.tls.NodeTlsModule = elide.runtime.node.tls.NodeTlsModule()

  @Test fun `getCiphers returns array`() = test {
    val code = """
      const tls = require('node:tls');
      const ciphers = tls.getCiphers();
      if (!Array.isArray(ciphers) || ciphers.length === 0) throw new Error('bad');
      'ok';
    """.trimIndent()
    executeGuest(code)
  }
}


/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test

@TestCase internal class NodePunycodeBehaviorTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.punycode.NodePunycodeModule>() {
  override val moduleName: String get() = "punycode"
  override fun provide(): elide.runtime.node.punycode.NodePunycodeModule = elide.runtime.node.punycode.NodePunycodeModule()

  @Test fun `encode-decode roundtrip`() = test {
    val code = """
      const p = require('node:punycode');
      const s = 'mañana-例';
      const enc = p.encode(s);
      const dec = p.decode(enc);
      if (dec !== s) throw new Error('bad');
      'ok';
    """.trimIndent()
    executeGuest(code)
  }
}


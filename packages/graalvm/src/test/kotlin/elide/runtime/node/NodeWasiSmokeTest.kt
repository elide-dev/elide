/*
 * Copyright (c) 2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.testing.annotations.TestCase

/** A tiny smoke test to require('wasi') and instantiate WASI. */
@TestCase internal class NodeWasiSmokeTest : GenericJsModuleTest<Nothing>() {
  override val moduleName: String get() = "wasi"
  override fun provide(): Nothing = error("no provide")

  @Test fun `should load wasi and expose WASI constructor`() = test {
    val code = """
      const wasi = require('wasi');
      if (typeof wasi !== 'object') throw new Error('wasi did not load');
      if (typeof wasi.WASI !== 'function') throw new Error('WASI constructor not present');
      new wasi.WASI({});
      'ok';
    """.trimIndent()
    executeGuest(code)
  }
}


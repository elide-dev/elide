/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test

@TestCase internal class NodeWorkerThreadsParentPortTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.worker.NodeWorkerThreadsModule>() {
  override val moduleName: String get() = "worker_threads"
  override fun provide(): elide.runtime.node.worker.NodeWorkerThreadsModule = elide.runtime.node.worker.NodeWorkerThreadsModule()

  @Test fun `parentPort handles message`() = test {
    val code = """
      const wt = require('node:worker_threads');
      let got = false;
      if (wt.parentPort) wt.parentPort.onmessage = (msg) => { got = true; };
      if (wt.parentPort) wt.parentPort.postMessage({x:2});
      if (wt.parentPort && !got) throw new Error('bad');
      'ok';
    """.trimIndent()
    executeGuest(code)
  }
}


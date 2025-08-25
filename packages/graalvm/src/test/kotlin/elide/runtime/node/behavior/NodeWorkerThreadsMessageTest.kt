/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test

@TestCase internal class NodeWorkerThreadsMessageTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.worker.NodeWorkerThreadsModule>() {
  override val moduleName: String get() = "worker_threads"
  override fun provide(): elide.runtime.node.worker.NodeWorkerThreadsModule = elide.runtime.node.worker.NodeWorkerThreadsModule()

  @Test fun `worker onmessage receives postMessage`() {
    val code = """
      const wt = require('node:worker_threads');
      const w = new wt.Worker('');
      let ok = false;
      w.onmessage = (msg) => { ok = true; };
      w.postMessage({x:1});
      if (!ok) throw new Error('bad');
      'ok';
    """.trimIndent()
    executeGuest(true) { code }
  }
}


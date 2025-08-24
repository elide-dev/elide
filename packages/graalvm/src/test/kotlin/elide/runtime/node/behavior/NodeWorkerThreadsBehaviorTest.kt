/*
 * Copyright (c) 2025 Elide.
 * Licensed under the MIT license.
 */
package elide.runtime.node.behavior

import elide.testing.annotations.TestCase
import kotlin.test.Test

@TestCase internal class NodeWorkerThreadsBehaviorTest : elide.runtime.node.GenericJsModuleTest<elide.runtime.node.worker.NodeWorkerThreadsModule>() {
  override val moduleName: String get() = "worker_threads"
  override fun provide(): elide.runtime.node.worker.NodeWorkerThreadsModule = elide.runtime.node.worker.NodeWorkerThreadsModule()

  @Test fun `worker constructor returns object`() = test {
    val code = """
      const wt = require('node:worker_threads');
      const w = new wt.Worker('');
      if (typeof w !== 'object') throw new Error('bad');
      w.postMessage({});
      w.terminate();
      'ok';
    """.trimIndent()
    executeGuest(code)
  }
}


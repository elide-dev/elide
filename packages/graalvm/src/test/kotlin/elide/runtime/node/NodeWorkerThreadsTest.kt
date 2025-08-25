/*
 * Copyright (c) 2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.worker.NodeWorkerThreadsModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `worker_threads` built-in module. */
@TestCase internal class NodeWorkerThreadsTest : NodeModuleConformanceTest<NodeWorkerThreadsModule>() {
  override val moduleName: String get() = "worker_threads"
  override fun provide(): NodeWorkerThreadsModule = NodeWorkerThreadsModule()
  @Inject lateinit var workerThreads: NodeWorkerThreadsModule

  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("isMainThread")
    yield("Worker")
  }

  @Test override fun testInjectable() {
    assertNotNull(workerThreads)
  }
}


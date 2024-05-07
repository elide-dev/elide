/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.gvm.internals.js.node

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import elide.annotations.Inject
import elide.runtime.gvm.internals.node.buffer.NodeBufferModule
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.runtime.intrinsics.js.node.BufferAPI
import elide.runtime.intrinsics.js.node.buffer.Buffer
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for [Buffer]. */
@TestCase internal class NodeBufferTest : NodeModuleConformanceTest<NodeBufferModule>() {
  override val moduleName: String get() = "buffer"
  override fun provide(): NodeBufferModule = NodeBufferModule()
  @Inject internal lateinit var buffer: BufferAPI

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("constants")
    yield("kMaxLength")
    yield("kStringMaxLength")
    yield("resolveObjectURL")
    yield("transcode")
    yield("isUtf8")
    yield("isAscii")
    yield("atob")
    yield("btoa")
    yield("Buffer")
    yield("Blob")
    yield("File")
    yield("SlowBuffer")
  }

  @Test override fun testInjectable() {
    assertNotNull(buffer)
  }

  @Test fun testEmptyBuffers() {
    assertNotNull(Buffer.empty())
    assertEquals(Buffer.empty(), Buffer.empty())
    assertSame(Buffer.empty(), Buffer.empty())
  }
}

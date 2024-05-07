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

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.gvm.internals.node.stream.NodeStreamWebModule
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `stream/web` built-in module. */
@TestCase internal class NodeStreamWebTest : NodeModuleConformanceTest<NodeStreamWebModule>() {
  override val moduleName: String get() = "stream/web"
  override fun provide(): NodeStreamWebModule = NodeStreamWebModule()
  @Inject lateinit var stream: NodeStreamWebModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("ReadableStream")
    yield("ReadableStreamDefaultReader")
    yield("ReadableStreamBYOBReader")
    yield("ReadableStreamDefaultController")
    yield("ReadableByteStreamController")
    yield("ReadableStreamBYOBRequest")
    yield("WritableStream")
    yield("WritableStreamDefaultWriter")
    yield("WritableStreamDefaultController")
    yield("TransformStream")
    yield("TransformStreamDefaultController")
    yield("ByteLengthQueuingStrategy")
    yield("CountQueuingStrategy")
    yield("TextEncoderStream")
    yield("TextDecoderStream")
    yield("CompressionStream")
    yield("DecompressionStream")
  }

  @Test override fun testInjectable() {
    assertNotNull(stream)
  }
}

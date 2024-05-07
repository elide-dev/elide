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
import elide.runtime.gvm.internals.node.http2.NodeHttp2Module
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `http2` built-in module. */
@TestCase internal class NodeHttp2Test : NodeModuleConformanceTest<NodeHttp2Module>() {
  override val moduleName: String get() = "http2"
  override fun provide(): NodeHttp2Module = NodeHttp2Module()
  @Inject lateinit var http2: NodeHttp2Module

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("Http2Session")
    yield("ServerHttp2Session")
    yield("ClientHttp2Session")
    yield("Http2Stream")
    yield("ClientHttp2Stream")
    yield("ServerHttp2Stream")
    yield("Http2Server")
    yield("Http2SecureServer")
    yield("Http2ServerRequest")
    yield("Http2ServerResponse")
  }

  @Test override fun testInjectable() {
    assertNotNull(http2)
  }
}

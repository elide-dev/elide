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
import elide.runtime.gvm.internals.node.http.NodeHttpModule
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `http` built-in module. */
@TestCase internal class NodeHttpTest : NodeModuleConformanceTest<NodeHttpModule>() {
  override val moduleName: String get() = "http"
  override fun provide(): NodeHttpModule = NodeHttpModule()
  @Inject lateinit var http: NodeHttpModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("Agent")
    yield("ClientRequest")
    yield("Server")
    yield("ServerResponse")
    yield("IncomingMessage")
    yield("OutgoingMessage")
    yield("METHODS")
    yield("STATUS_CODES")
    yield("createServer")
    yield("get")
    yield("globalAgent")
    yield("maxHeaderSize")
    yield("request")
    yield("validateHeaderName")
    yield("validateHeaderValue")
    yield("setMaxIdleHTTPParsers")
  }

  @Test override fun testInjectable() {
    assertNotNull(http)
  }
}

/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.runtime.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.net.NodeNetworkModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `net` built-in module. */
@TestCase internal class NodeNetTest : NodeModuleConformanceTest<NodeNetworkModule>() {
  override val moduleName: String get() = "net"
  override fun provide(): NodeNetworkModule = NodeNetworkModule()
  @Inject lateinit var net: NodeNetworkModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("BlockList")
    yield("SocketAddress")
    yield("Server")
    yield("Socket")
    yield("connect")
    yield("createConnection")
    yield("createServer")
    yield("getDefaultAutoSelectFamily")
    yield("getDefaultAutoSelectFamilyAttemptTimeout")
    yield("isIP")
    yield("isIPv4")
    yield("isIPv6")
  }

  @Test override fun testInjectable() {
    assertNotNull(net)
  }
}

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
import elide.runtime.node.dns.NodeDNSPromisesModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `dns` built-in module. */
@TestCase internal class NodeDnsPromisesTest : NodeModuleConformanceTest<NodeDNSPromisesModule>() {
  override val moduleName: String get() = "dns/promises"
  override fun provide(): NodeDNSPromisesModule = NodeDNSPromisesModule()
  @Inject lateinit var dns: NodeDNSPromisesModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("Resolver")
    yield("getServers")
    yield("lookupService")
    yield("resolve")
    yield("resolve4")
    yield("resolve6")
    yield("resolveAny")
    yield("resolveCname")
    yield("resolveCaa")
    yield("resolveMx")
    yield("resolveNaptr")
    yield("resolveNs")
    yield("resolvePtr")
    yield("resolveSoa")
    yield("resolveSrv")
    yield("resolveTxt")
    yield("reverse")
    yield("setDefaultResultOrder")
    yield("getDefaultResultOrder")
  }

  @Test override fun testInjectable() {
    assertNotNull(dns)
  }
}

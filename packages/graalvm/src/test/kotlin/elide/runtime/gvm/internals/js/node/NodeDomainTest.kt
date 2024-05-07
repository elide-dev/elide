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
import elide.runtime.gvm.internals.node.domain.NodeDomainModule
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `domain` built-in module. */
@TestCase internal class NodeDomainTest : NodeModuleConformanceTest<NodeDomainModule>() {
  override val moduleName: String get() = "domain"
  override fun provide(): NodeDomainModule = NodeDomainModule()
  @Inject lateinit var domain: NodeDomainModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("create")
    yield("Domain")
  }

  @Test override fun testInjectable() {
    assertNotNull(domain)
  }
}

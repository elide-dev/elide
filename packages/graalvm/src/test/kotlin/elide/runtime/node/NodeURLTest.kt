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
import elide.runtime.node.url.NodeURLModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `url` built-in module. */
@TestCase internal class NodeURLTest : NodeModuleConformanceTest<NodeURLModule>() {
  override val moduleName: String get() = "url"
  override fun provide(): NodeURLModule = NodeURLModule()
  @Inject lateinit var url: NodeURLModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("URL")
    yield("URLSearchParams")
    yield("domainToASCII")
    yield("domainToUnicode")
    yield("fileURLToPath")
    yield("pathToFileURL")
    yield("urlToHttpOptions")
  }

  @Test override fun testInjectable() {
    assertNotNull(url)
  }
}

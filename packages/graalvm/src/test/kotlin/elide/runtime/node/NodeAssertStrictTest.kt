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
import elide.annotations.Inject
import elide.runtime.intrinsics.js.node.AssertStrictAPI
import elide.runtime.node.asserts.NodeAssertStrictModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `assert/strict` built-in module. */
@TestCase internal class NodeAssertStrictTest : NodeModuleConformanceTest<NodeAssertStrictModule>() {
  override val moduleName: String get() = "assert/strict"
  override fun provide(): NodeAssertStrictModule = NodeAssertStrictModule()
  @Inject lateinit var assert: AssertStrictAPI

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("AssertionError")
    yield("ok")
    yield("fail")
    yield("strict")
    yield("deepEqual")
    yield("deepStrictEqual")
    yield("notDeepEqual")
    yield("notDeepStrictEqual")
    yield("match")
    yield("doesNotMatch")
    yield("throws")
    yield("doesNotThrow")
    yield("rejects")
    yield("doesNotReject")
    yield("ifError")
  }

  @Test override fun testInjectable() {
//    assertNotNull(assert)
  }
}

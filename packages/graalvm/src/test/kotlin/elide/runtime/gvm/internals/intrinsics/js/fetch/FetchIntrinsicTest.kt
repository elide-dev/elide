/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.gvm.internals.intrinsics.js.fetch

import org.junit.jupiter.api.Assertions.assertNotNull
import elide.annotations.Inject
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for the Fetch API intrinsics provided by Elide. */
@TestCase internal class FetchIntrinsicTest : AbstractJsIntrinsicTest<FetchIntrinsic>() {
  // Injected fetch intrinsic under test.
  @Inject internal lateinit var fetch: FetchIntrinsic

  override fun provide(): FetchIntrinsic = fetch

  @Test override fun testInjectable() {
    assertNotNull(fetch, "should be able to inject fetch intrinsic instance")
  }
}

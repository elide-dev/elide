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

package elide.tool.testing

import io.micronaut.context.annotation.Bean
import elide.tool.annotations.EmbeddedTest

/** Basic sanity self-test. */
@Bean @EmbeddedTest class SanitySelfTest : SelfTest() {
  override suspend fun SelfTestContext.test() {
    assertNotNull(this, "`this` should never be `null`")
    assertEquals(2, 1 + 1, "math should work")
  }
}

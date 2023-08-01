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

package elide.util

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Tests for generating UUIDs via [UUID]. */
class UUIDTest {
  @Test fun testGenerateUuidRandom() {
    val generated = UUID.random()
    assertNotNull(
      generated,
      "should not get `null` for generated UUID"
    )
    for (i in 0 until 10) {
      assertNotEquals(
        generated,
        UUID.random(),
        "should not get same ID for consecutive calls to `UUID.random`"
      )
    }
  }

  @Test fun testGenerateUuidConsistentCasing() {
    val generated = UUID.random()
    assertNotNull(
      generated,
      "should not get `null` for generated UUID"
    )
    // there should be no lowercase letters in the UUID
    assertNull(
      generated.find { it.isLowerCase() },
      "should not have lowercase letters in generated UUID"
    )
  }
}

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

package elide.tool.testing

/**
 * TBD.
 */
@JvmInline value class TestInfo private constructor (private val info: TestInfoRecord) {
  /**
   * TBD.
   */
  @JvmRecord internal data class TestInfoRecord(
    val name: String,
  )

  /** @return Name of the test. */
  val name: String get() = info.name

  companion object {
    /**
     * TBD.
     */
    @Suppress("UNUSED_PARAMETER")
    @JvmStatic fun <C: TestContext, T: Testable<C>> of(name: String, case: T) = TestInfo(TestInfoRecord(
      name = name,
    ))
  }
}

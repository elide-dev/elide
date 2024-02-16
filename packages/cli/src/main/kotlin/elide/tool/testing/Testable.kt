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

/**
 * TBD.
 */
@FunctionalInterface
interface Testable<Context> where Context: TestContext {
  /**
   * TBD.
   */
  suspend fun runTestIn(context: Context): Unit = context.test()

  /**
   * TBD.
   */
  suspend fun Context.test()

  /**
   * TBD.
   */
  fun testInfo(): TestInfo = TestInfo.of(
    this::class.java.simpleName,
    this,
  )
}

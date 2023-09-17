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
interface TestEvent<T, C> where C: TestContext, T: Testable<C> {
  /**
   * TBD.
   */
  enum class Type {
    PENDING,
    WARMUP,
    PRE_EXECUTE,
    EXECUTE,
    POST_EXECUTE,
    RESULT,
    DONE;
  }

  /**
   * Type of the event.
   */
  val type: Type

  /**
   * Test to which this event relates.
   */
  val test: T

  /**
   * Context in which this test will run.
   */
  val context: C

  /**
   * Result from this test, if available.
   */
  val result: TestResult?
}

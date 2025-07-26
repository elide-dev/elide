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
package elide.tooling.testing

/**
 * ## Test Result
 *
 * Describes the resulting terminal state of a single test case execution run. Tests either pass, fail, or are skipped
 * due to some condition.
 */
public sealed interface TestResult {
  /**
   * ### Test pass.
   *
   * The test was executed without error.
   */
  public data object Pass : TestResult

  /**
   * ### Test fail.
   *
   * The test was executed and failed.
   *
   * @property cause Optional exception describing the failure.
   */
  @JvmRecord public data class Fail(public val cause: Throwable? = null) : TestResult

  /**
   * ### Test skip.
   *
   * The test was not executed.
   *
   * @property reason describing why the test was not executed.
   */
  @JvmInline public value class Skip(public val reason: Reason) : TestResult
}

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
package elide.runtime.intrinsics.testing

import elide.tooling.testing.TestOutcome

/**
 * ## Test Entrypoint
 *
 * Describes the actual executable entrypoint of a test, regardless of how it is executed; exceptions should be
 * captured by this entrypoint, and a return value of [TestResult] provided.
 *
 * Exceptions thrown directly by the entrypoint (or propagated from the test itself) are thrown aggressively as these
 * indicate test runner errors, rather than test failures.
 */
public fun interface TestEntrypoint {
  /**
   * ### Invoke the test.
   *
   * Load and invoke the actual test code implemented by this registered test; if the test runs but fails within its
   * primary code in any manner, [TestResult.Fail] should be returned. If the test is not executed for any reason, the
   * [TestResult.Skip] result should be returned. Otherwise, [TestResult.Pass] is expected.
   *
   * Exception state should be described to [TestResult.Fail]. Exceptions thrown by this method lead to an immediate
   * crash of the test runner.
   */
  public operator fun invoke(): TestOutcome
}

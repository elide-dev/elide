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
package elide.tooling.runner

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import elide.tooling.config.TestConfigurator
import elide.tooling.testing.TestDriver
import elide.tooling.testing.TestCase
import elide.tooling.testing.TestResult

/** Basic test runner that runs all tests sequentially in the order they are provided. */
public class SequentialTestRunner(
  drivers: Set<TestDriver<TestCase>>,
  override val events: TestConfigurator.TestEventController,
) : AbstractTestRunner(drivers) {
  override fun testFlow(tests: Sequence<TestCase>): Flow<TestResult> = tests.asFlow().map { test ->
    runTest(test)
  }
}

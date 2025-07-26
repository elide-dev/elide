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

import kotlin.time.Duration

/**
 * Results of an individual test case run.
 *
 * @property scope The scope in which the test was a member.
 * @property result The result of the test case execution.
 * @property duration The duration of the test case execution.
 */
@JvmRecord public data class TestCaseResult(
  public val scope: TestScope<*>,
  public val result: TestResult,
  public val duration: Duration,
)

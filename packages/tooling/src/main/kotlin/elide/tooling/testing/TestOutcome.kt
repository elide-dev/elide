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

public data class TestResult(
  val test: TestNodeKey,
  val outcome: TestOutcome,
  val duration: Duration,
)

public sealed interface TestOutcome {
  public data object Skipped : TestOutcome
  public data object Success : TestOutcome
  @JvmInline public value class Failure(public val reason: Any? = null) : TestOutcome
  @JvmInline public value class Error(public val reason: Any? = null) : TestOutcome
}

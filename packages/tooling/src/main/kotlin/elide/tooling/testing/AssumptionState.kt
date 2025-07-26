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
 * ## Assumption State
 *
 * Describes top-level assumption evaluation state for a registered test.
 */
public sealed interface AssumptionState {
  /**
   * ### Assumptions: Valid.
   *
   * Assumptions found no invalid conditions.
   */
  public data object Valid: AssumptionState

  /**
   * ### Assumptions: Ignored.
   *
   * The test was explicitly ignored or skipped without reasoning; this occurs when, for example, the `@Disabled` or
   * `@Ignore` annotation is affixed to JVM tests (or similar mechanisms for other languages).
   */
  public data object Ignored: AssumptionState

  /**
   * ### Assumptions: Ignored with reasoning.
   *
   * The test was ignored or skipped with reasoning; this state is adopted when either explicit reasoning is provided
   * for skipping a test (in the form of a string, usually), or one or more [ViolatedAssumption] exceptions are thrown
   * while registering or evaluating the test.
   */
  @JvmInline public value class IgnoredWithReasoning(public val reason: Reason): AssumptionState
}

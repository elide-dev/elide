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
 * ## Reason
 *
 * Reasoning for why a condition applied within a testing context; for example, why a test was ignored, or why a test
 * was found ineligible to run.
 */
public sealed interface Reason {
  /**
   * Produce a string message explaining the reason.
   *
   * @return Formatted string message explaining the reason.
   */
  public fun message(): String

  /**
   * ### Reason: Message.
   *
   * Encloses a string message as reasoning.
   */
  @JvmInline public value class ReasonMessage(public val message: String) : Reason {
    override fun message(): String = message
  }

  /**
   * ### Reason: Violated Assumptions.
   *
   * Encloses one or more [ViolatedAssumption] exceptions describing what assumptions were violated.
   */
  @JvmInline public value class ViolatedAssumptions(public val list: List<ViolatedAssumption>) : Reason {
    override fun message(): String = buildString {
      append("${list.size} violated assumptions")
      val joined = list.joinToString(", ") { it.reasonMessage() }
      if (joined.isNotEmpty()) {
        append(": ")
        append(joined)
      }
    }
  }
}

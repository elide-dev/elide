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

package elide.internal.conventions.linting

/** Defines static utilities for working with Ktlint. */
@Suppress("unused", "MemberVisibilityCanBePrivate") public object KtlintConventions {
  public enum class RuleCategory (internal val id: String) {
    STANDARD("standard"),
    CODE("code"),
  }

  public fun ktlintRule(category: RuleCategory, name: String, setting: String): Pair<String, String> =
    "ktlint_${category.id}_$name" to setting
  public fun ktlintRule(name: String, setting: String): Pair<String, String> =
    ktlintRule(RuleCategory.STANDARD, name, setting)
  public fun ktlintDisable(category: RuleCategory, name: String): Pair<String, String> =
    ktlintRule(category, name, "disabled")
  public fun ktlintDisable(name: String): Pair<String, String> =
    ktlintDisable(RuleCategory.STANDARD, name)

  public fun ktRuleset(vararg rules: Pair<String, String>): Map<String, String> = rules.toMap()
}

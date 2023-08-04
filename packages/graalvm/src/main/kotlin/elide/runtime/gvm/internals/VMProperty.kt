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

package elide.runtime.gvm.internals

/** Abstract base interface for a guest VM configuration property. */
public sealed interface VMProperty : Comparable<VMProperty> {
  /** Symbol to use for this property with the guest VM. */
  public val symbol: String

  /** @return Resolved value for this property. */
  public fun value(): String?

  /** @return Indication of whether a value is present for this property. */
  public fun active(): Boolean = when (value()) {
    "true", "yes", "on", "active", "enabled" -> true
    "false", "no", "off", "inactive", "disabled", "", " " -> false
    null -> false
    else -> true
  }

  override fun compareTo(other: VMProperty): Int {
    return this.symbol.compareTo(other.symbol)
  }
}

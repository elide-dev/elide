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

package elide.embedded.env

/**
 * # Environment Value
 *
 * Describes, in type-safe terms, a value which was resolved from an [EnvResolver] implementation; this includes a
 * reference to the value's [EnvKey] information, and whether the value was resolved and found (and if so, the value can
 * be obtained).
 */
public interface EnvValue {
  /**
   * The key which was resolved.
   */
  public val key: EnvKey

  /**
   * Whether the value was resolved and found.
   */
  public val resolved: Boolean

  /**
   * The resolved value, if found.
   */
  public val value: String?
}

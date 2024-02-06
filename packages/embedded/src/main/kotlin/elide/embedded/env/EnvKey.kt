/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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
 * # Environment Key
 *
 * Describes a single value which can be resolved from [EnvResolver] implementations; this typically pairs a "system
 * property" name with an environment variable name, both of which apply to one value.
 */
public interface EnvKey {
  /**
   * The system property name.
   */
  public val propertyName: String

  /**
   * The environment variable name.
   */
  public val envName: String
}

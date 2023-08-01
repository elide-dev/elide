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

package elide.core

/**
 * # Core: Defaults
 *
 * Specifies sensible defaults for well-known or commonly-used types or values. These configurations may change over
 * time, but they should change very slowly, and represent the best practices for the current time, with a focus on
 * interoperability.
 */
public object Defaults : PlatformDefaults {
  /**
   * ## Defaults: Charset.
   *
   * Default character set when interpreting string data, or converting string data to and from raw bytes. The default
   * character set should be overridable in almost every circumstance; this value is merely a reasonable default.
   */
  public override val charset: String = "UTF-8"
}

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

package elide.runtime.wasm

import elide.runtime.LogLevel

/** Specifies a lightweight [elide.runtime.Logger] implementation for use in pure WASM. */
public data class Logger(
  public val name: String? = null,
): elide.runtime.Logger {
  /** @inheritDoc */
  override fun isEnabled(level: LogLevel): Boolean = true  // @TODO(sgammon): conditional logging in WASM

  /** @inheritDoc */
  override fun log(level: LogLevel, message: List<Any>, levelChecked: Boolean): Unit = when (level) {
    LogLevel.TRACE,
    LogLevel.DEBUG,
    LogLevel.INFO,
    LogLevel.WARN,
    LogLevel.ERROR -> println(message.toTypedArray())
  }
}

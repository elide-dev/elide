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
package elide.runtime.telemetry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName(CRASH_EVENT_NAME)
public data class CrashEvent internal constructor (
  val exitCode: Int? = null,
  val message: String? = null,
): Event {
  public companion object {
    @JvmStatic public fun create(exitCode: Int? = null, message: String? = null): CrashEvent {
      return CrashEvent(
        exitCode = exitCode,
        message = message,
      )
    }
  }
}

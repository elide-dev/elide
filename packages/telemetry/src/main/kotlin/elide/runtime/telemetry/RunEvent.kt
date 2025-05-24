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

package elide.runtime.telemetry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@Serializable
@SerialName(RUN_EVENT_NAME)
public data class RunEvent internal constructor (
  public val exitCode: Int? = null,
  public val duration: Long? = null,
  public val mode: ExecutionMode? = null,
): Event {
  /**
   * Execution modes for a run event.
   */
  public enum class ExecutionMode {
    @SerialName(RUN_MODE_RUN) Run,
    @SerialName(RUN_MODE_SERVE) Serve,
    @SerialName(RUN_MODE_TEST) Test,
  }

  public companion object {
    @JvmStatic public fun create(mode: ExecutionMode, exitCode: Int, duration: Duration? = null): RunEvent {
      return RunEvent(mode = mode, exitCode = exitCode, duration = duration?.inWholeMilliseconds)
    }
  }
}

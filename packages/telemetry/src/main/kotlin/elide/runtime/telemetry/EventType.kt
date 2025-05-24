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

import kotlinx.serialization.Serializable
import elide.core.api.Symbolic

@Serializable
public enum class EventType (override val symbol: String): Symbolic<String> {
  Run(RUN_EVENT_NAME),
  Crash(CRASH_EVENT_NAME);

  public companion object: Symbolic.SealedResolver<String, EventType> {
    override fun resolve(symbol: String): EventType = when (symbol) {
      RUN_EVENT_NAME -> Run
      CRASH_EVENT_NAME -> Crash
      else -> throw unresolved("Unknown event type: $symbol")
    }
  }
}

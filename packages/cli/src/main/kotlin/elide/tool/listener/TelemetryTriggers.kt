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

package elide.tool.listener

import io.micronaut.context.event.ApplicationEventPublisher
import kotlin.time.Duration
import elide.annotations.Context
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.telemetry.CrashEvent
import elide.runtime.telemetry.Event
import elide.runtime.telemetry.RunEvent
import elide.runtime.telemetry.RunEvent.ExecutionMode
import elide.runtime.telemetry.manager.TelemetryManager

@Singleton
@Context
class TelemetryTriggers @Inject constructor (
  private val telemetryManager: TelemetryManager,
  private val publisher: ApplicationEventPublisher<Event>,
) {
  fun sendRunEvent(mode: ExecutionMode, exitCode: Int, duration: Duration) {
    publisher.publishEvent(RunEvent.create(mode, exitCode, duration))
  }

  fun sendCrashEvent(exitCode: Int = 1, message: String? = null) {
    publisher.publishEvent(CrashEvent.create(exitCode, message))
  }

  fun manager(): TelemetryManager = telemetryManager
}

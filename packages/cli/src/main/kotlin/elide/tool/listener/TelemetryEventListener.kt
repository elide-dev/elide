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

import io.micronaut.context.event.ApplicationEventListener
import elide.annotations.Context
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.telemetry.Event
import elide.runtime.telemetry.manager.TelemetryManager

@Singleton
@Context
class TelemetryEventListener @Inject constructor (
  private val manager: TelemetryManager,
): ApplicationEventListener<Event> {
  override fun supports(event: Event): Boolean = true

  override fun onApplicationEvent(event: Event) {
    manager.deliver(event)
  }
}

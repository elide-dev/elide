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
package elide.runtime.telemetry.client

import elide.runtime.telemetry.Event
import kotlinx.coroutines.Deferred

/**
 * # Telemetry Client
 *
 * Describes the API surface for a telemetry client; telemetry messages are created and emitted by Elide (when enabled)
 * for various runtime events, like regular executions, crashes, and other things. The telemetry client delivers data
 * which is relevant to Elide's development: latencies for executions, errors, and general usage patterns (which langs
 * and other features are used/preferred, etc).
 *
 * No data is ever included with these messages which originates from the user's environment, which may be used to
 * detect or otherwise identify the user, their environment, or their application or usage patterns. IP addresses are
 * not logged along with telemetry events.
 */
public interface TelemetryClient {
  /**
   * Spawn a deferred job to deliver an event; this is a non-blocking call.
   *
   * Events are enqueued for background transmission so as not to block the caller. Calls are canceled or otherwise
   * flushed at runtime shutdown, and may be subject to sampling or other filtering mechanisms. Telemetry can always be
   * disabled by end-users via several mechanisms.
   *
   * @param event Event to deliver.
   * @return Deferred event delivery result.
   */
  public suspend fun <E: Event> deliver(event: E): Deferred<EventDelivery>
}

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
package elide.runtime.telemetry.manager

import elide.runtime.telemetry.Event
import elide.runtime.telemetry.client.TelemetryClient

/**
 * # Telemetry Manager
 *
 * An implementation of the telemetry manager is created (as a singleton) at runtime, and is configured to either send
 * telemetry or disable all events, based on the user's preferences. Once obtained, events are handed to the manager,
 * which typically moves them to background threads for delivery.
 */
public interface TelemetryManager {
  /**
   * Disable all telemetry traffic.
   *
   * This will cease to deliver any telemetry events after this call completes; telemetry events which are currently
   * in-flight (if any) may also be cancelled.
   */
  public fun disableTelemetry()

  /**
   * Enable configured telemetry traffic.
   *
   * This will re-enable telemetry events after previously being disabled via [disableTelemetry]. If telemetry was never
   * disabled, this call will have no effect. No additional telemetry events are configured by this call; it simply
   * resumes telemetry traffic if it was previously disabled.
   */
  public fun enableTelemetry()

  /**
   * Obtain an instance of the telemetry client.
   *
   * @return Telemetry client instance
   */
  public fun client(): TelemetryClient

  /**
   * Deliver a telemetry event via the telemetry client.
   *
   * @param event Event to deliver
   * @param E Type of event
   */
  public fun <E: Event> deliver(event: E)
}

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

/**
 * # Event Delivery
 *
 * Describes the result of delivering an event to Elide's telemetry system; properties made available here are always
 * available, regardless of the delivery method used.
 *
 * @see TelemetryClient telemetry client
 */
public interface EventDelivery {
  public val success: Boolean

  /**
   * ## Event Delivered
   *
   * Yielded when an event is successfully delivered to Elide's telemetry system.
   */
  public data object EventDelivered: EventDelivery {
    override val success: Boolean = true
  }

  /**
   * ## Event Delivery Failure
   *
   * Yielded when an event fails to be delivered to Elide's telemetry system.
   */
  public data object EventFailure: EventDelivery {
    override val success: Boolean = false
  }
}

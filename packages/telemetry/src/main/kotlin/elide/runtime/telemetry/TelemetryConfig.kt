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

/**
 * Provides access to static telemetry service configuration.
 */
public object TelemetryConfig {
  public const val ENABLED: Boolean = false
  public const val HOSTNAME: String = "telemetry.elide.dev"
  public const val PATH: String = "/v1/event:submit"
  public const val ENDPOINT: String = "https://$HOSTNAME$PATH"
}

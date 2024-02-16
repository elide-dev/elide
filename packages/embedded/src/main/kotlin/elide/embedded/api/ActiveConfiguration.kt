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

package elide.embedded.api

import tools.elide.call.HostConfiguration
import elide.annotations.API

/**
 * # Active Configuration
 *
 * Represents the merged, interpreted, and activated instance configuration for a given run of the Elide Embedded
 * runtime; this injectable type wraps all configuration settings necessary to start and run the server.
 */
@API public interface ActiveConfiguration {
  /**
   * Access to merged host-level configuration for this instance of Elide.
   */
  public val host: HostConfiguration
}

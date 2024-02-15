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

import elide.annotations.API
import elide.annotations.Singleton

/**
 * # Configuration Manager
 *
 * Responsible for gathering all available configurations at instance boot time, and then merging them just before the
 * instance starts.
 */
@API public interface ConfigurationManager {
  /**
   * Provide the final instance configuration, merging all mounted configurations.
   *
   * After this call, the instance is expected to be "locked" from further modification. The returned configuration
   * object (an [InstanceConfiguration]) is expected to be read-only.
   *
   * @return The final instance configuration.
   */
  @Singleton public fun provide(): ActiveConfiguration
}

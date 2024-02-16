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

package elide.server.controller

import io.micronaut.context.ApplicationContext
import elide.server.assets.AssetManager

/**
 * Describes the top-level expected interface for Elide-based controllers; any base class which inherits from this one
 * may be used as a controller, and activated/deactivated with Micronaut annotations (see: `@Controller`).
 */
public interface ElideController {
  /** @return Access to the active asset manager. */
  public fun assets(): AssetManager

  /** @return Access to the active application context. */
  public fun context(): ApplicationContext
}

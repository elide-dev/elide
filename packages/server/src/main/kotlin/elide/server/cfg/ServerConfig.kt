/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.server.cfg

import io.micronaut.context.annotation.ConfigurationProperties

/**
 * Configuration properties loaded at runtime through Micronaut's configuration system, which govern how Elide hosts
 * server-side code.
 */
@ConfigurationProperties("elide.server")
public data class ServerConfig(
  public var assets: AssetConfig = object : AssetConfig {},
)

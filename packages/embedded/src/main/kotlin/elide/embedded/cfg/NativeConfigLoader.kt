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

package elide.embedded.cfg

import tools.elide.call.HostConfiguration
import jakarta.inject.Provider
import elide.annotations.Component
import elide.embedded.api.InstanceConfiguration
import elide.embedded.api.InstanceConfigurationFactory
import elide.embedded.api.NativeConfiguration

/**
 * # Native Configuration Loader
 *
 * Designed to be mounted manually as an eager singleton, before application initialization, by a build-time
 * configurator class. Provides native configuration to the application, as proposed by an embedded runtime host.
 */
public class NativeConfigLoader private constructor (
  private val config: Provider<NativeConfiguration>,
) : InstanceConfigurationFactory {
  /** Methods to create a native configuration loader in static contexts. */
  public companion object {
    @JvmStatic public fun create(config: Provider<NativeConfiguration>): NativeConfigLoader = NativeConfigLoader(config)
  }

  // Merge provided native configuration to produce a final rendered configuration.
  private fun apply(native: NativeConfiguration): InstanceConfiguration {
    return InstanceConfiguration.createFrom(native.applyTo(HostConfiguration.newBuilder()).build())
  }

  @Component override fun provide(): InstanceConfiguration = apply(config.get())
}

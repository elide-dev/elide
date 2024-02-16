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
import elide.annotations.Factory
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.embedded.api.ActiveConfiguration
import elide.embedded.api.ConfigurationManager
import elide.embedded.api.InstanceConfiguration
import elide.embedded.api.InstanceConfigurationFactory

/** Default logic for merging and mounting instance configuration. */
@Singleton @Factory internal class DefaultConfigManager @Inject constructor(
  private val defaults: DefaultConfigFactory,
  private val factories: Collection<InstanceConfigurationFactory>,
) : ConfigurationManager {
  // Merge and build configurations from all available configuration factories, starting with defaults.
  private fun mergeAndBuild(): InstanceConfiguration {
    return InstanceConfiguration.createFrom(factories.filter {
      it !== defaults
    }.fold(defaults.defaults.toBuilder()) { acc, factory ->
      factory.provide()?.host?.let { stanza ->
        acc.mergeFrom(stanza)
      }
    }.build())
  }

  @Singleton override fun provide(): ActiveConfiguration = mergeAndBuild().let {
    object: ActiveConfiguration {
      override val host: HostConfiguration get() = it.host
    }
  }
}

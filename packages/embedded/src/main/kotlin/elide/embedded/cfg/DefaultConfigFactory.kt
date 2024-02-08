/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

import com.google.protobuf.Value
import tools.elide.app.appConfigurationSuite
import tools.elide.app.configurationValue
import tools.elide.call.HostConfiguration
import tools.elide.call.VMConfigurationKt
import tools.elide.meta.GuestPolicy
import tools.elide.meta.guestPolicy
import tools.elide.std.LogLevel.INFO
import tools.elide.std.logHandler
import tools.elide.std.logger
import tools.elide.std.loggerSettings
import java.net.InetAddress
import elide.annotations.Component
import elide.annotations.Factory
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.embedded.api.Constants.Defaults
import elide.embedded.api.Constants.Environment
import elide.embedded.api.Constants.SystemProperty
import elide.embedded.api.InstanceConfiguration
import elide.embedded.api.InstanceConfigurationFactory
import elide.embedded.env.EnvResolver

/** Configuration factory which produces defaults. */
@Singleton @Factory internal class DefaultConfigFactory @Inject constructor(
  private val envResolver: EnvResolver,
) : InstanceConfigurationFactory {
  private companion object {
    private const val DEFAULT_STACK_DEPTH = 20_000
    private const val DEFAULT_MAX_MEMORY = 2_000_000_000  // 2GB
  }

  // Default instance configuration as a built immutable.
  internal val defaults: HostConfiguration = HostConfiguration.newBuilder().apply { buildDefaults() }.build()

  // Fetch a default from the environment and from system properties.
  private fun defaultSetting(prop: String, env: String, default: String? = null, then: (String) -> Unit) {
    (envResolver.resolve(env to prop)?.ifBlank { null } ?: default)?.let {
      then(it)
    }
  }

  // Formulate a default guest access policy.
  private fun defaultGuestPolicy(): GuestPolicy = guestPolicy {
    sandbox = true
    threads = true
    stackDepth = DEFAULT_STACK_DEPTH
    maxMemory = DEFAULT_MAX_MEMORY
  }

  // Build defaults for configuration.
  private fun HostConfiguration.Builder.buildDefaults() {
    // top-level engine settings, set default policy
    engineBuilder.caching = true
    engineBuilder.generalBuilder.enabled = true
    engineBuilder.generalBuilder.setDefaultPolicy(defaultGuestPolicy())

    // gather host information
    hostBuilder.hostname = InetAddress.getLocalHost().hostName
    defaultSetting(
      SystemProperty.DATACENTER,
      Environment.DATACENTER,
      Defaults.DATACENTER,
    ) {
      hostBuilder.datacenter = it
    }
    defaultSetting(
      SystemProperty.REGION,
      Environment.REGION,
      Defaults.REGION,
    ) {
      hostBuilder.region = it
    }

    // default logging configuration
    loggingBuilder.rootBuilder.level = INFO
    loggingBuilder.rootBuilder.addHandler("stderr")
    loggingBuilder.addHandler(logHandler {
      name = "stderr"
    })
    loggingBuilder.addHandler(logHandler {
      name = "stdout"
    })
    loggingBuilder.addLogger(logger {
      root = true
      settings = loggerSettings {
        level = INFO
      }
    })

    // outer engine configuration, default app configuration
    addConfig(
      appConfigurationSuite {
      name = "defaults"
      config.add(configurationValue {
        key = "elide.embedded"
        value = Value.newBuilder().setBoolValue(true).build()
      })
    }
    )
    addDefaults(
      appConfigurationSuite {
      name = "defaults"
      config.add(configurationValue {
        key = "elide.embedded"
        value = Value.newBuilder().setBoolValue(true).build()
      })
      config.add(configurationValue {
        key = "elide.guest"
        value = Value.newBuilder().setBoolValue(true).build()
      })
    }
    )
  }

  @Component override fun provide(): InstanceConfiguration = InstanceConfiguration.createFrom(defaults)
}

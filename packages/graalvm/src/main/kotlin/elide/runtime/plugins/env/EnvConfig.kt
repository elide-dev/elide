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
package elide.runtime.plugins.env

import java.util.concurrent.ConcurrentSkipListMap
import elide.runtime.EnvVar
import elide.runtime.core.DelicateElideApi

/**
 * Defines configuration options for built-in managed guest access to the host environment.
 *
 * This container is meant to be used by the [Environment] plugin.
 */
@DelicateElideApi public class EnvConfig internal constructor() {
  /** Configuration for managed application environment.  */
  @DelicateElideApi public class AppEnvConfig internal constructor() {
    /**
     * Whether to enable managed environment features.
     */
    public var enabled: Boolean = true

    /**
     * Isolated suite of environment variables to provide to the guest application. Use [setEnv] to register new
     * variable values or override existing ones.
     */
    public val isolatedEnvironmentVariables: Map<String, EnvVar> get() = mutableEnvironmentVariables

    /**
     * Private mutable map for setting environment variables.
     */
    private val mutableEnvironmentVariables: MutableMap<String, EnvVar> = ConcurrentSkipListMap()

    /** Register an [EnvVar] with the given [key]. */
    public fun setEnv(key: String, value: EnvVar) {
      mutableEnvironmentVariables[key] = value
    }
  }

  /** Environment settings which apply to the guest application. */
  internal val app: AppEnvConfig = AppEnvConfig()

  /** Configure application environment. */
  public fun environment(block: AppEnvConfig.() -> Unit) {
    app.enabled = true
    app.apply(block)
  }

  /** Inject an explicit application environment variable. */
  public fun environment(name: String, value: String?) {
    if (value == null) return
    if (!app.enabled) app.enabled = true
    app.setEnv(name, EnvVar.of(name, value))
  }

  /** Inject an explicit application environment variable resolved from a [callback]. */
  public fun environment(name: String, callback: () -> String?) {
    if (!app.enabled) app.enabled = true
    app.setEnv(name, EnvVar.provide(name, callback))
  }

  /** Expose or map a [hostVariable] to the provided [alias] (defaults to the same name). */
  public fun mapToHostEnv(hostVariable: String, alias: String = hostVariable, defaultValue: String? = null) {
    if (!app.enabled) app.enabled = true
    app.setEnv(alias, EnvVar.mapToHost(hostVariable, alias, defaultValue))
  }

  /** Expose or map a [hostVariable] to the provided alias (defaults to the same name). */
  public fun exposeHostEnv(hostVariable: String, defaultValue: String? = null) {
    mapToHostEnv(hostVariable, defaultValue = defaultValue)
  }

  /** Inject an environment variable at [name] that was loaded from a `.env` [file]. */
  public fun fromDotenv(file: String, name: String, value: String?) {
    if (!app.enabled) app.enabled = true
    app.setEnv(name, EnvVar.fromDotenv(file, name, value))
  }
}


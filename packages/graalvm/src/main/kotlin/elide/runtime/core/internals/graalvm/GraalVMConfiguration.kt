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

package elide.runtime.core.internals.graalvm

import elide.runtime.core.*
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.internals.MutableEngineLifecycle

/**
 * Internal implementation of the [PolyglotEngineConfiguration] abstract class, specialized for the GraalVM engine
 * implementation.
 */
@DelicateElideApi internal class GraalVMConfiguration(
  /** A [MutableEngineLifecycle] instance that can be used to emit events to registered plugins. */
  private val lifecycle: MutableEngineLifecycle
) : PolyglotEngineConfiguration() {
  /**
   * Represents an [InstallationScope] used by plugins, binding to this configuration's lifecycle and other required
   * properties.
   */
  @JvmInline private value class GraalVMInstallationScope(val config: GraalVMConfiguration) : InstallationScope {
    override val configuration: PolyglotEngineConfiguration get() = config
    override val lifecycle: EngineLifecycle get() = config.lifecycle
  }

  /** Internal map holding plugin instances that can be retrieved during engine configuration. */
  private val plugins: MutableMap<String, Any?> = mutableMapOf()

  /** Internal mutable set of enabled languages. */
  private val langs: MutableSet<GuestLanguage> = mutableSetOf()

  /** A set of languages enabled for use in the engine. */
  internal val languages: Set<GuestLanguage> get() = langs

  override fun <C : Any, I : Any> install(plugin: EnginePlugin<C, I>, configure: C.() -> Unit): I {
    require(plugin.key.id !in plugins) { "A plugin with the provided key is already registered" }

    val instance = plugin.install(GraalVMInstallationScope(this), configure)
    plugins[plugin.key.id] = instance

    return instance
  }

  override fun <T> plugin(key: EnginePlugin.Key<T>): T? {
    @Suppress("unchecked_cast")
    return plugins[key.id] as? T
  }

  override fun enableLanguage(language: GuestLanguage) {
    langs.add(language)
  }
}

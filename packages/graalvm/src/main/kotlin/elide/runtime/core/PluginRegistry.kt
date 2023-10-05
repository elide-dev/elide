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

package elide.runtime.core

/**
 * The [PluginRegistry] interface defines the base contract for classes that allow installing and managing
 * [engine plugins][EnginePlugin].
 */
@DelicateElideApi public interface PluginRegistry {
  /**
   * Install a [plugin] and [configure] it.
   *
   * @param plugin A plugin identifier representing the plugin to be installed.
   * @param configure A configuration DSL block to be applied to the plugin on installation.
   * @return A plugin instance added to the registry after installation.
   */
  public fun <C : Any, I : Any> install(plugin: EnginePlugin<C, I>, configure: C.() -> Unit = { }): I

  /**
   * Retrieve a plugin instance identified by the provided [key] from this registry. If no
   * instance is registered for that key, `null` will be returned.
   *
   * @param key A plugin key identifying the instance to be retrieved.
   * @return A plugin instance associated with the given key, or null if none was found.
   */
  public fun <T> plugin(key: EnginePlugin.Key<T>): T?
}

/**
 * Returns the plugin instance associated with the given [key], or throws [IllegalStateException] of no plugin instance
 * is found.
 */
@DelicateElideApi public fun <T> PluginRegistry.requirePlugin(key: EnginePlugin.Key<T>): T {
  return plugin(key) ?: error("Plugin not installed: ${key.id}")
}

/**
 * Returns the installed [plugin] instance from the registry, or null if this [plugin] is not installed.
 */
@DelicateElideApi public fun <T : Any> PluginRegistry.plugin(plugin: EnginePlugin<*, T>): T? {
  return plugin(plugin.key)
}

/**
 * Returns the installed [plugin] instance from the registry, or throws [IllegalStateException] of no plugin instance
 * is found.
 */
@DelicateElideApi public fun <T : Any> PluginRegistry.requirePlugin(plugin: EnginePlugin<*, T>): T {
  return plugin(plugin.key) ?: error("Plugin not found: ${plugin.key.id}")
}

/**
 * Returns the installed [plugin] instance from the registry, or throws [IllegalStateException] of no plugin instance
 * is found.
 */
@DelicateElideApi public fun <C : Any, T : Any> PluginRegistry.getOrInstall(
  plugin: EnginePlugin<C, T>,
  configure: C.() -> Unit = { },
): T {
  return plugin(plugin.key) ?: install(plugin, configure)
}


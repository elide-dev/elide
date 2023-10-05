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
 * Engine plugins provide a generic way to extend a [PolyglotEngine] and its [PolyglotContext] instances using an
 * intuitive configuration DSL:
 * 
 * ```kotlin
 * val engine = PolyglotEngine {
 *  // install support for specific languages
 *  install(JavaScript) {
 *    esm = true
 *
 *    intrinsics {
 *      console()
 *      base64()
 *      json()
 *    }
 *  }
 * }
 * ```
 *
 * Plugins can subscribe to [events][EngineLifecycleEvent] to intercept engine and context creation, and adjust options
 * using builders.
 *
 * ### Writing engine plugins
 * 
 * Plugins have three main components:
 * 1. The plugin marker, which is typically a singleton (such as a companion object), that can be used as identifier in
 * the [PluginRegistry.install] method.
 * 2. The configuration class, used as received for the DSL provided during installation.
 * 3. The plugin instance, which is attached to the engine after configuration.
 *
 * A simple plugin implementation may look like this:
 * 
 * ```kotlin
 * // This class is used for instances of the plugin
 * class MyPlugin(private val config: MyConfig) {
 *  // A custom configuration class than defines this plugin's DSL
 *  class MyConfig {
 *    var message: String = "Hello"
 *  }
 *
 *  // a simple function to be called from the engine creation event listener
 *  fun greetEngine() {
 *    println(config.message)
 *  }
 *
 *  // a simple function to be called from the context creation event listener
 *  fun greetContext() {
 *    println(config.message)
 *  } 
 *
 *  // The companion object is used as plugin identifier
 *  companion object : EnginePlugin<MyConfig, MyPlugin> {
 *    // This key will identify the plugin in the registry
 *    override val key = Key("MyPlugin")
 *
 *    // This event will be called by the DSL to obtain the plugin instance that
 *    // will be added to the plugin registry
 *    override fun install(
 *      scope: InstallationScope,
 *      configuration: Config.() -> Unit
 *    ): Instance {
 *      // we receive the DSL function, so we can instantiate the configuration
 *      // class as needed, don't forget to apply the user configuration!
 *      val config = MyConfig().apply(configuration)
 *
 *      // now we create the plugin instance that will live in the engine
 *      val instance = MyPlugin(config)
 *
 *      // we can subscribe to lifecycle events using the installation scope
 *      scope.lifecycle.on(EngineCreated) { instance.greetEngine() }
 *      scope.lifecycle.on(ContextCreated) { instance.greetContext() }
 *
 *      // it is also possible to detect and install other plugins in the scope
 *      scope.configuration.plugin(OtherPlugin)?.let {
 *        println("OtherPlugin is installed")
 *      }
 *
 *      // finally, we return the instance
 *      return instance
 *    }
 *  }
 * }
 * ```
 */
@DelicateElideApi public interface EnginePlugin<Config : Any, Instance : Any> {
  /**
   * Plugin keys identify a plugin within the registry, allowing it to be retrieved after installation, and avoiding
   * redundant applications. Keys are type-safe, and specify the type of the plugin instance they represent.
   */
  @DelicateElideApi @JvmInline public value class Key<@Suppress("unused") T>(public val id: String)

  /**
   * Encapsulates the scope provided to an [EnginePlugin] during the [install] event, allowing plugins to access the
   * [lifecycle] and [configuration] during installation.
   */
  @DelicateElideApi public interface InstallationScope {
    /**
     * The lifecycle for the engine being configured. Use this to receive specific events while the plugin instance is
     * attached to the engine.
     */
    public val lifecycle: EngineLifecycle

    /**
     * The general configuration scope, used for retrieving installed plugins, as well as installing additional ones.
     * Generic engine options can also be updated through this property.
     */
    public val configuration: PolyglotEngineConfiguration
  }

  /**
   * Identifies the plugin's instances, so they can be retrieved after being applied to the engine, avoiding the need
   * for global references.
   */
  public val key: Key<Instance>

  /**
   * Install this plugin into the provided [scope], using the [configuration] DSL block to adjust settings.
   * This method is called from [PluginRegistry.install] to obtain a plugin [Instance] that will be added to the
   * registry.
   *
   * @param scope An [InstallationScope] providing access to [EngineLifecycle] and [PolyglotEngineConfiguration] APIs.
   * @param configuration A DSL configuration block to be applied to an instance of this plugin's [Config] class.
   * @return A plugin instance to be added to the plugin registry.
   */
  public fun install(scope: InstallationScope, configuration: Config.() -> Unit): Instance
}

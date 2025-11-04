/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyHashMap
import org.graalvm.polyglot.proxy.ProxyIterable
import org.graalvm.polyglot.proxy.ProxyIterator
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.core.*
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.vm.annotations.Polyglot

/**
 * Engine plugin providing isolated application environment.
 *
 * In order to correctly support all languages, this plugin is cooperative: a language plugin may opt into
 * using it by [installing][install] the env map binding into a context.
 *
 * @see EnvConfig
 */
@DelicateElideApi public class Environment private constructor(public val config: EnvConfig) {
  /** Collect the configured environment variables and keep the ones currently present. */
  private val effectiveEnvironment: Map<String, String> by lazy {
    config.app.isolatedEnvironmentVariables
      .filter { it.value.isPresent }
      .mapValues { it.value.value!! }
  }

  private inner class DeferredEnvMap : Map<String, String> {
    override operator fun get(key: String): String? = effectiveEnvironment[key]
    override val size: Int get() = effectiveEnvironment.size
    override val keys: Set<String> get() = effectiveEnvironment.keys
    override val entries: Set<Map.Entry<String, String>> get() = effectiveEnvironment.entries
    override val values: Collection<String> get() = effectiveEnvironment.values
    override fun containsKey(key: String): Boolean = key in effectiveEnvironment
    override fun containsValue(value: String): Boolean = effectiveEnvironment.containsValue(value)
    override fun isEmpty(): Boolean = false
  }

  /** A proxied map for guest language access to env; it should not be mutable. */
  private val proxiedEnvMap: ProxyHashMap by lazy {
    object : ProxyHashMap, ProxyIterable, ProxyObject {
      @Polyglot override fun getHashSize(): Long = effectiveEnvironment.size.toLong()
      @Polyglot override fun hasHashEntry(key: Value): Boolean = effectiveEnvironment.contains(key.asString())
      @Polyglot override fun getHashValue(key: Value): Any? = effectiveEnvironment[key.asString()]
      @Polyglot override fun getHashEntriesIterator(): Any = effectiveEnvironment.entries
      @Polyglot override fun putHashEntry(key: Value, value: Value) {
        throw UnsupportedOperationException("Elide forbids writes to process environment after VM boot")
      }

      @Polyglot override fun getMember(key: String): Any? = effectiveEnvironment[key]
      @Polyglot override fun getMemberKeys(): Any = effectiveEnvironment.keys.toTypedArray()
      @Polyglot override fun hasMember(key: String): Boolean = effectiveEnvironment.contains(key)
      @Polyglot override fun putMember(key: String, value: Value?) {
        throw UnsupportedOperationException("Elide forbids writes to process environment after VM boot")
      }

      @Polyglot override fun getIterator(): Any {
        val iterator = effectiveEnvironment.keys.iterator()
        return object : ProxyIterator {
          override fun hasNext(): Boolean = iterator.hasNext()
          override fun getNext(): Any = iterator.next()
        }
      }
    }
  }

  /** Apply an environment [config] to a context [builder] during the [ContextCreated] event. */
  internal fun onContextCreate(builder: PolyglotContextBuilder) {
    if (!config.app.enabled) return

    // apply configured app environment
    builder.environment(DeferredEnvMap())
  }

  /**
   * Install the application environment bindings into the target [context] for a given [language]. This is only
   * necessary if polyglot bindings are not accessible in the target language.
   *
   * This method should be used by language plugins to opt into using the virtual environment support, and should be
   * called during the [ContextInitialized][elide.runtime.core.EngineLifecycleEvent.ContextInitialized] event.
   */
  public fun configure(scope: InstallationScope, context: PolyglotContext, language: GuestLanguage) {
    scope.deferred {
      context.bindings(language).putMember(APP_ENV_BIND_PATH, proxiedEnvMap)

      // must be added to both language bindings and polyglot bindings, so that it surfaces in all languages.
      context.unwrap().polyglotBindings.putMember(APP_ENV_BIND_PATH_FACTORY, ProxyExecutable {
        proxiedEnvMap
      })
    }
  }

  /** Identifier for the [Environment] plugin, which provides isolated application environment. */
  public companion object Plugin : EnginePlugin<EnvConfig, Environment> {
    /** Binding path for the env container. */
    private const val APP_ENV_BIND_PATH = "app_env"
    private const val APP_ENV_BIND_PATH_FACTORY = "app_env_factory"

    override val key: Key<Environment> = Key("Environment")

    override fun install(scope: InstallationScope, configuration: EnvConfig.() -> Unit): Environment {
      // apply the configuration and create the plugin instance
      val config = EnvConfig().apply(configuration)
      val instance = Environment(config)

      // subscribe to lifecycle events
      scope.lifecycle.on(ContextCreated, instance::onContextCreate)

      return instance
    }

    /**
     * Returns the value providing access to the environment map installed in the given [context] for a specific
     * [language].
     */
    @JvmStatic public fun forLanguage(language: GuestLanguage, context: Context): PolyglotValue {
      return forLanguage(language.languageId, context)
    }


    /**
     * Returns the value providing access to the environment map installed in the given [context] for a specific
     * [language].
     */
    @JvmStatic public fun forLanguage(languageId: String, context: Context): PolyglotValue {
      return context.getBindings(languageId).getMember(APP_ENV_BIND_PATH)
        ?: error("Environment binding is not available for language $languageId in this context")
    }
  }
}

/** Configure the [Environment] plugin, installing it if not already present. */
@DelicateElideApi public fun PolyglotEngineConfiguration.environment(configure: EnvConfig.() -> Unit) {
  plugin(Environment)?.config?.apply(configure) ?: configure(Environment, configure)
}

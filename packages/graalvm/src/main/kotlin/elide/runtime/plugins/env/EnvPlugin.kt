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

package elide.runtime.plugins.env

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.core.*
import elide.runtime.core.EngineLifecycleEvent.*
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.gvm.internals.GraalVMGuest
import elide.runtime.gvm.internals.intrinsics.js.struct.map.JsMap
import elide.runtime.plugins.env.Environment.Plugin.APP_ENV_ACCESSOR
import elide.runtime.plugins.env.Environment.Plugin.APP_ENV_SYMBOL
import elide.vm.annotations.Polyglot

/**
 * Engine plugin providing isolated application environment.
 *
 * @see EnvConfig
 */
@DelicateElideApi public class Environment private constructor (public val config: EnvConfig) {
  private val effectiveEnvironment: AtomicReference<MutableMap<String, String?>> = AtomicReference(null)
  private val languageSupport: MutableMap<String, Boolean> = ConcurrentSkipListMap()

  // Maybe inject an environment variable, otherwise run `or`.
  private fun maybeInjectedVar(variable: String, or: () -> Any?) : Any? {
    return when (variable.lowercase().trim()) {
      nodeEnvVariable -> "production"
      else -> or.invoke()
    }
  }

  // Build proxied map for guest language access to env; it should not be mutable.
  private val proxiedEnvMap: ProxyHashMap by lazy {
    val jsMap = JsMap.of(effectiveEnvironment.get() ?: error("Could not resolve effective environment"))
    object: ProxyHashMap, ProxyIterable, ProxyObject {
      @Polyglot override fun getHashSize(): Long = jsMap.size.toLong() + injectedVariableCount
      @Polyglot override fun hasHashEntry(key: Value): Boolean = jsMap.has(key.asString())
      @Polyglot override fun getHashValue(key: Value): Any? = jsMap[key.asString()]
      @Polyglot override fun getHashEntriesIterator(): Any = jsMap.entries()
      @Polyglot override fun putHashEntry(key: Value, value: Value) =
        throw UnsupportedOperationException("Elide forbids writes to `process.env` after VM boot")

      @Polyglot override fun getMember(key: String): Any? = jsMap[key]
      @Polyglot override fun getMemberKeys(): Any = jsMap.keys.toTypedArray()
      @Polyglot override fun hasMember(key: String): Boolean = jsMap.has(key)
      @Polyglot override fun putMember(key: String, value: Value?) =
        throw UnsupportedOperationException("Elide forbids writes to `process.env` after VM boot")

      @Polyglot override fun getIterator(): Any {
        val iterator = jsMap.keys.iterator()
        return object: ProxyIterator {
          override fun hasNext(): Boolean = iterator.hasNext()
          override fun getNext(): Any = iterator.next()
        }
      }
    }
  }

  /** Apply an environment [config] to a context [builder] during the [ContextCreated] event. */
  internal fun onContextCreate(builder: PolyglotContextBuilder) {
    if (config.app.enabled) {
      config.app.isolatedEnvironmentVariables.filterValues {
        it.isPresent
      }.mapValues {
        it.value.value
      }.toMutableMap().let { effective ->
        // apply configured app environment
        effectiveEnvironment.set(effective)
        builder.environment(effective)
      }
    }
  }

  /** Install universal application environment symbols. */
  internal fun installPolyglotBinding(context: PolyglotContext) = effectiveEnvironment.get()?.let { envMap ->
    context.bindings().putMember(APP_ENV_ACCESSOR, ProxyExecutable {
      proxiedEnvMap
    })
  }

  /** Install JS-specific application environment symbols. */
  internal fun installJsAppEnvBinding(context: PolyglotContext) = effectiveEnvironment.get()?.let { envMap ->
    context.bindings(object: GuestLanguage {
      override val languageId: String get() = GraalVMGuest.JAVASCRIPT.engine
    }).putMember(
      APP_ENV_SYMBOL,
      proxiedEnvMap,
    )
  }

  /** Install Ruby-specific application environment symbols. */
  internal fun installRubyAppEnvAccessors(context: PolyglotContext) = effectiveEnvironment.get()?.let { envMap ->
    // nothing at this time
  }

  /** Check [language] support and run [do] if enabled. */
  private fun ifLangSupported(language: String, `do`: () -> Unit) {
    if (languageSupport[language] == true) {
      `do`.invoke()
    }
  }

  /** Install language-specific bindings for injected application environment. */
  internal fun onContextInitialize(context: PolyglotContext) {
    installPolyglotBinding(context)
    ifLangSupported(GraalVMGuest.JAVASCRIPT.engine) { installJsAppEnvBinding(context) }
    ifLangSupported(GraalVMGuest.RUBY.engine) { installRubyAppEnvAccessors(context) }
  }

  /** Identifier for the [Environment] plugin, which provides isolated application environment. */
  public companion object Plugin : EnginePlugin<EnvConfig, Environment> {
    private const val injectedVariableCount = 1L
    private const val nodeEnvVariable = "NODE_ENV"
    private const val APP_ENV_SYMBOL = "__Elide_app_env__"
    private const val APP_ENV_ACCESSOR = "elide_app_environment"
    override val key: Key<Environment> = Key("Environment")

    override fun install(scope: InstallationScope, configuration: EnvConfig.() -> Unit): Environment {
      // apply the configuration and create the plugin instance
      val config = EnvConfig().apply(configuration)
      val instance = Environment(config)

      // @TODO: graceful language detection
      val engine = org.graalvm.polyglot.Engine.create()
      listOf(
        GraalVMGuest.JAVASCRIPT.engine,
        GraalVMGuest.PYTHON.engine,
        GraalVMGuest.RUBY.engine,
        GraalVMGuest.JVM.engine,
        GraalVMGuest.WASM.engine,
      ).forEach {
        instance.languageSupport[it] = engine.languages.containsKey(it)
      }

      // subscribe to lifecycle events
      scope.lifecycle.on(ContextCreated, instance::onContextCreate)
      scope.lifecycle.on(ContextInitialized, instance::onContextInitialize)

      return instance
    }
  }
}

/** Configure the [Environment] plugin, installing it if not already present. */
@DelicateElideApi public fun PolyglotEngineConfiguration.environment(configure: EnvConfig.() -> Unit) {
  plugin(Environment)?.config?.apply(configure) ?: install(Environment, configure)
}

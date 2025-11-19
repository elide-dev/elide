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
package elide.runtime.plugins.jvm

import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EngineLifecycleEvent.ContextInitialized
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.core.extensions.disableOption
import elide.runtime.core.extensions.disableOptions
import elide.runtime.core.extensions.enableOption
import elide.runtime.core.extensions.enableOptions
import elide.runtime.core.extensions.setOption
import elide.runtime.plugins.AbstractLanguagePlugin

// Whether patched Espresso is in use.
private const val USE_PATCHED_ESPRESSO = false

/**
 * Language plugin providing support for JVM bytecode.
 *
 * Unlike for other languages, such as JavaScript, execution using [PolyglotContext.evaluate] is not available for
 * the JVM.
 *
 * When a context with JVM support is created, it will be able to load classes from the classpath defined in this
 * plugin's [configuration][config]. A standard JVM entry point can then be accessed using the [runJvm] extension.
 *
 * @see JvmConfig
 * @see PolyglotContext.runJvm
 */
@DelicateElideApi public class Jvm private constructor(public val config: JvmConfig) {
  private fun initializeContext(context: PolyglotContext) {
    // apply init-time settings
    config.applyTo(context)
  }

  private fun configureContext(builder: PolyglotContextBuilder) {
    builder.allowCreateThread(true)
    builder.allowNativeAccess(true)
    builder.enableOptions(
      "java.BytecodeLevelInlining",
      "java.CHA",
      "java.InlineMethodHandle",
      "java.Polyglot",
      "java.BuiltInPolyglotCollections",
      "java.SoftExit",
      "java.SplitMethodHandles",
    )
    if (System.getProperty("os.name")?.lowercase()?.contains("mac") != true) {
      // don't activate native backend options on macOS, since these are linux-specific for now
      builder.option(
        "java.NativeBackend",
        config.nativeBackend,
      )
    }
    if (config.maxTotalNativeBufferSize != null) {
      builder.option(
        "java.MaxDirectMemorySize",
        config.maxTotalNativeBufferSize.toString(),
      )
    }
    if (config.enableHotSwap) {
      builder.enableOption(
        "java.HotSwapAPI",
      )
    }
    if (config.enableSourceIntegration && USE_PATCHED_ESPRESSO) {
      builder.option(
        "java.HostSourceLoader",
        "elide.runtime.gvm.jvm.JvmSourceLoader",
      )
    }
    if (config.enableNative) {
      builder.option(
        "java.EnableNativeAccess",
        (config.nativeModules.ifEmpty { null } ?: listOf("ALL-UNNAMED")).joinToString(","),
      )
    }
    if (config.enableTruffleRegex) {
      builder.enableOption(
        "java.UseTRegex"
      )
    } else {
      builder.disableOption(
        "java.UseTRegex"
      )
    }
    if (config.enableManagement) {
      builder.enableOption(
        "java.EnableManagement"
      )
    } else {
      builder.disableOption(
        "java.EnableManagement"
      )
    }

    builder.disableOptions(
      "java.EnableNativeAgents",
      "java.EnableManagement",
      "java.ExposeNativeJavaVM",
    )

    // guest classpath
    val classpath = config.collectClasspath()
    builder.option("java.Classpath", classpath)
    logging.debug { "Using guest classpath: $classpath" }

    // threading
    builder.setOption("java.MultiThreaded", config.multithreading)
  }

  public companion object Plugin : AbstractLanguagePlugin<JvmConfig, Jvm>() {
    private const val JVM_LANGUAGE_ID = "java"
    private const val JVM_MANIFEST_KEY = "jvm"
    private const val JVM_PLUGIN_ID = "JVM"
    private val logging by lazy { Logging.of(Jvm::class) }
    override val manifestKey: String = JVM_MANIFEST_KEY
    override val languageId: String = JVM_LANGUAGE_ID
    override val key: Key<Jvm> = Key(JVM_PLUGIN_ID)

    override fun install(scope: InstallationScope, configuration: JvmConfig.() -> Unit): Jvm {
      configureLanguageSupport(scope)

      // apply the configuration and create the plugin instance
      val config = JvmConfig().apply(configuration)
      configureSharedBindings(scope, config)

      val resources = resolveEmbeddedManifest(scope)
      val instance = Jvm(config)

      // subscribe to lifecycle events
      scope.lifecycle.on(ContextCreated, instance::configureContext)
      scope.lifecycle.on(ContextInitialized, instance::initializeContext)

      // register resources with the VFS
      installEmbeddedBundles(scope, resources)

      return instance
    }
  }
}

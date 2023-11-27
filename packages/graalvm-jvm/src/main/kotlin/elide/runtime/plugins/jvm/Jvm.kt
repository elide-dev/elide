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

package elide.runtime.plugins.jvm

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EngineLifecycleEvent.ContextInitialized
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.core.extensions.disableOptions
import elide.runtime.core.extensions.enableOptions
import elide.runtime.core.extensions.setOption
import elide.runtime.plugins.AbstractLanguagePlugin

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
    builder.enableOptions(
      "java.BytecodeLevelInlining",
      "java.CHA",
      "java.HotSwapAPI",
      "java.InlineMethodHandle",
      "java.Polyglot",
      "java.SoftExit",
      "java.SplitMethodHandles",
    )

    builder.disableOptions(
      "java.EnableAgents",
      "java.EnableManagement",
      "java.ExposeNativeJavaVM",
    )

    // guest classpath
    builder.option("java.Classpath", config.collectClasspath())

    // threading
    builder.setOption("java.MultiThreaded", config.multithreading)
  }

  public companion object Plugin : AbstractLanguagePlugin<JvmConfig, Jvm>() {
    private const val JVM_MANIFEST_KEY = "jvm"
    private const val JVM_LANGUAGE_ID = "java"
    private const val JVM_PLUGIN_ID = "JVM"

    override val manifestKey: String = JVM_MANIFEST_KEY
    override val languageId: String = JVM_LANGUAGE_ID
    override val key: Key<Jvm> = Key(JVM_PLUGIN_ID)

    override fun install(scope: InstallationScope, configuration: JvmConfig.() -> Unit): Jvm {
      configureLanguageSupport(scope)

      // apply the configuration and create the plugin instance
      val config = JvmConfig().apply(configuration)
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

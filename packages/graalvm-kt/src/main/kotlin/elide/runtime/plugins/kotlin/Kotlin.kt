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

package elide.runtime.plugins.kotlin

import kotlin.io.path.*
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.ContextInitialized
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.GuestLanguageEvaluator
import elide.runtime.core.PolyglotContext
import elide.runtime.core.getOrInstall
import elide.runtime.plugins.AbstractLanguagePlugin
import elide.runtime.plugins.jvm.Jvm
import elide.runtime.plugins.kotlin.shell.GuestKotlinEvaluator

/**
 * Runtime plugin adding support for evaluating Kotlin in an interactive shell. Applying this plugin will automatically
 * install the [Jvm] plugin.
 *
 * This plugin adds custom guest classpath entries using the [Jvm] plugin configuration which are necessary for the
 * [GuestKotlinEvaluator] to function, such as the Kotlin standard library, the scripting runtime, and a custom
 * JAR providing helper classes used to interface with the guest context from the host.
 *
 * @see [GuestKotlinEvaluator]
 */
@DelicateElideApi public class Kotlin private constructor(public val config: KotlinConfig) {
  /** Configure a new [context] and attach a [GuestKotlinEvaluator] to allow running snippets. */
  private fun initializeContext(context: PolyglotContext) {
    // create an evaluator and bind it to the context
    context[GuestLanguageEvaluator.contextElementFor(Kotlin)] = GuestKotlinEvaluator(context)
  }

  public companion object Plugin : AbstractLanguagePlugin<KotlinConfig, Kotlin>() {
    private const val GUEST_CLASSPATH_KEY = "classpath"

    private const val KOTLIN_LANGUAGE_ID = "kt"
    private const val KOTLIN_PLUGIN_ID = "Kotlin"

    override val languageId: String = KOTLIN_LANGUAGE_ID
    override val key: Key<Kotlin> = Key(KOTLIN_PLUGIN_ID)

    /**
     * Resolve all guest classpath entries from application resources, extracting them as needed into the root specified
     * in the plugin [config]. If an entry is already present in the root it will be reused.
     *
     * @return The list of classpath entries after extraction.
     */
    private fun resolveOrExtractGuestClasspath(config: KotlinConfig, manifest: LanguagePluginManifest): List<String> {
      return runCatching {
        val entries = manifest.resources[GUEST_CLASSPATH_KEY] ?: return emptyList()
        val root = Path(config.guestClasspathRoot)

        entries.map { entry ->
          val output = root.resolve(entry)

          // reuse entry if already extracted
          if (output.exists()) return@map output.absolutePathString()
          output.createParentDirectories()
          output.createFile()

          val resource = Kotlin::class.java.getResource("${manifest.root}/$entry")
            ?: error("Resource missing for classpath entry $entry")

          // extract the JAR from the resources
          resource.openStream().use { source ->
            output.outputStream().use { dest ->
              source.transferTo(dest)
            }
          }

          output.absolutePathString()
        }
      }.getOrElse { cause ->
        throw IllegalStateException("Failed to prepare guest Kotlin classpath", cause)
      }
    }

    override fun install(scope: InstallationScope, configuration: KotlinConfig.() -> Unit): Kotlin {
      val config = KotlinConfig().apply(configuration)
      val resources = resolveEmbeddedManifest(scope)

      // apply the JVM plugin and register the custom classpath entries
      scope.configuration.getOrInstall(Jvm).config.apply {
        classpath(resolveOrExtractGuestClasspath(config, resources))
      }

      // apply the configuration and register events
      val instance = Kotlin(config)
      scope.lifecycle.on(ContextInitialized, instance::initializeContext)

      // register resources with the VFS
      installEmbeddedBundles(scope, resources)

      return instance
    }
  }
}

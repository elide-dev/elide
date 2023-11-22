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

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.getOrInstall
import elide.runtime.plugins.AbstractLanguagePlugin
import elide.runtime.plugins.jvm.Jvm
import elide.runtime.plugins.kotlin.shell.GuestKotlinInterpreter

/**
 * Runtime plugin adding support for evaluating Kotlin in an interactive shell. Applying this plugin will automatically
 * install the [Jvm] plugin.
 *
 * This plugin adds custom guest classpath entries using the [Jvm] plugin configuration which are necessary for the
 * [GuestKotlinInterpreter] to function, such as the Kotlin standard library, the scripting runtime, and a custom
 * JAR providing helper classes used to interface with the guest context from the host.
 *
 * @see [GuestKotlinInterpreter]
 */
@DelicateElideApi public class Kotlin private constructor(public val config: KotlinConfig) {
  public companion object Plugin : AbstractLanguagePlugin<KotlinConfig, Kotlin>() {
    private const val GUEST_CLASSPATH_KEY = "classpath"

    private const val KOTLIN_LANGUAGE_ID = "kt"
    private const val KOTLIN_PLUGIN_ID = "Kotlin"

    override val languageId: String = KOTLIN_LANGUAGE_ID
    override val key: Key<Kotlin> = Key(KOTLIN_PLUGIN_ID)

    private fun resolveGuestClasspathEntries(manifest: LanguagePluginManifest): List<String> {
      return manifest.resources[GUEST_CLASSPATH_KEY]?.map {
        Kotlin::class.java.getResource("${manifest.root}/$it")?.toString() ?: error(
          "Failed to resolve embedded classpath element: $it",
        )
      } ?: emptyList()
    }

    override fun install(scope: InstallationScope, configuration: KotlinConfig.() -> Unit): Kotlin {
      val resources = resolveEmbeddedManifest(scope)

      // apply the JVM plugin first, and register the custom classpath entries
      scope.configuration.getOrInstall(Jvm).config.apply {
        classpath(resolveGuestClasspathEntries(resources))
      }

      // apply the configuration and create the plugin instance
      val config = KotlinConfig().apply(configuration)
      val instance = Kotlin(config)

      // register resources with the VFS
      installEmbeddedBundles(scope, resources)

      return instance
    }
  }
}

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

package elide.runtime.plugins.java

import org.graalvm.polyglot.Source
import elide.runtime.core.*
import elide.runtime.core.EngineLifecycleEvent.ContextInitialized
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.plugins.AbstractLanguagePlugin
import elide.runtime.plugins.java.shell.GuestJavaEvaluator
import elide.runtime.plugins.jvm.Jvm

/**
 * Runtime plugin adding support for evaluating Java in an interactive shell. Applying this plugin will automatically
 * install the [Jvm] plugin.
 *
 * Note that currently the Java shell feature is available even without applying the [Java] plugin, it only requires
 * the [Jvm] plugin to be installed, and a [GuestJavaEvaluator] instance.
 *
 * In the future however, the plugin will automatically create an interpreter instance and associate it with the
 * context for use with extensions, reducing the overhead of starting the shell.
 *
 * @see [GuestJavaEvaluator]
 */
@Suppress("unused") @DelicateElideApi public class Java private constructor(private val config: JavaConfig) {
  /** Configure a new [context] and attach a [GuestJavaEvaluator] to allow running snippets. */
  private fun initializeContext(context: PolyglotContext) {
    // create an evaluator and bind it to the context
    context[GuestLanguageEvaluator.contextElementFor(Java)] = GuestJavaEvaluator(context)
  }

  public companion object Plugin : AbstractLanguagePlugin<JavaConfig, Java>() {
    private const val JAVA_PLUGIN_ID = "Java"
    private const val JAVA_LANGUAGE_ID = "java"

    override val key: Key<Java> = Key(JAVA_PLUGIN_ID)
    override val languageId: String = JAVA_LANGUAGE_ID

    override fun install(scope: InstallationScope, configuration: JavaConfig.() -> Unit): Java {
      // apply the JVM plugin first
      scope.configuration.getOrInstall(Jvm)

      // apply the configuration and create the plugin instance
      val config = JavaConfig().apply(configuration)
      val instance = Java(config)

      scope.lifecycle.on(ContextInitialized, instance::initializeContext)

      return instance
    }
  }
}

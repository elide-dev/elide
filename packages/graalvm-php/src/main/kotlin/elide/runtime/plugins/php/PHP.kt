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
package elide.runtime.plugins.php

import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EnginePlugin
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.plugins.AbstractLanguagePlugin

private const val PHP_PLUGIN_ID: String = "PHP"
private const val PHP_LANGUAGE_ID: String = "php"

/**
 * TBD.
 */
public class PHP {
  @JvmRecord public data class PhpConfig(public val enabled: Boolean = true)

  @Suppress("unused", "unused_parameter")
  private fun configureContext(builder: PolyglotContextBuilder) {
    // nothing at this time
  }

  public companion object Plugin : AbstractLanguagePlugin<PhpConfig, PHP>() {
    override val languageId: String = PHP_LANGUAGE_ID
    override val key: Key<PHP> = Key(PHP_PLUGIN_ID)

    override fun install(scope: EnginePlugin.InstallationScope, configuration: PhpConfig.() -> Unit): PHP {
      configureLanguageSupport(scope)

      return PhpConfig().apply(configuration).let { config ->
        PHP().apply {
          scope.lifecycle.on(ContextCreated, this::configureContext)
        }
      }
    }
  }
}

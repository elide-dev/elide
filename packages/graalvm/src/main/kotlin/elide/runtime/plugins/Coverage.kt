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
package elide.runtime.plugins

import kotlin.io.path.absolutePathString
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent
import elide.runtime.core.EnginePlugin
import elide.runtime.core.PolyglotEngineBuilder
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.core.extensions.enableOption
import elide.runtime.core.plugin

// Default coverage format to use.
private const val DEFAULT_COVERAGE_FORMAT = "json"

/**
 * Engine plugin providing coverage instrumentation and reporting.
 *
 * @see coverage accessor for installing the coverage plugin
 * @see CoverageConfig coverage configuration options
 */
@DelicateElideApi public class Coverage private constructor(
  public val config: CoverageConfig,
) {
  /** Apply debug [config] to a context [builder] during the [EngineLifecycleEvent.EngineCreated] event. */
  internal fun onEngineCreated(builder: PolyglotEngineBuilder) = builder.apply {
    if (!config.enabled) {
      return@apply
    }
    enableOption("coverage")
    enableOption("coverage.Count")

    // if file outputs are requested, we always generate a parseable type, as we can convert that into other formats.
    when (val out = config.outputDirectory) {
      // nothing to do (no output on-disk specified)
      null -> {}
      else -> {
        option("coverage.Output", DEFAULT_COVERAGE_FORMAT)
        option("coverage.OutputFile", out.resolve("coverage.$DEFAULT_COVERAGE_FORMAT").absolutePathString())
      }
    }
  }

  /** Identifier for the [Coverage] plugin. */
  public companion object Plugin : EnginePlugin<CoverageConfig, Coverage> {
    override val key: EnginePlugin.Key<Coverage> get() = EnginePlugin.Key("Coverage")

    override fun install(scope: EnginePlugin.InstallationScope, configuration: CoverageConfig.() -> Unit): Coverage {
      // apply the configuration and create the plugin instance
      val config = CoverageConfig().apply(configuration)
      return Coverage(config).also { instance ->
        scope.lifecycle.on(EngineLifecycleEvent.EngineCreated, instance::onEngineCreated)
      }
    }
  }
}

/** Configure the [Coverage] plugin, installing it if not already present. */
@DelicateElideApi public fun PolyglotEngineConfiguration.coverage(configure: CoverageConfig.() -> Unit) {
  plugin(Coverage)?.config?.apply(configure) ?: configure(Coverage, configure)
}

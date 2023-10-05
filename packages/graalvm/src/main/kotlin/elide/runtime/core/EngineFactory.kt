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

import elide.runtime.core.internals.MutableEngineLifecycle
import elide.runtime.core.internals.graalvm.GraalVMConfiguration
import elide.runtime.core.internals.graalvm.GraalVMEngine

/**
 * Create and [configure] a [PolyglotEngine][elide.runtime.core.PolyglotEngine]. Within the configuration scope,
 * you can install plugins and adjust general engine configuration that will be applied to every context.
 *
 * ### Using the DSL
 *
 * The [install][PluginRegistry.install] function allows plugins to be installed into the
 * engine configuration:
 *
 * ```kotlin
 * val engine = PolyglotEngine {
 *  // apply a language plugin
 *  install(JavaScript) {
 *    // configure the plugin
 *    esm = true
 *  }
 * }
 * ```
 *
 * Plugins can interact with each other within the configuration scope, which can be used to establish dependencies
 * between them: for example, the JavaScript plugin may depend on the VFS plugin to load core intrinsics from a bundle.
 */
@DelicateElideApi public fun PolyglotEngine(configure: PolyglotEngineConfiguration.() -> Unit = { }): PolyglotEngine {
  val lifecycle = MutableEngineLifecycle()
  val configuration = GraalVMConfiguration(lifecycle).apply(configure)
  return GraalVMEngine.create(configuration, lifecycle)
}

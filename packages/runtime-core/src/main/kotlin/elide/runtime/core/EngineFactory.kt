package elide.runtime.core

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
  return GraalVMEngine.create(GraalVMConfiguration().apply(configure))
}

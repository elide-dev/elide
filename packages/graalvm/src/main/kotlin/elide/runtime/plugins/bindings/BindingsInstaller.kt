package elide.runtime.plugins.bindings

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EnginePlugin.InstallationScope

/**
 * A simple contract for components that install intrinsic bindings for one or more languages. Installers are called
 * during engine configuration and may register their mappings as well as access the engine configuration.
 *
 * Installers should not make any assumptions about the target language or the plugin providing support for it. Support
 * for the shared bindings plugin is opt-in and certain language implementations may choose not to apply it.
 */
@DelicateElideApi public fun interface BindingsInstaller {
  /** Install intrinsic bindings using a [registrar] in the given [scope]. */
  public fun install(registrar: BindingsRegistrar, scope: InstallationScope)
}

package elide.runtime.plugins.vfs

import org.graalvm.polyglot.io.FileSystem
import org.graalvm.polyglot.io.IOAccess
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EngineLifecycleEvent.EngineCreated
import elide.runtime.core.EnginePlugin
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.core.PolyglotEngineBuilder

/**
 * Engine plugin providing configurable VFS support for polyglot contexts. Both embedded and host VFS implementations
 * are supported and can be toggled using the [VfsConfig] DSL:
 *
 * ```kotlin
 * val engine = PolyglotEngine {
 *   install(Vfs) {
 *     // this is the default, if set to true, registered bundles are ignored
 *     useHost = false
 *     writable = true
 *
 *     // add bundles from resources
 *     include(MyApp::class.java.getResource("myBundle.tar.gz"))
 *   }
 * }
 * ```
 */
@DelicateElideApi public class Vfs private constructor(public val config: VfsConfig) {
  /** Pre-configured VFS, created when the engine is initialized. */
  private lateinit var fileSystem: FileSystem

  internal fun onEngineCreated(@Suppress("unused_parameter") builder: PolyglotEngineBuilder) {
    // select the VFS implementation depending on the configuration
    // TODO(@darvld)
    /*fileSystem = when(config.useHost) {
      true -> HostVFSImpl.Builder.newBuilder()
        .setReadOnly(!config.writable)
        .build()

      false -> EmbeddedGuestVFSImpl.Builder.newBuilder()
        .setBundlePaths(config.registeredBundles)
        .setReadOnly(!config.writable)
        .build()
    }*/
  }

  /** Configure a context builder to use a custom [fileSystem]. */
  internal fun configureContext(builder: PolyglotContextBuilder) {
    // use the configured VFS for each context
    builder.allowIO(IOAccess.newBuilder().fileSystem(fileSystem).build())
  }
  
  /** Identifier for the [Vfs] plugin, which configures contexts with a custom file system. */
  public companion object : EnginePlugin<VfsConfig, Vfs> {
    override val key: Key<Vfs> = Key("GuestVFS")

    override fun install(scope: InstallationScope, configuration: VfsConfig.() -> Unit): Vfs {
      // apply the configuration and create the plugin instance
      val config = VfsConfig().apply(configuration)
      val instance = Vfs(config)
      
      // subscribe to lifecycle events
      scope.lifecycle.on(EngineCreated, instance::onEngineCreated)
      scope.lifecycle.on(ContextCreated, instance::configureContext)
      
      return instance
    }
  }
}

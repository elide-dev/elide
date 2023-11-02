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

package elide.runtime.plugins.vfs

import org.graalvm.polyglot.io.FileSystem
import org.graalvm.polyglot.io.IOAccess
import java.net.URI
import elide.runtime.Logging
import elide.runtime.core.*
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EngineLifecycleEvent.EngineCreated
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl
import elide.runtime.gvm.internals.vfs.HybridVfs

/**
 * Engine plugin providing configurable VFS support for polyglot contexts. Both embedded and host VFS implementations
 * are supported and can be toggled using the [VfsConfig] DSL:
 *
 * ```kotlin
 * val engine = PolyglotEngine {
 *   install(Vfs) {
 *     // this is the default, if set to true, registered bundles are ignored
 *     writable = true
 *
 *     // add bundles from resources
 *     include(MyApp::class.java.getResource("myBundle.tar.gz"))
 *   }
 * }
 * ```
 */
@DelicateElideApi public class Vfs private constructor(public val config: VfsConfig) {
  /** Plugin logger instance. */
  private val logging by lazy { Logging.of(Vfs::class) }

  /** Pre-configured VFS, created when the engine is initialized. */
  private lateinit var fileSystem: FileSystem

  internal fun onEngineCreated(@Suppress("unused_parameter") builder: PolyglotEngineBuilder) {
    // select the VFS implementation depending on the configuration
    // if no host access is requested, use an embedded in-memory vfs
    fileSystem = if (!config.useHost) {
      logging.debug("No host access requested, using in-memory vfs")
      acquireEmbeddedVfs(config.writable, config.registeredBundles)
    } else {
      // if the configuration requires host access, we use a hybrid vfs
      logging.debug("Host access requested, using hybrid vfs")
      HybridVfs.acquire(config.writable, config.registeredBundles)
    }
  }

  /** Configure a context builder to use a custom [fileSystem]. */
  internal fun configureContext(builder: PolyglotContextBuilder) {
    // use the configured VFS for each context
    builder.allowIO(IOAccess.newBuilder().fileSystem(fileSystem).build())
  }

  /** Identifier for the [Vfs] plugin, which configures contexts with a custom file system. */
  public companion object Plugin : EnginePlugin<VfsConfig, Vfs> {
    override val key: Key<Vfs> = Key("GuestVFS")

    override fun install(scope: InstallationScope, configuration: VfsConfig.() -> Unit): Vfs {
      // apply the configuration and create the plugin instance
      val config = VfsConfig(scope.configuration).apply(configuration)
      val instance = Vfs(config)

      // subscribe to lifecycle events
      scope.lifecycle.on(EngineCreated, instance::onEngineCreated)
      scope.lifecycle.on(ContextCreated, instance::configureContext)

      return instance
    }

    /** Build a new embedded [FileSystem], optionally [writable], using the specified [bundles]. */
    private fun acquireEmbeddedVfs(writable: Boolean, bundles: List<URI>): FileSystem {
      return EmbeddedGuestVFSImpl.Builder.newBuilder()
        .setBundlePaths(bundles)
        .setReadOnly(!writable)
        .build()
    }
  }
}

/** Configure the [Vfs] plugin, installing it if not already present. */
@DelicateElideApi public fun PolyglotEngineConfiguration.vfs(configure: VfsConfig.() -> Unit) {
  plugin(Vfs)?.config?.apply(configure) ?: install(Vfs, configure)
}

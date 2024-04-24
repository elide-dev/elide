/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

import java.net.URI
import java.net.URL
import java.util.concurrent.ConcurrentSkipListSet
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.GuestLanguage
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess.ALLOW_ALL
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess.ALLOW_IO
import elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl

/** Configuration DSL for the [Vfs] plugin. */
@DelicateElideApi public class VfsConfig internal constructor(configuration: PolyglotEngineConfiguration) {
  /** Private mutable list of registered bundles. */
  private val bundles: MutableList<URI> = mutableListOf()

  /** Internal list of bundles registered for use in the VFS. */
  internal val registeredBundles: List<URI> get() = bundles

  /** Whether the file system is writable. If false, write operations will throw an exception. */
  public var writable: Boolean = false

  /** Enable deferred loading of embedded VFS assets, where supported. */
  public var deferred: Boolean = EmbeddedGuestVFSImpl.Settings.DEFAULT_DEFERRED_READS

  /** Whether VFS-incompatible languages are present in the engine. */
  public var languages: MutableSet<GuestLanguage> = ConcurrentSkipListSet()

  /**
   * Whether to use the host's file system instead of an embedded VFS. If true, bundles registered using [include] will
   * not be applied.
   *
   * Enabled by default if the engine's [hostAccess][PolyglotEngineConfiguration.hostAccess] is set to [ALLOW_ALL] or
   * [ALLOW_IO], otherwise false.
   */
  internal var useHost: Boolean = configuration.hostAccess == ALLOW_ALL || configuration.hostAccess == ALLOW_IO

  /** Register a [bundle] to be added to the VFS on creation. */
  public fun include(bundle: URI) {
    bundles.add(bundle)
  }
}

/** Include a bundle by its [url]. This is a shortcut for calling `include(url.toURI())`. */
@DelicateElideApi public fun VfsConfig.include(url: URL) {
  include(url.toURI())
}

/** Include a bundle using a URI string. The string must be a properly formatted URI. */
@DelicateElideApi public fun VfsConfig.include(uriString: String) {
  include(URI.create(uriString))
}

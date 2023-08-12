package elide.runtime.plugins.vfs

import java.net.URI
import java.net.URL
import elide.runtime.core.DelicateElideApi

/** Configuration DSL for the [Vfs] plugin. */
@DelicateElideApi public class VfsConfig internal constructor() {
  /** Private mutable list of registered bundles. */
  private val bundles: MutableList<URI> = mutableListOf()
  
  /** Internal list of bundles registered for use in the VFS. */
  internal val registeredBundles: List<URI> get() = bundles
  
  /** Whether the file system is writable. If false, write operations will throw an exception. */
  public var writable: Boolean = false
  
  /**
   * Whether to use the host's file system instead of an embedded VFS. If true, bundles registered using [include] will
   * not be applied.
   */
  public var useHost: Boolean = false
  
  /** Register a [bundle] to be added to the VFS on creation. */
  public fun include(bundle: URI) {
    bundles.add(bundle)
  }
}

/** Include a bundle by its [url]. This is a shortcut for calling `include(url.toURI())`. */
@DelicateElideApi public fun VfsConfig.include(url: URL) {
  include(url.toURI())
}
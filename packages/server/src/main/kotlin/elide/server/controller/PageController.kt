package elide.server.controller

import elide.server.annotations.Page
import elide.server.assets.AssetManager
import io.micronaut.context.ApplicationContext
import jakarta.inject.Inject

/**
 * Defines the built-in concept of a `Page`-type handler, which is capable of performing SSR, serving static assets, and
 * handling page-level RPC calls.
 *
 * Page controllers use a dual-pronged mechanism to hook into application code. First, the controller annotates with
 * [Page], which provides AOT advice and route bindings; a suite of on-class functions and injections related to page
 * can also then be inherited from [PageController], although this is only necessary to leverage static asset serving
 * and SSR. Most of these resources are acquired statically, which keeps things fast.
 *
 * When the developer calls a method like `ssr` or `asset`, for example, the bean context is consulted, and an
 * `AssetManager` or `JsRuntime` is resolved to satisfy the response.
 *
 * ### Controller lifecycle
 *
 * Bean objects created within a Micronaut dependency injection context have an associated _scope_, which governs
 * something called the "bean lifecycle." The bean lifecycle, and by extension, the bean scope, determines when an
 * instance is constructed, how long it survives, and when garbage is collected.
 *
 * By default, raw Micronaut controllers are API endpoints. For example, the default input/output `Content-Type` is JSON
 * and the lifecycle is set to `Singleton`. This means a controller is initialized the _first time it is accessed_, and
 * then lives for the duration of the server run.
 *
 * Pages follow this default and provide on-class primitives to the user, via [PageController], which help with the
 * management of state, caching, sessions, and so forth.
 */
@Suppress("UnnecessaryAbstractClass")
public abstract class PageController : BaseController() {
  // Asset management runtime.
  @Inject internal lateinit var assetManager: AssetManager

  // Application context.
  @Inject internal lateinit var appContext: ApplicationContext

  /** @return Access to the active asset manager. */
  override fun assets(): AssetManager {
    return assetManager
  }

  /** @return Access to the active application context. */
  override fun context(): ApplicationContext {
    return appContext
  }
}

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

package elide.server.controller

import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import jakarta.inject.Inject
import kotlinx.html.BODY
import kotlinx.html.HEAD
import kotlinx.html.HTML
import kotlinx.html.tagext.body
import kotlinx.html.tagext.head
import elide.server.*
import elide.server.annotations.Page
import elide.server.assets.AssetManager
import elide.server.assets.AssetPointer
import elide.server.assets.AssetReference

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
  override fun assets(): AssetManager = assetManager

  /** @return Access to the active application context. */
  override fun context(): ApplicationContext = appContext

  /** @return Reference to the asset identified by the provided [module] ID. */
  public fun asset(module: AssetModuleId, handler: (AssetReferenceBuilder.() -> Unit)? = null): AssetReference {
    val pointer = assetManager.findAssetByModuleId(module)
    require(pointer != null) {
      "Failed to locate asset at module ID: '$module'"
    }
    val generatedLink = assetManager.linkForAsset(module)
    return if (handler != null) {
      val builder = AssetReferenceBuilder(pointer)
      handler.invoke(builder)
      return builder.build(generatedLink)
    } else {
      AssetReference.fromPointer(pointer, generatedLink)
    }
  }

  /** */
  protected suspend inline fun html(
    request: HttpRequest<*>,
    crossinline renderer: suspend ServerRenderAgent.() -> Unit,
  ): Mono<HttpResponse<Publisher<ByteArray>>> = ssr(request) {
    ServerRenderAgent(this, this@PageController, request).let { agent ->
      renderer.invoke(agent)
    }
  }

  /**
   * TBD.
   */
  public class ServerRenderAgent (
    public val dom: HTML,
    private val controller: PageController,
    private val request: HttpRequest<*>,
  ) {
    public suspend inline fun head(crossinline block: suspend HEAD.() -> Unit) {
      dom.head {
        block.invoke(this)
      }
    }

    public suspend inline fun body(classes : String? = null, crossinline block: suspend BODY.() -> Unit) {
      dom.body(classes) {
        block.invoke(this)
      }
    }

    /**
     * TBD.
     */
    public suspend fun BODY.render(
      domId: String = DEFAULT_SSR_DOM_ID,
      classes: Set<String> = emptySet(),
      attrs: List<Pair<String, String>> = emptyList(),
      path: String? = null,
      embeddedRoot: String? = null,
    ) {
      injectSSR(
        controller,
        request,
        domId,
        classes,
        attrs,
        path,
        embeddedRoot,
      )
    }
  }

  /** Context handler to collect asset configuration. */
  @Suppress("MemberCanBePrivate")
  public inner class AssetReferenceBuilder internal constructor(private val pointer: AssetPointer) {
    /** Set the asset module for this reference. */
    public var module: AssetModuleId = pointer.moduleId

    /** Whether this asset is eligible for inlining. */
    public var inline: Boolean = false

    /** Whether this asset is eligible for preloading. */
    public var preload: Boolean = false

    /** @return Fabricated asset reference. */
    internal fun build(link: String): AssetReference {
      val module = this.module
      require(module.isNotBlank()) {
        "Module ID is required to generate an asset link (was blank)"
      }
      return AssetReference(
        module = module,
        assetType = pointer.type,
        href = link,
        inline = inline,
        preload = preload,
      )
    }
  }
}

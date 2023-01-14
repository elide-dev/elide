@file:JsModule("react-router-dom/server")
@file:JsNonModule
@file:OptIn(ExperimentalJsExport::class)

package react.router.dom.server

import history.PartialLocation
import react.Props
import react.PropsWithChildren

/**
 *
 */
@JsExport
public external interface StaticRouterProps : PropsWithChildren {
  public var basename: String?
  public var location: PartialLocation
}

/**
 *
 */
public external val StaticRouter : react.FC<StaticRouterProps>

/**
 *
 */
@JsExport
public external interface StaticRouterProviderProps : Props {
  public var context: StaticHandlerContext
  public var router: Router
  public var nonce: String?
  public var hydrate: Boolean?
}

/**
 *
 */
@JsName("unstable_StaticRouterProvider")
public external val StaticRouterProvider : react.FC<StaticRouterProviderProps>

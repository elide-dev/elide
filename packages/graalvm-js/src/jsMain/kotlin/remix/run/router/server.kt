package remix.run.router

import history.PartialLocation
import react.Props
import react.PropsWithChildren

/**
 *
 */
public external interface StaticRouterProps : PropsWithChildren {
  public var basename: String?
  public var location: PartialLocation
}

/**
 *
 */
public external val StaticRouter: react.FC<StaticRouterProps>

/**
 *
 */
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
public external val StaticRouterProvider: react.FC<StaticRouterProviderProps>

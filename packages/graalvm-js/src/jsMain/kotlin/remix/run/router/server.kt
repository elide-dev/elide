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
public external val StaticRouter : react.FC<StaticRouterProps>

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
public external val StaticRouterProvider : react.FC<StaticRouterProviderProps>

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

package elide.server.http

import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import jakarta.inject.Inject
import elide.server.assets.AssetManager

/**
 * Provides an [HttpServerFilter] which affixes context values at known keys in [HttpRequest]s processed by Elide apps;
 * known keys are defined via [RequestContext.Key].
 *
 * @see RequestContext.Key for an exhaustive review of available request context.
 */
@Filter("/**") public class RequestContextFilter : HttpServerFilter {
  @Inject internal lateinit var assetManager: AssetManager

  /** @inheritDoc */
  override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
    request.setAttribute(
      RequestContext.Key.ASSET_MANAGER.name,
      assetManager
    )
    return chain.proceed(request)
  }
}

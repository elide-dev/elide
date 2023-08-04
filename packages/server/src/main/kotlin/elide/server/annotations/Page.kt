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

package elide.server.annotations

import io.micronaut.context.annotation.AliasFor
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.DefaultScope
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.UriMapping
import jakarta.inject.Singleton

/**
 * Annotations which is applied to handlers which respond to HTTP requests with HTTP responses, typically encoded in
 * HTML, for the purpose of presenting a user interface.
 *
 * The Micronaut annotations `Controller` behaves in essentially the same manner as `Page`, but with different defaults
 * and available settings.
 *
 * @param route HTTP route that should be bound to this page.
 * @param name Name to use when generating links for this route.
 * @param produces Types produced by this page; defaults to `text/html`.
 * @param consumes Types consumed by this page; defaults to nothing.
 * @param precompile Whether this page handler should be considered for pre-compiled sites. Defaults to `true`.
 */
@Bean
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Controller
@DefaultScope(Singleton::class)
public annotation class Page(
  /** HTTP route that should be bound to this page. */
  @get:AliasFor(annotation = UriMapping::class, member = "value")
  public val route: String = UriMapping.DEFAULT_URI,

  /** Name for this route. */
  public val name: String = "",

  /** Content-Type produced by this endpoint; defaults to HTML. */
  @get:AliasFor(annotation = Produces::class, member = "value")
  public val produces: Array<String> = [MediaType.TEXT_HTML],

  /** Content-Type consumed by this endpoint; defaults to JSON. */
  @get:AliasFor(annotation = Consumes::class, member = "value")
  public val consumes: Array<String> = [],

  /** Enable pre-compilation for this page (i.e. via SSG). */
  public val precompile: Boolean = true,
)

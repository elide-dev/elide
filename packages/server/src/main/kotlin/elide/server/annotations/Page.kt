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
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@Bean
@Controller
@DefaultScope(Singleton::class)
public annotation class Page(
  /** HTTP route that should be bound to this page. */
  @get:AliasFor(annotation = UriMapping::class, member = "value")
  val route: String = UriMapping.DEFAULT_URI,

  /** Content-Type produced by this endpoint; defaults to HTML. */
  @get:AliasFor(annotation = Produces::class, member = "value")
  val produces: Array<String> = [MediaType.TEXT_HTML],

  /** Content-Type consumed by this endpoint; defaults to JSON. */
  @get:AliasFor(annotation = Consumes::class, member = "value")
  val consumes: Array<String> = [MediaType.TEXT_HTML],
)

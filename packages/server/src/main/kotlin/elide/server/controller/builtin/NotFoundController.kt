package elide.server.controller.builtin

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.annotation.Get
import java.io.ByteArrayOutputStream
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.html.tagext.body
import kotlinx.html.tagext.head
import kotlinx.html.title
import elide.server.RawPayload
import elide.server.RawResponse
import elide.server.annotations.Eager
import elide.server.html

/** Default built-in controller which handles `404 Not Found` events. */
@Eager @Controller public class NotFoundController : BuiltinController() {
  /** @inheritDoc */
  @Get("/error/notfound", produces = [
    MediaType.TEXT_HTML,
    MediaType.APPLICATION_JSON,
  ])
  @Error(status = HttpStatus.NOT_FOUND, global = true)
  override suspend fun handle(request: HttpRequest<out Any>): RawResponse {
    val accept = (request.accept() ?: listOf(MediaType.TEXT_HTML)).map { it.toString() }
    return when {
      accept.contains(MediaType.TEXT_HTML) -> serveHTMLNotFound()
      accept.contains(MediaType.APPLICATION_JSON) -> serveJSONNotFound()
      else -> servePlaintext()
    }
  }

  // Serve a 404 in HTML.
  private suspend fun serveHTMLNotFound(): RawResponse = html {
    html {
      head {
        title { +"Not Found" }
      }
      body {
        h1 { +"Not Found" }
        p { +"The requested resource was not found." }
      }
    }
  }

  // Serve a 404 in JSON.
  private fun serveJSONNotFound(): RawResponse {
    return HttpResponse.notFound<RawPayload>().contentType(MediaType.APPLICATION_JSON)
  }

  private fun servePlaintext(): RawResponse {
    val baos = ByteArrayOutputStream()
    baos.use {
      it.writeBytes("Not found.".toByteArray())
    }
    return HttpResponse.notFound<RawPayload>().contentType(MediaType.TEXT_PLAIN).body(
      baos
    )
  }
}

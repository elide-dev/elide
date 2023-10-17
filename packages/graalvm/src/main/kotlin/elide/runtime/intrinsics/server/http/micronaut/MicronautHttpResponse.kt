package elide.runtime.intrinsics.server.http.micronaut

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intrinsics.server.http.HttpRequest
import elide.runtime.intrinsics.server.http.HttpResponse
import io.micronaut.http.HttpResponse as MicronautResponse
import io.micronaut.http.MutableHttpResponse as MicronautMutableResponse

/** [HttpRequest] implementation wrapping a Netty handler context. */
@DelicateElideApi internal class MicronautHttpResponse(
  private val response: MicronautMutableResponse<*>
) : HttpResponse {
  constructor() : this(MicronautResponse.ok<Any?>())

  @Export override fun send(status: Int, body: PolyglotValue?) {
    response.status(status)
    body?.let { response.body(it.toString()) }
  }
  
  /** Returns the wrapped Micronaut response. */
  internal fun unwrap(): MicronautResponse<*> {
    return response
  }
}

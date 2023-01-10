package elide.runtime.gvm.internals.js

import elide.runtime.gvm.MicronautRequestExecutionInputs
import elide.runtime.intrinsics.js.FetchRequest
import io.micronaut.http.HttpRequest
import java.io.InputStream
import java.net.URI

/** Implementation of JS execution inputs (a [FetchRequest]), based on a Micronaut [HttpRequest]. */
internal class JsMicronautRequestExecutionInputs private constructor (private val request: HttpRequest<InputStream>) :
  FetchRequest,
  MicronautRequestExecutionInputs<InputStream>,
  JsServerRequestExecutionInputs<HttpRequest<InputStream>>() {
  /** @inheritDoc */
  override fun getURL(): URI = request.uri

  /** @inheritDoc */
  override fun request(): HttpRequest<InputStream> = request

  /** @inheritDoc */
  override fun hasBody(): Boolean = request.body.isPresent

  /** @inheritDoc */
  override fun requestBody(): InputStream = request.body.get()

  /** @inheritDoc */
  override fun requestHeaders(): Map<String, List<String>> = request.headers.asMap()
}

package elide.runtime.gvm.internals.js

import elide.runtime.gvm.MicronautRequestExecutionInputs
import elide.runtime.intrinsics.js.FetchRequest
import io.micronaut.http.HttpRequest
import java.io.InputStream
import java.net.URI

/** Implementation of JS execution inputs (a [FetchRequest]), based on a Micronaut [HttpRequest]. */
internal class JsMicronautRequestExecutionInputs private constructor (
  private val request: HttpRequest<Any>,
  state: Any?,
) :
  FetchRequest,
  MicronautRequestExecutionInputs<Any>,
  JsServerRequestExecutionInputs<HttpRequest<Any>>(state) {
  /** @inheritDoc */
  override fun getURL(): URI = request.uri

  /** @inheritDoc */
  override fun request(): HttpRequest<Any> = request

  /** @inheritDoc */
  override fun hasBody(): Boolean = request.body.isPresent

  /** @inheritDoc */
  override fun requestBody(): InputStream = request.getBody(InputStream::class.java).get()

  /** @inheritDoc */
  override fun requestHeaders(): Map<String, List<String>> = request.headers.asMap()

  /** Factory for creating inputs of this type. */
  internal companion object {
    /** @return Micronaut-backed JavaScript request execution inputs. */
    @JvmStatic fun of(request: HttpRequest<Any>, state: Any? = null) = JsMicronautRequestExecutionInputs(
      request,
      state,
    )
  }
}

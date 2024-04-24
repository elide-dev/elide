package elide.embedded.internal

import elide.embedded.http.EmbeddedResponse
import elide.embedded.http.MutableEmbeddedHeaders
import elide.vm.annotations.Polyglot

/**
 * A direct implementation of the [EmbeddedResponse] interface, used when a response originates from an internal
 * component and is not meant to cross the native serialization boundary, such as during testing.
 */
internal class ImmediateResponse : EmbeddedResponse {
  @Polyglot override var statusCode: Int = 200
  @Polyglot override var statusMessage: String = "OK"
  @Polyglot override val headers: MutableEmbeddedHeaders = ImmediateHeaders()
}

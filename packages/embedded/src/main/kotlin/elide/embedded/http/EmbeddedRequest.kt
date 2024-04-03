package elide.embedded.http

import elide.vm.annotations.Polyglot

/**
 * Represents an HTTP request passed to the embedded runtime by the host application.
 *
 * Requests may be passed using a serialization protocol such as ProtoBuf or Cap'n'Proto, or directly using structs
 * over a native boundary. The exchange mechanism may influence the behavior of certain methods and properties, but
 * the general contract is kept regardless of the provenance.
 */
public interface EmbeddedRequest {
  /** The request URI string. */
  @get:Polyglot public val uri: String

  /** The HTTP method of the request. */
  @get:Polyglot public val method: String

  /** A multi-map holding values for the request headers. */
  @get:Polyglot public val headers: Map<String, List<String>>
}
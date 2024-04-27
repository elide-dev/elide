package elide.embedded.http

import elide.vm.annotations.Polyglot

/**
 * Represents an HTTP response returned by the embedded runtime to the host application.
 *
 * Responses may be passed using a serialization protocol such as ProtoBuf or Cap'n'Proto, or directly using structs
 * over a native boundary. The exchange mechanism may influence the behavior of certain methods and properties, but
 * the general contract is kept regardless of the provenance.
 */
public interface EmbeddedResponse

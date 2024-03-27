package elide.embedded.http

/**
 * Represents an HTTP response returned by the embedded runtime to the host application.
 *
 * Responses may be passed using a serialization protocol such as ProtoBuf or Cap'n'Proto, or directly using structs
 * over a native boundary. The exchange mechanism may influence the behavior of certain methods and properties, but
 * the general contract is kept regardless of the provenance.
 * 
 * Note that this type is not directly mean to be used in guest execution contexts, language-specific wrappers should
 * be used in that case.
 */
public typealias EmbeddedResponse = Any
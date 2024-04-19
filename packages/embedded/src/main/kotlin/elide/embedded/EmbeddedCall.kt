package elide.embedded

import elide.embedded.http.EmbeddedRequest
import elide.embedded.internal.EmbeddedCallImpl

/** A unique incremental identifier for an [EmbeddedCall]. */
@JvmInline public value class EmbeddedCallId(public val value: String)

/**
 * Represents an incoming call being dispatched through the embedded runtime. Calls provide a unique [id] and a
 * [request] to be processed.
 */
public interface EmbeddedCall {
  /** A unique identifier for this call. */
  public val id: EmbeddedCallId

  /** The [EmbeddedRequest] for this call, to be handled by a guest application. */
  public val request: EmbeddedRequest
}

/** Create a new [EmbeddedCall] with the specified [id] wrapping a [request]. */
public fun EmbeddedCall(id: EmbeddedCallId, request: EmbeddedRequest): EmbeddedCall {
  return EmbeddedCallImpl(id, request)
}
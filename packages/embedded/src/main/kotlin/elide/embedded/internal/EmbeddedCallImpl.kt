package elide.embedded.internal

import elide.embedded.EmbeddedCall
import elide.embedded.EmbeddedCallId
import elide.embedded.http.EmbeddedRequest

/** Default [EmbeddedCall] implementation with an embedded ID and request. */
internal data class EmbeddedCallImpl(
  override val id: EmbeddedCallId,
  override val request: EmbeddedRequest,
) : EmbeddedCall
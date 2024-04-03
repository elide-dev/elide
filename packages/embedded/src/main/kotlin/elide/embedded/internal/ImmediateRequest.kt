package elide.embedded.internal

import elide.embedded.http.EmbeddedRequest
import elide.vm.annotations.Polyglot

/**
 * A direct implementation of the [EmbeddedRequest] interface, used when a request originates from an internal
 * component such as those during testing.
 */
internal data class ImmediateRequest(
  @Polyglot override val uri: String,
  @Polyglot override val method: String,
  @Polyglot override val headers: Map<String, List<String>>
) : EmbeddedRequest
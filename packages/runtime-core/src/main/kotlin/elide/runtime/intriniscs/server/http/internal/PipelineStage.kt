package elide.runtime.intriniscs.server.http.internal

import elide.runtime.core.DelicateElideApi

/**
 * Represents a single stage in a request handler pipeline.
 *
 * For an incoming request, a [stage][PipelineStage] is executed if its [matcher] returns `true`, in which case a
 * [GuestHandler] reference is resolved for invocation using the stage's [key].
 */
@DelicateElideApi internal data class PipelineStage(
  /** The routing key for this stage, used to resolve a [GuestHandler] reference. */
  val key: String,
  /** Matcher function used to test this stage against incoming requests. */
  val matcher: PipelineMatcher,
)
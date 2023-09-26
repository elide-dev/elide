package elide.runtime.intrinsics.server.http.internal

import elide.runtime.core.DelicateElideApi

/**
 * Represents a single stage in a request handler pipeline.
 *
 * For an incoming request, a [stage][PipelineStage] is executed if its [matcher] returns `true`, in which case a
 * [GuestHandler] reference is resolved for invocation from the [stage] index.
 */
@DelicateElideApi internal data class PipelineStage(
  /**
   * The routing key for this stage, and its absolute index in the pipeline, used to resolve a [GuestHandler]
   * reference from a [registry][HandlerRegistry].
   */
  val stage: Int,
  /** Matcher function used to test this stage against incoming requests. */
  val matcher: PipelineMatcher,
)
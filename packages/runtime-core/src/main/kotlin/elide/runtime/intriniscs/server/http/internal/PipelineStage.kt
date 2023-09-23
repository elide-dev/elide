package elide.runtime.intriniscs.server.http.internal

import elide.runtime.core.DelicateElideApi

@DelicateElideApi internal data class PipelineStage(
  val key: String,
  val matcher: PipelineMatcher,
)
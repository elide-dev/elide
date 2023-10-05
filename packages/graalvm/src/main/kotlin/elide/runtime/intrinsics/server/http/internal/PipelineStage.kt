/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

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

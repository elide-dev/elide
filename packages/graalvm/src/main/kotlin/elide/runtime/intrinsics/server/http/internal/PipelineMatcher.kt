package elide.runtime.intrinsics.server.http.internal

import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpRequest
import elide.runtime.intrinsics.server.http.HttpContext

/** Represents the signature of a request matcher function, used by the [PipelineRouter] */
@DelicateElideApi internal typealias PipelineMatcher = (HttpRequest, HttpContext) -> Boolean
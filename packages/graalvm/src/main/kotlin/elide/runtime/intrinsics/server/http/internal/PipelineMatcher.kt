package elide.runtime.intriniscs.server.http.internal

import elide.runtime.core.DelicateElideApi
import elide.runtime.intriniscs.server.http.HttpRequest
import elide.runtime.intriniscs.server.http.HttpContext

/** Represents the signature of a request matcher function, used by the [PipelineRouter] */
@DelicateElideApi internal typealias PipelineMatcher = (HttpRequest, HttpContext) -> Boolean
package elide.runtime.intriniscs.server.http.internal

import elide.runtime.core.DelicateElideApi

/** Represents the signature of a request matcher function, used by the [HttpRouter] */
@DelicateElideApi internal typealias PipelineMatcher = (HttpRequest, HttpRequestContext) -> Boolean
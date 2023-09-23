package elide.runtime.intriniscs.server.http.internal

import elide.runtime.core.DelicateElideApi

@DelicateElideApi internal typealias PipelineMatcher = (HttpRequest, HttpRequestContext) -> Boolean
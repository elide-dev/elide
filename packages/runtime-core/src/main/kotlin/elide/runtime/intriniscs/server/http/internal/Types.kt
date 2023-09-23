package elide.runtime.intriniscs.server.http.internal

import elide.runtime.core.DelicateElideApi

/**
 * Represents the signature of a method used as request handler. The [GuestHandler] wrapper implements this signature
 * by executing a guest value.
 */
@DelicateElideApi internal typealias GuestHandlerFunction = (HttpRequest, HttpResponse, HttpRequestContext) -> Unit

/** Internal alias used for mutable maps containing handler associated to their routes. */
@DelicateElideApi internal typealias HandlerMap = MutableMap<String, GuestHandler>
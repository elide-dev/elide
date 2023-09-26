package elide.runtime.intrinsics.server.http.internal

import elide.runtime.core.DelicateElideApi

/**
 * Represents an asynchronous [Sequence], producing [GuestHandler] references that form a processing pipeline. Note
 * that the index (key) for the stages in the pipeline may not be continuous.
 */
@DelicateElideApi internal typealias ResolvedPipeline = Sequence<GuestHandler>
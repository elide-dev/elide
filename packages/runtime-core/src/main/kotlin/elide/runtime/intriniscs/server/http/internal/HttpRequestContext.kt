package elide.runtime.intriniscs.server.http.internal

import elide.runtime.core.DelicateElideApi

/**
 * A lightweight container for values bound to a specific [HttpRequest]. The [HttpRequestContext] is meant to hold
 * values such as those extracted from path variables.
 */
@DelicateElideApi @JvmInline internal value class HttpRequestContext(
  private val map: MutableMap<String, Any?>
) : MutableMap<String, Any?> by map {
  /** Constructs a new empty [HttpRequestContext]. */
  constructor() : this(mutableMapOf())
}
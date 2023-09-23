package elide.runtime.intriniscs.server.http.internal

import elide.runtime.core.DelicateElideApi

@DelicateElideApi @JvmInline internal value class HttpRequestContext(
  private val map: MutableMap<String, Any?>
) : MutableMap<String, Any?> by map {
  constructor() : this(mutableMapOf())
}
package elide.runtime.intriniscs.server.http

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.core.DelicateElideApi

/**
 * A lightweight container for values bound to a specific [HttpRequest]. The [HttpContext] is meant to hold
 * values such as those extracted from path variables.
 */
@DelicateElideApi public class HttpContext private constructor(
  private val map: MutableMap<String, Any?>
) : MutableMap<String, Any?> by map, ProxyObject {
  /** Constructs a new empty context. */
  internal constructor() : this(mutableMapOf())

  override fun getMember(key: String): Any {
    return map[key] ?: error("no member found with key $key")
  }

  override fun getMemberKeys(): Any {
    return map.keys.toList()
  }

  override fun hasMember(key: String): Boolean {
    return map.containsKey(key)
  }

  override fun putMember(key: String, value: Value?) {
    map[key] = value
  }
}
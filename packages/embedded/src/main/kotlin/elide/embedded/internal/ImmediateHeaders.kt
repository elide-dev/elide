package elide.embedded.internal

import elide.embedded.http.MutableEmbeddedHeaders
import elide.vm.annotations.Polyglot

@JvmInline internal value class ImmediateHeaders(
  private val map: MutableMap<String, MutableSet<String>>
) : MutableEmbeddedHeaders {
  
  public constructor() : this(mutableMapOf())
  
  @Polyglot override fun put(key: String, value: String) {
    map.compute(key) { _, current ->
      (current ?: mutableSetOf()).also { it.add(value) }
    }
  }

  @Polyglot override fun clear(key: String) {
    map.remove(key)
  }

  @Polyglot override fun get(key: String): Set<String>? {
    return map[key]
  }
}
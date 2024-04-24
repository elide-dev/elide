package elide.embedded.http

import elide.vm.annotations.Polyglot

public interface EmbeddedHeaders {
  @Polyglot public fun get(key: String): Set<String>?
}

public interface MutableEmbeddedHeaders : EmbeddedHeaders {
  @Polyglot public fun put(key: String, value: String)
  
  @Polyglot public fun clear(key: String)
}
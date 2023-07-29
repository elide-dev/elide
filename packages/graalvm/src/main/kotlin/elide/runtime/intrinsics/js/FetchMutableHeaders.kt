package elide.runtime.intrinsics.js

import elide.vm.annotations.Polyglot

/**
 * TBD.
 */
public interface FetchMutableHeaders : FetchHeaders {
  /**
   * TBD.
   */
  @Polyglot public fun append(name: String, value: String)

  /**
   * TBD.
   */
  @Polyglot public fun delete(name: String)

  /**
   * TBD.
   */
  @Polyglot public fun set(name: String, value: String)
}

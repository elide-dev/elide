package elide.runtime.intrinsics.js

import elide.annotations.core.Polyglot

/**
 * TBD.
 */
public interface FetchMutableRequest : FetchRequest {
  /**
   * TBD.
   */
  @get:Polyglot @set:Polyglot public override var headers: FetchHeaders

  /**
   * TBD.
   */
  @get:Polyglot @set:Polyglot public override var method: String

  /**
   * TBD.
   */
  @get:Polyglot @set:Polyglot public override var url: String
}

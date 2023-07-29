package elide.runtime.intrinsics.js

import elide.vm.annotations.Polyglot

/**
 * TBD.
 */
public interface FetchMutableResponse : FetchResponse {
  /**
   * TBD.
   */
  @get:Polyglot @set:Polyglot public override var headers: FetchHeaders

  /**
   * TBD.
   */
  @get:Polyglot @set:Polyglot public override var status: Int

  /**
   * TBD.
   */
  @get:Polyglot @set:Polyglot public override var statusText: String

  /**
   * TBD.
   */
  @get:Polyglot @set:Polyglot public override var url: String
}

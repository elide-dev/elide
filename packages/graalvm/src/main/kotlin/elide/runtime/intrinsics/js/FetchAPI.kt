package elide.runtime.intrinsics.js

import elide.annotations.core.Polyglot

/**
 * TBD.
 */
public interface FetchAPI {
  /**
   * TBD.
   */
  @Polyglot public fun fetch(request: FetchRequest): JsPromise<FetchResponse>

  /**
   * TBD.
   */
  @Polyglot public fun fetch(url: String): JsPromise<FetchResponse>

  /**
   * TBD.
   */
  @Polyglot public fun fetch(url: URL): JsPromise<FetchResponse> = fetch(url.toString())
}

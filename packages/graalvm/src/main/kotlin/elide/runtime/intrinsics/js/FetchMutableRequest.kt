package elide.runtime.intrinsics.js

import elide.annotations.core.Polyglot
import elide.runtime.gvm.internals.intrinsics.js.fetch.FetchRequestIntrinsic
import io.micronaut.http.HttpRequest
import java.net.http.HttpRequest as JavaNetHttpRequest

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

  /**
   * TBD.
   */
  public interface RequestFactory<Impl: FetchRequest> {
    /**
     * TBD.
     */
    public fun forRequest(request: JavaNetHttpRequest): Impl

    /**
     * TBD.
     */
    public fun forRequest(request: HttpRequest<*>): Impl
  }

  /**
   * TBD.
   */
  public companion object DefaultFactory : RequestFactory<FetchMutableRequest> {
    /**
     * TBD.
     */
    @JvmStatic override fun forRequest(request: java.net.http.HttpRequest): FetchMutableRequest {
      return FetchRequestIntrinsic.forRequest(request)
    }

    /**
     * TBD.
     */
    @JvmStatic override fun forRequest(request: HttpRequest<*>): FetchMutableRequest {
      return FetchRequestIntrinsic.forRequest(request)
    }
  }
}

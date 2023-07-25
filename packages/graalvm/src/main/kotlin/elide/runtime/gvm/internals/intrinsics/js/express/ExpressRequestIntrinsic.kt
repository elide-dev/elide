package elide.runtime.gvm.internals.intrinsics.js.express

import elide.runtime.intrinsics.js.express.ExpressRequest
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import reactor.netty.http.server.HttpServerRequest

/** An [ExpressRequest] implemented as a wrapper around a Reactor Netty [HttpServerRequest]. */
internal class ExpressRequestIntrinsic(private val request: HttpServerRequest) : ExpressRequest {
  /** A JavaScript object proxy wrapping a [request]'s parameters. */
  @JvmInline private value class ParamsProxy(private val params: Map<String, String>) : ProxyObject {
    constructor(request: HttpServerRequest) : this(request.params() ?: emptyMap())
    
    override fun getMember(key: String): Any? = params[key]
    override fun getMemberKeys(): Any = params.keys
    override fun hasMember(key: String): Boolean = params.containsKey(key)
    override fun putMember(key: String, value: Value?): Unit = error(
      "Modifying the request parameters is not supported"
    )

    override fun removeMember(key: String): Boolean = error(
      "Modifying the request parameters is not supported"
    )
  }
  
  override val params: Value = Value.asValue(ParamsProxy(request))
}
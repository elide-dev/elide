package elide.rpc.server.web

import io.grpc.StatusRuntimeException
import kotlinx.coroutines.Deferred

/**
 * Describes the provided API surface of the internal fulfillment service which is responsible for dispatching gRPC
 * calls on behalf of [GrpcWebController].
 *
 * On server startup, the `GrpcWebConfigurator` observes the creation of the [io.grpc.ServerBuilder] bean. At this time,
 * service definitions/bindings are captured and registered with [elide.server.rpc.RpcRuntime].
 */
internal interface GrpcWebService : GrpcWebTransport {
  /**
   * Given a prepared and unfulfilled gRPC Web [call], and a prepared [interceptor] for a one-shot dispatch, fulfill the
   * call using the backing gRPC server managed by this service; provide a deferred return value which evaluates to the
   * completed call.
   *
   * The invoker is expected to translate [StatusRuntimeException] instances into appropriate gRPC Web errors, with
   * consideration for the enclosed [StatusRuntimeException.status].
   *
   * @param call gRPC Web call which we should fulfill with the backing gRPC server.
   * @param interceptor One-shot client side interceptor for outgoing header and response interception.
   * @return Deferred task which resolves to the provided [call], when completed.
   * @throws StatusRuntimeException if the call experiences an error on the remote server.
   */
  suspend fun fulfillAsync(call: GrpcWebCall, interceptor: GrpcWebClientInterceptor): Deferred<GrpcWebCall>
}

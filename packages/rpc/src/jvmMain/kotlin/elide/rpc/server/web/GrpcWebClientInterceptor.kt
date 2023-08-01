package elide.rpc.server.web

import com.google.common.annotations.VisibleForTesting
import io.grpc.*
import io.grpc.ClientCall.Listener
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.Logger
import elide.runtime.Logging

/**
 * Client-facing interceptor which is responsible for mediating traffic between Elide's gRPC Web integration layer and
 * the backing gRPC server which is fulfilling requests.
 *
 * Outgoing headers to the backing server are affixed by this interceptor, and trailers/headers are captured on the
 * response side of the cycle.
 */
internal class GrpcWebClientInterceptor(
  internal val latch: CountDownLatch,
) : ClientInterceptor {
  // Private logger.
  private val logging: Logger = Logging.of(GrpcWebClientInterceptor::class)

  // Terminal status for this call.
  internal val terminalStatus: AtomicReference<Status> = AtomicReference(Status.INTERNAL)

  // Response headers for this call.
  internal val headers: Metadata = Metadata()

  // Response trailers for this call.
  internal val trailers: Metadata = Metadata()

  /**
   * Affix base headers to the provided [headers], returning a set which combines any call-specific headers and any base
   * headers which are added to all outgoing RPCs.
   *
   * @param headers Call-specific headers to equip with base header values.
   * @return Merged set of headers to use on the outgoing RPC.
   */
  @VisibleForTesting
  internal fun affixHeaders(headers: Metadata): Metadata {
    // affix special header to indicate that this is an internal call
    headers.put(GrpcWeb.Metadata.internalCall, "1")
    return headers
  }

  /**
   * Event handler called when we have received the provided set of [headers] as part of a response from the backing
   * gRPC server.
   *
   * @param headers Metadata, as headers, received as part of a gRPC Web dispatch cycle.
   */
  @VisibleForTesting
  internal fun headersReceived(headers: Metadata) {
    logging.trace {
      "Headers received for gRPC request: $headers"
    }
    this.headers.merge(headers)
  }

  /**
   * Event handler called when we have received a final [status] and set of [trailers] as part of a response from the
   * backing gRPC server.
   *
   * @param status Terminal status for the request/response cycle.
   * @param trailers Set of trailers enclosed on the response.
   */
  @VisibleForTesting
  internal fun trailersReceived(status: Status, trailers: Metadata) {
    logging.debug {
      "Status received for gRPC Web request: '${status.code.name}'"
    }
    logging.trace {
      "Trailers received for gRPC Web Request: $trailers"
    }
    this.terminalStatus.set(status)
    this.trailers.merge(trailers)
  }

  /**
   * Listener which is installed by this interceptor to (1) emit request headers in merged form, and (2) capture headers
   * and trailers from the response.
   */
  inner class MetadataResponseListener<T>(listener: Listener<T>) : SimpleForwardingClientCallListener<T>(listener) {
    /** @inheritDoc */
    override fun onHeaders(headers: Metadata) {
      headersReceived(headers)
      super.onHeaders(headers)
    }

    /** @inheritDoc */
    override fun onClose(status: Status, trailers: Metadata) {
      trailersReceived(status, trailers)
      super.onClose(status, trailers)
    }
  }

  /** @inheritDoc */
  override fun <Req : Any, Resp : Any> interceptCall(
    method: MethodDescriptor<Req, Resp>,
    callOptions: CallOptions,
    next: Channel,
  ): ClientCall<Req, Resp> {
    return object : SimpleForwardingClientCall<Req, Resp>(
      next.newCall(
      method, callOptions,
    ),
    ) {
      /** @inheritDoc */
      override fun start(responseListener: Listener<Resp>, headers: Metadata) {
        super.start(
          MetadataResponseListener(responseListener),
          affixHeaders(headers),
        )
      }
    }
  }
}

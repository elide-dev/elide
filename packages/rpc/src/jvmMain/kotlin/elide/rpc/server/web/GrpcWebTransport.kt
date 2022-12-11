package elide.rpc.server.web

import io.grpc.ManagedChannel

/**
 * Describes the provided API surface for a manager which holds a connection to a backing gRPC server, which is used to
 * fulfill requests on behalf of Elide's gRPC Web integration.
 *
 * This manager is typically used by the [GrpcWebService] to maintain an open connection (or connection pool) to the
 * fulfilling server. During testing, this interface may be overridden to mock the backing server without needing
 * network access.
 */
public interface GrpcWebTransport {
  /**
   * Create or acquire a gRPC channel which the consumer can use to interact with the gRPC server backing this service.
   *
   * No guarantee is provided that the return channel is different across method invocations, to allow for connection
   * pooling and other internal operations.
   *
   * @return Managed channel that should be used to communicate with the server.
   */
  public fun channel(): ManagedChannel
}

/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

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

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

import io.grpc.Metadata.ASCII_STRING_MARSHALLER
import io.grpc.Metadata.Key

/** Provides constant values which are used internally by Elide's gRPC integration layer. */
@Suppress("unused") public object GrpcWeb {
  /** Special header names. */
  public object Headers {
    /** gRPC status header, used on a gRPC-Web response. */
    public const val status: String = "grpc-status"

    /** Error message header, used on an exceptional gRPC-Web response. */
    public const val errorMessage: String = "grpc-error"

    /** Indicates whether a given HTTP request is encoded for use with gRPC-web. */
    public const val sentinel: String = "grpc-web"
  }

  /** Special metadata keys. */
  public object Metadata {
    /** Key which is used to signify an internal gRPC Web call to the backing server. */
    public val internalCall: Key<String> = Key.of(
      "x-grpc-web-internal",
      ASCII_STRING_MARSHALLER,
    )

    /** User-provided `authorization` header. */
    public val authorization: Key<String> = Key.of(
      "authorization",
      ASCII_STRING_MARSHALLER,
    )

    /** User-provided `x-api-key` header. */
    public val apiKey: Key<String> = Key.of(
      "x-api-key",
      ASCII_STRING_MARSHALLER,
    )

    /** Trace header. */
    public val trace: Key<String> = Key.of(
      "x-trace",
      ASCII_STRING_MARSHALLER,
    )
  }

  /** Suffix applied to headers which should be considered with a binary value. */
  public const val BINARY_HEADER_SUFFIX: String = "-bin"

  /** Prefix for gRPC-standard headers. */
  public const val GRPC_HEADER_PREFIX: String = "x-grpc-"
}

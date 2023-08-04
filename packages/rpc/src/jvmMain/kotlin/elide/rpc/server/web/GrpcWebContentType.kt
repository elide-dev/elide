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

import io.micronaut.http.MediaType

/**
 * Describes the content types available for use with gRPC Web, including their corresponding HTTP symbols.
 *
 * @param symbol HTTP `Content-Type` value corresponding to this format.
 */
public enum class GrpcWebContentType (internal val symbol: String) {
  /**
   * Binary dispatch for gRPC-Web, potentially with Protocol Buffers.
   *
   * @see contentType For calculation of a full `Content-Type` header.
   */
  BINARY("application/grpc-web"),

  /**
   * Text (base64-encoded) dispatch for gRPC-Web, potentially with Protocol Buffers.
   *
   * @see contentType For calculation of a full `Content-Type` header.
   */
  TEXT("application/grpc-web-text");

  public companion object {
    // All valid `Content-Type` values for gRPC-Web.
    @JvmStatic internal val allValidContentTypes: Array<String> = listOf(
      BINARY.contentType(false),
      BINARY.contentType(true),
      TEXT.contentType(false),
      TEXT.contentType(true),
    ).toTypedArray()

    // Resolve a `GrpcWebContentType` from the provided HTTP `MediaType`, or throw.
    @JvmStatic internal fun resolve(contentType: MediaType): GrpcWebContentType = when (
      contentType.toString().trim().lowercase()
    ) {
      "application/grpc-web+proto",
      "application/grpc-web" -> BINARY
      "application/grpc-web-text+proto",
      "application/grpc-web-text" -> TEXT
      else -> throw IllegalArgumentException("Cannot resolve invalid `Content-Type` for gRPC-Web: '$contentType'")
    }
  }

  /**
   * Render an HTTP `Content-Type` string for the selected format with consideration made for use of [proto]col buffers.
   *
   * @param proto Whether protocol buffers are in use.
   * @return `Content-Type` string to use.
   */
  public fun contentType(proto: Boolean = true): String {
    return if (proto) {
      "$symbol+proto"
    } else {
      this.symbol
    }
  }

  /**
   * Render a Micronaut [MediaType] for the selected format with consideration made for the use of [proto]col buffers.
   *
   * @param proto Whether protocol buffers are in use.
   * @return Micronaut [MediaType] to use.
   */
  public fun mediaType(proto: Boolean = true): MediaType {
    return MediaType(
      contentType(proto)
    )
  }
}

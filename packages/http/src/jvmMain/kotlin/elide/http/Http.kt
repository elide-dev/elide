/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

package elide.http

import elide.http.request.JavaNetHttpRequest
import elide.http.request.MicronautHttpRequest
import elide.http.request.NettyHttpRequest
import elide.http.response.JavaNetHttpResponse
import elide.http.response.MicronautHttpResponse
import elide.http.response.NettyHttpResponse
import io.micronaut.http.HttpRequest as MicronautRequest
import io.netty.handler.codec.http.HttpRequest as NettyRequest
import java.net.http.HttpRequest as JavaHttpRequest

/**
 * # HTTP Utilities
 *
 * Provides static access to create and manage HTTP requests and responses, using the abstract layer provided by the
 * `elide.http` package; content is typically backed by one of Micronaut, Netty, or core JDK classes. A sealed hierarchy
 * of classes is provided for each core HTTP type, such as the [Method] or [ProtocolVersion]; implementations use inline
 * value classes to wrap the underlying types.
 *
 * HTTP requests and responses originate from each framework, and Elide doesn't always have full control over which set
 * of types is in use. Thus, this package provides a suite of abstracted types that can be used to represent HTTP
 * interactions regardless of the underlying engine; the inline class implementation allows compiling away most of the
 * abstractions represented by the type hierarchy.
 *
 * ## Wrapping Types
 *
 * HTTP request and response types can be wrapped in two ways: via methods provided on this static object, or via scoped
 * extension functions on each platform type. The latter should be preferred when available.
 */
public object Http {
  /**
   * HTTP request options.
   *
   * Describes options which affect how an HTTP request is expressed, but aren't part of the request itself. Used when
   * inflating requests to different types.
   *
   * @property version HTTP protocol version
   * @property host Host name
   * @property port Port number
   * @property scheme Scheme (e.g. `http` or `https`)
   */
  @JvmRecord public data class HttpRequestOptions(
    public val version: ProtocolVersion = ProtocolVersion.HTTP_1_1,
    public val host: String? = null,
    public val port: UShort? = null,
    public val scheme: String? = null,
  )

  /**
   * Create an Elide HTTP request from a JDK HTTP request.
   *
   * @param req JDK HTTP request to wrap
   * @return Elide HTTP request
   */
  @JvmStatic public fun request(req: JavaHttpRequest, options: HttpRequestOptions? = null): Request =
    JavaNetHttpRequest.from(req, options)

  /**
   * Create an Elide HTTP request from a Netty HTTP request.
   *
   * @param req Netty HTTP request to wrap
   * @return Elide HTTP request
   */
  @JvmStatic public fun request(req: NettyRequest, options: HttpRequestOptions? = null): Request =
    NettyHttpRequest.from(req, options)

  /**
   * Create an Elide HTTP request from a Micronaut HTTP request.
   *
   * @param T Type of the request body; defaults to [Any]
   * @param req Micronaut HTTP request to wrap
   * @return Elide HTTP request
   */
  @JvmStatic public fun <T> request(req: MicronautRequest<T>, options: HttpRequestOptions? = null): Request =
    MicronautHttpRequest.from(req, options)

  /**
   * Create an Elide HTTP response from a JDK HTTP response.
   *
   * @param res JDK HTTP response to wrap
   * @return Elide HTTP response
   */
  @JvmStatic public fun <T> response(res: java.net.http.HttpResponse<T>): Response =
    JavaNetHttpResponse(res)

  /**
   * Create an Elide HTTP request from a Netty HTTP response.
   *
   * @param res Netty HTTP response to wrap
   * @return Elide HTTP response
   */
  @JvmStatic public fun response(res: io.netty.handler.codec.http.HttpResponse): Response =
    NettyHttpResponse(res)

  /**
   * Create an Elide HTTP request from a Micronaut HTTP response.
   *
   * @param T Type of the response body; defaults to [Any]
   * @param res Micronaut HTTP response to wrap
   * @return Elide HTTP response
   */
  @JvmStatic public fun <T> response(res: io.micronaut.http.HttpResponse<T>): Response =
    MicronautHttpResponse.of(res)
}

/**
 * Convert a JDK HTTP request into a wrapped universal HTTP request type.
 *
 * @receiver JDK HTTP request to convert
 * @return Universal HTTP request type
 */
public fun JavaHttpRequest.toUniversalHttpRequest(): Request =
  Http.request(this)

/**
 * Convert a Netty HTTP request into a wrapped universal HTTP request type.
 *
 * @receiver Netty HTTP request to convert
 * @return Universal HTTP request type
 */
public fun NettyRequest.toUniversalHttpRequest(): Request =
  Http.request(this)

/**
 * Convert a Micronaut HTTP request into a wrapped universal HTTP request type.
 *
 * @receiver Micronaut HTTP request to convert
 * @return Universal HTTP request type
 */
public fun <T> MicronautRequest<T>.toUniversalHttpRequest(): Request =
  Http.request(this)

/**
 * Convert a JDK HTTP response into a wrapped universal HTTP response type.
 *
 * @receiver JDK HTTP response to convert
 * @return Universal HTTP response type
 */
public fun <T> java.net.http.HttpResponse<T>.toUniversalHttpResponse(): Response =
  Http.response(this)

/**
 * Convert a Netty HTTP response into a wrapped universal HTTP response type.
 *
 * @receiver Netty HTTP response to convert
 * @return Universal HTTP response type
 */
public fun io.netty.handler.codec.http.HttpResponse.toUniversalHttpResponse(): Response =
  Http.response(this)

/**
 * Convert a Micronaut HTTP response into a wrapped universal HTTP response type.
 *
 * @receiver Micronaut HTTP response to convert
 * @return Universal HTTP response type
 */
public fun <T> io.micronaut.http.HttpResponse<T>.toUniversalHttpResponse(): Response =
  Http.response(this)

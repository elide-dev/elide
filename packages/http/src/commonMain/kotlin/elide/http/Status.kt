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

import elide.http.StatusCode.StandardStatusCode

/**
 * ## HTTP Status
 *
 * Describes the concept of an HTTP response status. Statuses describe the terminal outcome of an HTTP request/response
 * cycle; only responses carry status values, and only one status value can be assigned to each response.
 *
 * HTTP status is composed of two primitive values:
 *
 * - The [code]; a numeric value, non-negative and capped at 3 digits, which indicates the status of the response.
 * - The [message]; an optional string value, which may be used to provide a short description of the status.
 *
 * A simple HTTP status pair can be obtained via [StatusPair] / the [of] factory method. Platform-specific classes may
 * choose to provide an implementation of this interface.
 */
public sealed interface Status: HttpToken {
  /**
   * ### Standard HTTP Status
   */
  public sealed interface StandardStatus: Status {
    public val standardCode: StandardStatusCode
    override val code: StatusCode get() = standardCode
    override val message: String? get() = standardCode.reasonPhrase
  }

  /**
   * Standard HTTP Statuses: `200 OK`.
   */
  public data object Ok: StandardStatus {
    override val standardCode: StandardStatusCode get() = StandardStatusCode.Ok
  }

  /**
   * Standard HTTP Statuses: `404 Not Found`.
   */
  public data object NotFound: StandardStatus {
    override val standardCode: StandardStatusCode get() = StandardStatusCode.NotFound
  }

  /**
   * ### HTTP status code.
   *
   * Numeric and non-zero HTTP status code portion of this status. If this status code is standard, it will be a 3-digit
   * integer value between `100` and `599`.
   */
  public val code: StatusCode

  /**
   * ### String HTTP status message, if any.
   *
   * Optional string message to enclose with the status; this is typically a short identifier, like `OK` for the `200`
   * status defined by the HTTP spec. If not specified, this value may be `null`, in which case it is omitted.
   */
  public val message: String?

  /**
   * Extension point for platform implementations of HTTP status.
   */
  public interface PlatformStatus: Status

  override fun asString(): String = when (message) {
    null -> code.asString()
    else -> "${code.asString()} $message"
  }

  /**
   * ## Status Pair
   *
   * Holds a simple [pair] consisting of a [StatusCode] and optional [String] message.
   *
   * @property code HTTP status code.
   * @property message Optional status message.
   */
  @JvmInline public value class StatusPair(private val pair: Pair<StatusCode, String?>): Status {
    override val code: StatusCode get() = pair.first
    override val message: String? get() = pair.second
  }

  /** Factories for producing or obtaining a [Status]. */
  public companion object {
    /**
     * Create a standard status code and message pair.
     *
     * @param code Standard HTTP status code to use.
     * @param message Optional status message to use.
     * @return A new [Status] instance with the given code and message.
     */
    @JvmStatic public fun of(code: StatusCode, message: String? = null): Status = StatusPair(code to message)
  }
}

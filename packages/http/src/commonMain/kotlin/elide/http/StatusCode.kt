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

import elide.core.api.Symbolic
import elide.http.StatusCode.StatusClass.CONTROL
import elide.http.StatusCode.StatusClass.CLIENT_ERROR
import elide.http.StatusCode.StatusClass.INFORMATIONAL
import elide.http.StatusCode.StatusClass.SERVER_ERROR
import elide.http.StatusCode.StatusClass.SUCCESS

// Informational messages.
private const val HTTP_STATUS_CONTINUE_CODE: UShort = 100u
private const val HTTP_STATUS_CONTINUE_MESSAGE: String = "Continue"
private const val HTTP_STATUS_SWITCHING_CODE: UShort = 101u
private const val HTTP_STATUS_SWITCHING_MESSAGE: String = "Switching Protocols"

// Success statuses.
private const val HTTP_STATUS_OK_CODE: UShort = 200u
private const val HTTP_STATUS_OK_MESSAGE: String = "OK"
private const val HTTP_STATUS_CREATED_CODE: UShort = 201u
private const val HTTP_STATUS_CREATED_MESSAGE: String = "Created"
private const val HTTP_STATUS_ACCEPTED_CODE: UShort = 202u
private const val HTTP_STATUS_ACCEPTED_MESSAGE: String = "Accepted"
private const val HTTP_STATUS_NONAUTH_CODE: UShort = 203u
private const val HTTP_STATUS_NONAUTH_MESSAGE: String = "Non-Authoritative Information"
private const val HTTP_STATUS_NOCONTENT_CODE: UShort = 204u
private const val HTTP_STATUS_NOCONTENT_MESSAGE: String = "No Content"
private const val HTTP_STATUS_RESETCONTENT_CODE: UShort = 205u
private const val HTTP_STATUS_RESETCONTENT_MESSAGE: String = "Reset Content"

// Redirection & control statuses.
private const val HTTP_STATUS_MULTIPLECHOICES_CODE: UShort = 300u
private const val HTTP_STATUS_MULTIPLECHOICES_MESSAGE: String = "Multiple Choices"

// Client error statuses.
private const val HTTP_STATUS_BADREQUEST_CODE: UShort = 400u
private const val HTTP_STATUS_BADREQUEST_MESSAGE: String = "Bad Request"
private const val HTTP_STATUS_UNAUTHORIZED_CODE: UShort = 401u
private const val HTTP_STATUS_UNAUTHORIZED_MESSAGE: String = "Unauthorized"
private const val HTTP_STATUS_FORBIDDEN_CODE: UShort = 403u
private const val HTTP_STATUS_FORBIDDEN_MESSAGE: String = "Forbidden"
private const val HTTP_STATUS_NOTFOUND_CODE: UShort = 404u
private const val HTTP_STATUS_NOTFOUND_MESSAGE: String = "Not Found"
private const val HTTP_STATUS_METHODNOTALLOWED_CODE: UShort = 405u
private const val HTTP_STATUS_METHODNOTALLOWED_MESSAGE: String = "Method Not Allowed"
private const val HTTP_STATUS_NOTACCEPTABLE_CODE: UShort = 406u
private const val HTTP_STATUS_NOTACCEPTABLE_MESSAGE: String = "Not Acceptable"
private const val HTTP_STATUS_PROXYAUTH_REQUIRED_CODE: UShort = 407u
private const val HTTP_STATUS_PROXYAUTH_REQUIRED_MESSAGE: String = "Proxy Authentication Required"

// Server error statuses.
private const val HTTP_STATUS_INTERNALSERVER_CODE: UShort = 500u
private const val HTTP_STATUS_INTERNALSERVER_MESSAGE: String = "Internal Server Error"

/**
 * ## HTTP Status Code
 */
public sealed interface StatusCode : HttpToken, Symbolic<HttpStatusCode> {
  /**
   * Numeric representation of this status code.
   */
  override val symbol: HttpStatusCode

  /**
   * ### Status Classes
   *
   * Describes classes of standardized HTTP response codes.
   */
  public enum class StatusClass (
    internal val success: Boolean,
    internal val clientError: Boolean = false,
    internal val serverError: Boolean = false,
  ) {
    INFORMATIONAL(success = true),
    SUCCESS(success = true),
    CONTROL(success = true),
    REDIRECTION(success = true),
    CLIENT_ERROR(success = false, clientError = true),
    SERVER_ERROR(success = false, serverError = true),
  }

  /**
   * ### Standard HTTP Status Codes
   */
  public enum class StandardStatusCode (
    internal val kind: StatusClass,
    override val symbol: HttpStatusCode,
    public val reasonPhrase: String? = null,
    public val allowsBody: Boolean = true,
    public val impliesBody: Boolean = kind == SUCCESS && allowsBody,
  ) : StatusCode {
    /** 100 Continue */
    Continue(INFORMATIONAL, HTTP_STATUS_CONTINUE_CODE, HTTP_STATUS_CONTINUE_MESSAGE),

    /** 101 Switching Protocols */
    SwitchingProtocols(INFORMATIONAL, HTTP_STATUS_SWITCHING_CODE, HTTP_STATUS_SWITCHING_MESSAGE),

    /** 200 OK */
    Ok(SUCCESS, HTTP_STATUS_OK_CODE, HTTP_STATUS_OK_MESSAGE),

    /** 201 Created */
    Created(SUCCESS, HTTP_STATUS_CREATED_CODE, HTTP_STATUS_CREATED_MESSAGE),

    /** 202 Accepted */
    Accepted(SUCCESS, HTTP_STATUS_ACCEPTED_CODE, HTTP_STATUS_ACCEPTED_MESSAGE),

    /** 203 Non-Authoritative Information */
    NonAuthoritativeInformation(SUCCESS, HTTP_STATUS_NONAUTH_CODE, HTTP_STATUS_NONAUTH_MESSAGE),

    /** 204 No Content */
    NoContent(SUCCESS, HTTP_STATUS_NOCONTENT_CODE, HTTP_STATUS_NOCONTENT_MESSAGE, allowsBody = false),

    /** 205 Reset Content */
    ResetContent(SUCCESS, HTTP_STATUS_RESETCONTENT_CODE, HTTP_STATUS_RESETCONTENT_MESSAGE),

    /** 300 Multiple Choices */
    MultipleChoices(CONTROL, HTTP_STATUS_MULTIPLECHOICES_CODE, HTTP_STATUS_MULTIPLECHOICES_MESSAGE),

    /** 400 Bad Request */
    BadRequest(CLIENT_ERROR, HTTP_STATUS_BADREQUEST_CODE, HTTP_STATUS_BADREQUEST_MESSAGE),

    /** 401 Unauthorized */
    Unauthorized(CLIENT_ERROR, HTTP_STATUS_UNAUTHORIZED_CODE, HTTP_STATUS_UNAUTHORIZED_MESSAGE),

    /** 403 Forbidden */
    Forbidden(CLIENT_ERROR, HTTP_STATUS_FORBIDDEN_CODE, HTTP_STATUS_FORBIDDEN_MESSAGE),

    /** 404 Not Found */
    NotFound(CLIENT_ERROR, HTTP_STATUS_NOTFOUND_CODE, HTTP_STATUS_NOTFOUND_MESSAGE),

    /** 405 Method Not Allowed */
    MethodNotAllowed(CLIENT_ERROR, HTTP_STATUS_METHODNOTALLOWED_CODE, HTTP_STATUS_METHODNOTALLOWED_MESSAGE),

    /** 406 Not Acceptable */
    NotAcceptable(CLIENT_ERROR, HTTP_STATUS_NOTACCEPTABLE_CODE, HTTP_STATUS_NOTACCEPTABLE_MESSAGE),

    /** 407 Proxy Authentication Required */
    ProxyAuthenticationRequired(
      CLIENT_ERROR,
      HTTP_STATUS_PROXYAUTH_REQUIRED_CODE,
      HTTP_STATUS_PROXYAUTH_REQUIRED_MESSAGE,
    ),

    /** 500 Internal Server Error */
    InternalServerError(SERVER_ERROR, HTTP_STATUS_INTERNALSERVER_CODE, HTTP_STATUS_INTERNALSERVER_MESSAGE);

    override fun asString(): String = "$symbol $reasonPhrase"

    /** Utilities for resolving standard HTTP status codes. */
    public companion object : Symbolic.SealedResolver<HttpStatusCode, StandardStatusCode> {
      public val all: Sequence<StandardStatusCode> = sequence {
        yieldAll(StandardStatusCode.entries)
      }

      override fun resolve(symbol: HttpStatusCode): StandardStatusCode = when (symbol) {
        HTTP_STATUS_CONTINUE_CODE -> Continue
        HTTP_STATUS_SWITCHING_CODE -> SwitchingProtocols
        HTTP_STATUS_OK_CODE -> Ok
        HTTP_STATUS_CREATED_CODE -> Created
        HTTP_STATUS_ACCEPTED_CODE -> Accepted
        HTTP_STATUS_NONAUTH_CODE -> NonAuthoritativeInformation
        HTTP_STATUS_NOCONTENT_CODE -> NoContent
        HTTP_STATUS_RESETCONTENT_CODE -> ResetContent
        HTTP_STATUS_MULTIPLECHOICES_CODE -> MultipleChoices
        HTTP_STATUS_BADREQUEST_CODE -> BadRequest
        HTTP_STATUS_UNAUTHORIZED_CODE -> Unauthorized
        HTTP_STATUS_FORBIDDEN_CODE -> Forbidden
        HTTP_STATUS_NOTFOUND_CODE -> NotFound
        HTTP_STATUS_METHODNOTALLOWED_CODE -> MethodNotAllowed
        HTTP_STATUS_NOTACCEPTABLE_CODE -> NotAcceptable
        HTTP_STATUS_PROXYAUTH_REQUIRED_CODE -> ProxyAuthenticationRequired
        HTTP_STATUS_INTERNALSERVER_CODE -> InternalServerError
        else -> throw unresolved(symbol)
      }
    }
  }

  /**
   * ### Custom HTTP Status Code
   */
  @JvmInline public value class CustomStatusCode private constructor (
    private val pair: Pair<HttpStatusCode, String?>,
  ) : StatusCode {
    public constructor(symbol: HttpStatusCode, reason: String?) : this(symbol to reason)
    public constructor(code: HttpStatusCode) : this(code, null)

    override val symbol: HttpStatusCode get() = pair.first
    override fun asString(): String = "$symbol" + (pair.second?.let { " $it" } ?: "")
    override fun toString(): String = asString()

    public companion object : Symbolic.SealedResolver<HttpStatusCode, CustomStatusCode> {
      public const val MIN: UShort = 100u
      public const val MAX: UShort = 999u

      override fun resolve(symbol: HttpStatusCode): CustomStatusCode = when (symbol) {
        in MIN..MAX -> CustomStatusCode(symbol to null)
        else -> throw unresolved(symbol)
      }
    }
  }

  /** Factories for resolving a [StatusCode] instance. */
  public companion object {
    /**
     * ### Resolve a [StatusCode] from a [HttpStatusCode].
     *
     * @param symbol Symbolic representation of the status code.
     * @return A resolved [StatusCode] instance.
     */
    @JvmStatic public fun resolve(symbol: HttpStatusCode, reason: String? = null): StatusCode {
      return try {
        StandardStatusCode.resolve(symbol)
      } catch (_: Symbolic.Unresolved) {
        CustomStatusCode(symbol, reason)
      }
    }
  }
}

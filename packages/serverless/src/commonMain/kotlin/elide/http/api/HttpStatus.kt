/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.http.api

import kotlin.jvm.JvmStatic

/**
 * # HTTP: Response Status
 *
 * Describes the concept of an HTTP response status, including a response [code] (numeric), and an optional [text]
 * phrase, description, or reason.
 *
 * ## Code ([code])
 *
 * Describes the numeric status code for the response expressed by this record. Status codes are standardized by the
 * various HTTP specifications.
 *
 * ## Text ([text])
 *
 * Describes the optional text "reason" or description for the response status expressed by this record. This value is
 * optional.
 */
public interface HttpStatus {
  /**
   * Describes the numeric status code for the response expressed by this record. Status codes are standardized by the
   * various HTTP specifications.
   */
  public val code: HttpStatusCode

  /**
   * Describes the optional text "reason" or description for the response status expressed by this record. This value is
   * optional.
   */
  public val text: HttpString?

  /**
   * Describes the inferred "type" of this HTTP status, based on the response [code] enclosed; this value is optional to
   * account for exotic (non-standard) status codes.
   */
  public val type: Type?

  /**
   * ## HTTP Status: Type
   *
   * Describes the broad type of a given HTTP status code, based on the numeric range of the status code. Ranges are
   * defined for each type, along with error states and whether the error is the server's fault.
   */
  public enum class Type (
    public val range: IntRange,
    public val description: String,
    public val err: Boolean = false,
    public val serverFault: Boolean = false,
  ) {
    /**
     * ## Informational Statuses
     *
     * Describes informational HTTP statuses, which are used to indicate state between the client and server, but do not
     * typically carry response data. Terminal states are typically modeled by higher-rank status codes.
     */
    INFORMATIONAL(100..199, "Informational"),

    /**
     * ## Successful Statuses
     *
     * Describes successful HTTP statuses, which are used to indicate that the client's request was successful in a
     * terminal manner. These statuses typically carry response data.
     */
    SUCCESSFUL(200..299, "Successful"),

    /**
     * ## Redirection Statuses
     *
     * Describes redirection HTTP statuses, which are temporary state responses that cause the client to fetch or
     * otherwise resolve the asset from some other source or location.
     */
    REDIRECTION(300..399, "Redirection"),

    /**
     * ## Client Error Statuses
     *
     * Describes client error HTTP statuses, which are used to indicate that the client's request was invalid, aborted,
     * canceled, not allowed, or otherwise caused failure in some terminal manner.
     */
    CLIENT_ERROR(400..499, "Client Error", err = true),

    /**
     * ## Server Error Statuses
     *
     * Describes server error HTTP statuses, which are used to indicate that the server failed to process the client's
     * request in some terminal manner. These statuses typically indicate a server-side fault.
     */
    SERVER_ERROR(500..599, "Server Error", err = true, serverFault = true);

    /** Methods for resolving HTTP status code types. */
    public companion object {
      /**
       * Describes the type of HTTP status code based on the numeric [code] value.
       *
       * @param code The numeric status code to infer the type from.
       * @return The inferred type, if any; if the code is exotic or out of spec, `null` is returned.
       */
      @JvmStatic public fun fromCode(code: HttpStatusCode): Type? = code.toInt().let { codeValue ->
        entries.firstOrNull { codeValue in it.range }
      }
    }
  }
}

/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package elide.http.api

import elide.annotations.API

/**
 * # HTTP Message
 *
 * Represents the top abstract concept of an HTTP message; this includes the concepts of HTTP requests and HTTP
 * responses, with commonalities between them expressed.
 *
 * ## Type ([type])
 *
 * Describes the type of message (request or response). This is set automatically by constructor parameters enforced by
 * base classes.
 *
 * ## Mutability ([mutable])
 *
 * Describes the mutability of the message. This is set automatically by constructor parameters enforced by base
 * classes.
 *
 * ## Version ([version])
 *
 * Describes the HTTP version for the request expressed by this record; implementations should preserve this value from
 * construction time.
 *
 * ## Headers ([headers])
 *
 * Describes HTTP headers for this message.
 *
 * @see MutableHttpMessage for the mutable form of this interface.
 */
@API public interface HttpMessage {
  /**
   * Describes the type of message (request or response).
   */
  public val type: HttpMessageType

  /**
   * Describes the mutability of the message.
   */
  public val mutable: Boolean get() = false

  /**
   * Describes the HTTP version for the request expressed by this record; implementations should preserve this value
   * from construction time.
   */
  public val version: HttpVersion

  /**
   * Describes HTTP headers for this message.
   */
  public val headers: HttpHeaders

  /**
   * Describes HTTP payload info for this message.
   */
  public val body: HttpPayload
}

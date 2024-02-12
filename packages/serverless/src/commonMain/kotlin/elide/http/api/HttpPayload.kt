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

package elide.http.api

import elide.http.HttpBytes

/**
 * # HTTP: Payload
 *
 * Describes a data container for an HTTP message payload; this interface presents useful methods which implementors
 * must provide to describe the underlying data, and to obtain such data in various forms.
 */
public interface HttpPayload {
  /** Indicates whether a payload value is present. */
  public val present: Boolean

  /** Indicates whether this payload object is mutable. */
  public val mutable: Boolean

  /** The size of the payload, in bytes. */
  public val size: ULong

  /** MIME content type of the payload data, if known. */
  public val contentType: Mimetype?

  /** Raw bytes of the underlying payload; empty if not present. */
  public val bytes: HttpBytes

  /** @return The set [contentType] of the payload, or `application/octet-stream` as a default. */
  public fun contentTypeOrDefault(): Mimetype = contentType ?: Mimetype.OctetStream
}

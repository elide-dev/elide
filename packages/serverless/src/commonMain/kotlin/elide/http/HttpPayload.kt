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

package elide.http

import elide.core.encoding.Encoding
import elide.http.api.Mimetype
import elide.http.api.HttpPayload as HttpPayloadAPI

/**
 *
 */
public sealed class HttpPayload(
  override val bytes: HttpBytes,
  override val contentType: Mimetype? = null,
  override val size: ULong = bytes.size,
) : HttpPayloadAPI {
  override val mutable: Boolean get() = false
  override val present: Boolean get() = true

  /**
   *
   */
  public class Bytes(bytes: HttpBytes, contentType: Mimetype? = null) : HttpPayload(
    bytes,
    contentType,
  )

  /**
   *
   */
  public class Text(text: HttpBytes, contentType: Mimetype? = null, public val encoding: Encoding = Encoding.UTF_8) :
    HttpPayload(text, contentType)

  /**
   *
   */
  public data object Empty : HttpPayload(HttpBytes.EMPTY) {
    override val present: Boolean get() = false
  }
}

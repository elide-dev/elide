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

package elide.http

import elide.core.encoding.Encoding
import elide.http.api.HttpPayload as HttpPayloadAPI

/**
 *
 */
public sealed class HttpPayload : HttpPayloadAPI {
  public class Bytes(public val bytes: HttpBytes) : HttpPayload()
  public class Text(public val text: HttpBytes, public val encoding: Encoding = Encoding.UTF_8) : HttpPayload()
  public data object Empty : HttpPayload()
}

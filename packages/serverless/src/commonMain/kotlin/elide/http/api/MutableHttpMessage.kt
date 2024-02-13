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
 *
 */
@API public interface MutableHttpMessage : HttpMessage {
  override val mutable: Boolean get() = true

  /**
   * Headers of the message; for this type, the headers are mutable.
   */
  override val headers: MutableHttpHeaders

  /**
   * Body of the message; for this type, the body is mutable.
   */
  override val body: MutableHttpPayload
}

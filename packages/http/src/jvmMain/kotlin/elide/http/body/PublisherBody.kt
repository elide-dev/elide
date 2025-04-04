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

package elide.http.body

import java.net.http.HttpRequest
import elide.http.ContentLengthValue

// Implements a platform-specific body type for `java.net.http` body publishers.
@JvmInline internal value class PublisherBody (
  private val publisher: HttpRequest.BodyPublisher,
): PlatformBody<HttpRequest.BodyPublisher> {
  override val contentLength: ContentLengthValue get() = publisher.contentLength().toULong()
  override val isPresent: Boolean get() = true
  override fun unwrap(): HttpRequest.BodyPublisher = publisher
}

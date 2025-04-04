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

package elide.http.request

import java.net.URI
import elide.http.HttpUrl.PlatformHttpUrl
import elide.http.Params

// Implements `HttpUrl` via `java.net.URI`.
@JvmInline internal value class JavaNetHttpUri(private val uri: URI) : PlatformHttpUrl<URI> {
  override val value: URI get() = uri
  override val scheme: String get() = uri.scheme
  override val host: String get() = uri.host
  override val port: UShort get() = uri.port.toUShort()
  override val path: String get() = uri.path
  override val params: Params get() = uri.query?.let { Params.parse(it) } ?: Params.Empty
}

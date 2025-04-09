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
import elide.http.Http
import elide.http.HttpUrl.PlatformHttpUrl
import elide.http.Params

// Implements `HttpUrl` via `java.net.URI`.
@JvmInline public value class JavaNetHttpUri(
  private val pair: Pair<URI, Http.HttpRequestOptions?>,
) : PlatformHttpUrl<URI> {
  public constructor(uri: URI) : this(Pair(uri, null))

  override val value: URI get() = pair.first
  override val scheme: String get() = pair.second?.scheme ?: pair.first.scheme ?: "http:"
  override val host: String get() = pair.second?.host ?: pair.first.host ?: "localhost"
  override val port: UShort get() = pair.second?.port ?: pair.first.port.toUShort()
  override val path: String get() = pair.first.path ?: "/"
  override val params: Params get() = pair.first.query?.let { Params.parse(it) } ?: Params.Empty

  /** @return Absolute string calculated from this URL and any applicable context. */
  public fun absoluteString(): String = when (pair.second) {
    null -> pair.first.toString()
    else -> StringBuilder().apply {
      val effectiveScheme = scheme.ifBlank { null }
      val effectiveHost = host.ifBlank { null }
      val effectivePort = port.toInt().takeIf { it > 0 }
      val effectivePath = path.ifBlank { null }
      if (effectiveScheme != null && effectiveHost != null && effectivePort != null) {
        append(effectiveScheme).append("//").append(effectiveHost)
        append(':').append(effectivePort)
        append(effectivePath)
        if (params != Params.Empty) {
          append('?').append(params)
        }
      }
    }.toString()
  }

  override fun toString(): String = absoluteString()

  public companion object {
    @JvmStatic public fun from(uri: URI, options: Http.HttpRequestOptions?): JavaNetHttpUri =
      JavaNetHttpUri(Pair(uri, options))
  }
}

/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.ssr

import elide.vm.annotations.Polyglot

/**
 * # SSR: Rendered Stream
 *
 * Data class which represents a rendered and streamed SSR response. Such responses are composed of headers (as all HTTP
 * responses are), front matter (`<head>` content), and chunked body content.
 *
 * @param status HTTP return status.
 * @param html HTML code to be emitted.
 * @param headers HTTP headers to be emitted in the response.
 * @param criticalCss Critical CSS to be emitted as front-matter.
 * @param styleChunks Additional style chunks to emit.
 */
public data class RenderedStream
  @Polyglot
  constructor(
  public val status: Int,
  public val html: String,
  public val headers: Map<String, String>,
  public val criticalCss: String,
  public val styleChunks: Array<CssChunk>,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as RenderedStream

    if (status != other.status) return false
    if (html != other.html) return false
    if (headers != other.headers) return false
    if (criticalCss != other.criticalCss) return false
    if (!styleChunks.contentEquals(other.styleChunks)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = status
    result = 31 * result + html.hashCode()
    result = 31 * result + headers.hashCode()
    result = 31 * result + criticalCss.hashCode()
    result = 31 * result + styleChunks.contentHashCode()
    return result
  }
}

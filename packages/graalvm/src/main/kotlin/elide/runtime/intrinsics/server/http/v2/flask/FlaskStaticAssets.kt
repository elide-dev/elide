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
package elide.runtime.intrinsics.server.http.v2.flask

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.DefaultHttpContent
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.LastHttpContent
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile
import elide.runtime.intrinsics.server.http.v2.HttpContentSink

internal data object FlaskStaticAssetsRouter {
  private const val ASSETS_ROOT = "static"
  private val ASSET_ROUTE_REGEX = Regex("/static/(.+)(?:\\?.+)?")

  fun serveStaticAsset(applicationRoot: Path, context: FlaskHttpContext): Boolean {
    val asset = ASSET_ROUTE_REGEX.matchEntire(context.request.uri())?.groups[1]?.value
      ?: return false

    val file = applicationRoot
      .resolve(ASSETS_ROOT)
      .resolve(asset)

    if (file == null || !file.exists() || !file.isRegularFile()) {
      context.response.status = HttpResponseStatus.NOT_FOUND
      context.response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0)
      context.responseBody.close()

      return true
    }

    val stream = file.inputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

    context.response.status = HttpResponseStatus.OK
    context.response.headers().apply {
      set(HttpHeaderNames.CONTENT_TYPE, Files.probeContentType(file))
      set(HttpHeaderNames.CONTENT_LENGTH, file.fileSize())
    }

    context.responseBody.source(
      object : HttpContentSink.Producer {
        override fun pull(handle: HttpContentSink.Handle) {
          val read = stream.read(buffer)

          if (read == -1) {
            handle.push(LastHttpContent.EMPTY_LAST_CONTENT)
            handle.release(close = true)
          } else {
            val buf = Unpooled.copiedBuffer(buffer, 0, read)
            handle.push(DefaultHttpContent(buf))
          }
        }

        override fun released() {
          stream.close()
        }
      },
    )

    return true
  }
}

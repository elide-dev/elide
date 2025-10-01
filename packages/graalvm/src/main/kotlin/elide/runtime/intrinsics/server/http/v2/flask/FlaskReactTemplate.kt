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
import io.netty.handler.codec.http.DefaultLastHttpContent
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.getOrSet
import elide.runtime.Logging
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.server.http.v2.HttpContentSink

internal class FlaskReactTemplate(private val source: Path) : ProxyExecutable {
  private val sourceScript by lazy {
    Source.newBuilder("ts", source.toFile())
      .build()
  }

  private val template = ThreadLocal<Value>()

  override fun execute(vararg arguments: Value?): Any {
    val parsedTemplate = template.getOrSet {
      Context.getCurrent().eval(sourceScript).getMember("default")
    }

    val result = parsedTemplate.execute(arguments.firstOrNull())
    val promise = JsPromise.wrapOrNull(
      value = result,
      unwrapFulfilled = { it!!.asString() },
      unwrapRejected = { it },
    )

    if (promise == null) {
      if (!result.isString) error("React template did not render to a string and did not return a promise")
      else return result.asString()
    }

    return object : HttpContentSink.Producer {
      private val pulled = AtomicReference<HttpContentSink.Handle>()

      override fun pull(handle: HttpContentSink.Handle) {
        if (!promise.isDone) pulled.set(handle)
        promise.then(
          onFulfilled = { rendered ->
            pulled.get()?.push(DefaultLastHttpContent(Unpooled.copiedBuffer(rendered, Charsets.UTF_8)))
            handle.release(close = true)
          },
          onCatch = {
            logging.error("Failed to render React template: $it")
            handle.release(close = true)
          },
        )
      }

      override fun released() {
        pulled.set(null)
      }
    }
  }

  private companion object {
    private val logging by lazy { Logging.of(FlaskReactTemplate::class) }
  }
}

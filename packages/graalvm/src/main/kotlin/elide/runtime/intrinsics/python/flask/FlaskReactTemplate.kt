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
package elide.runtime.intrinsics.python.flask

import io.netty.buffer.Unpooled
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.getOrSet
import elide.runtime.http.server.HttpResponseSource
import elide.runtime.http.server.HttpResponseBody
import elide.runtime.intrinsics.js.JsPromise

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

    return object : HttpResponseSource {
      private val closed = AtomicBoolean(false)

      override fun onPull() = Unit
      override fun onClose(failure: Throwable?) = closed.set(true)
      override fun onAttached(writer: HttpResponseBody.Writer) {
        promise.then(
          onFulfilled = {
            if (closed.get()) return@then
            writer.write(Unpooled.copiedBuffer(it, Charsets.UTF_8))
            writer.end()
          },
          onCatch = {
            writer.end(IllegalStateException("React rendering failed: $it"))
          },
        )
      }
    }
  }
}

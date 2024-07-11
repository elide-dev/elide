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
package elide.runtime.gvm.internals.node.stream

import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.WebStreamsAPI
import elide.runtime.intrinsics.js.node.stream.Readable
import elide.runtime.intrinsics.js.node.stream.Writable
import elide.runtime.intrinsics.js.node.buffer.Buffer
import elide.runtime.intrinsics.js.JsPromise
import org.graalvm.polyglot.Value
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

// Internal symbol where the Node built-in module is installed.
private const val STREAM_WEB_MODULE_SYMBOL = "node_stream_web"

// Installs the Node stream module into the intrinsic bindings.
@Intrinsic @Factory internal class NodeStreamWebModule : AbstractNodeBuiltinModule() {
  @Singleton internal fun provide(): WebStreamsAPI = NodeWebStreams.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[STREAM_WEB_MODULE_SYMBOL.asJsSymbol()] = provide()
  }
}

/**
 * # Node API: `stream/web`
 */
internal class NodeWebStreams : WebStreamsAPI {
  //

  internal companion object {
    private val SINGLETON = NodeWebStreams()
    fun obtain(): NodeWebStreams = SINGLETON
  }

  /**
   * Consumes a Readable stream and returns a promise that resolves with the data as a string.
   *
   * @param stream The Readable stream to consume.
   * @param encoding The encoding to use. Default is UTF-8.
   * @return A promise that resolves with the data as a string.
   */
  fun text(stream: Readable, encoding: Charset = StandardCharsets.UTF_8): JsPromise<String> {
    return JsPromise.create { resolve, reject ->
      val builder = StringBuilder()
      stream.setEncoding(encoding.name())
      stream.on("data") { chunk ->
        builder.append(chunk.asString())
      }
      stream.on("end") {
        resolve(builder.toString())
      }
      stream.on("error") { error ->
        reject(error)
      }
    }
  }

  /**
   * Consumes a Readable stream and returns a promise that resolves with the data as a Buffer.
   *
   * @param stream The Readable stream to consume.
   * @return A promise that resolves with the data as a Buffer.
   */
  fun buffer(stream: Readable): JsPromise<Buffer> {
    return JsPromise.create { resolve, reject ->
      val chunks = mutableListOf<Buffer>()
      stream.on("data") { chunk ->
        chunks.add(chunk.asBuffer())
      }
      stream.on("end") {
        resolve(Buffer.concat(chunks.toTypedArray()))
      }
      stream.on("error") { error ->
        reject(error)
      }
    }
  }

  /**
   * Consumes a Readable stream and writes the data to a Writable stream.
   *
   * @param readable The Readable stream to consume.
   * @param writable The Writable stream to write to.
   * @return A promise that resolves when the operation is complete.
   */
  fun pipe(readable: Readable, writable: Writable): JsPromise<Unit> {
    return JsPromise.create { resolve, reject ->
      readable.pipe(writable)
      writable.on("finish") {
        resolve(Unit)
      }
      writable.on("error") { error ->
        reject(error)
      }
    }
  }

  /**
   * Consumes a Readable stream and writes the data to an OutputStream.
   *
   * @param readable The Readable stream to consume.
   * @param outputStream The OutputStream to write to.
   * @return A promise that resolves when the operation is complete.
   */
  fun writeToOutputStream(readable: Readable, outputStream: OutputStream): JsPromise<Unit> {
    return pipe(readable, Writable.wrap(outputStream))
  }

  /**
   * Consumes an InputStream and returns a promise that resolves with the data as a string.
   *
   * @param inputStream The InputStream to consume.
   * @param encoding The encoding to use. Default is UTF-8.
   * @return A promise that resolves with the data as a string.
   */
  fun text(inputStream: InputStream, encoding: Charset = StandardCharsets.UTF_8): JsPromise<String> {
    return text(Readable.wrap(inputStream, encoding), encoding)
  }

  /**
   * Consumes an InputStream and returns a promise that resolves with the data as a Buffer.
   *
   * @param inputStream The InputStream to consume.
   * @return A promise that resolves with the data as a Buffer.
   */
  fun buffer(inputStream: InputStream): JsPromise<Buffer> {
    return buffer(Readable.wrap(inputStream))
  }

  /**
   * Consumes an InputStream and writes the data to a Writable stream.
   *
   * @param inputStream The InputStream to consume.
   * @param writable The Writable stream to write to.
   * @return A promise that resolves when the operation is complete.
   */
  fun pipe(inputStream: InputStream, writable: Writable): JsPromise<Unit> {
    return pipe(Readable.wrap(inputStream), writable)
  }

  /**
   * Consumes an InputStream and writes the data to an OutputStream.
   *
   * @param inputStream The InputStream to consume.
   * @param outputStream The OutputStream to write to.
   * @return A promise that resolves when the operation is complete.
   */
  fun writeToOutputStream(inputStream: InputStream, outputStream: OutputStream): JsPromise<Unit> {
    return writeToOutputStream(Readable.wrap(inputStream), outputStream)
  }
}

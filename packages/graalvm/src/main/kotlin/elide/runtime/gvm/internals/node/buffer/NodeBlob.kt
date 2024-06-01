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

package elide.runtime.gvm.internals.node.buffer

import org.graalvm.polyglot.HostAccess.Implementable
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.node.BufferAPI
import elide.vm.annotations.Polyglot

@DelicateElideApi @Implementable internal open class NodeBlob internal constructor(
  val bytes: ByteArray,
  @Polyglot override val type: String?
) : BufferAPI.Blob {
  @Polyglot constructor() : this(sources = null, options = null)

  @Polyglot constructor(sources: PolyglotValue) : this(sources, options = null)

  @Polyglot constructor(sources: PolyglotValue?, options: PolyglotValue?) : this(
    bytes = makeBlobBytes(sources, options),
    type = NewBlobOptions.type(options),
  )

  @Polyglot override val size: Int = bytes.size

  @Polyglot override fun arrayBuffer(): JsPromise<PolyglotValue> {
    return JsPromise.resolved(PolyglotValue.asValue(ByteBuffer.wrap(bytes)))
  }

  @Polyglot override fun slice(start: Int?, end: Int?, type: String?): NodeBlob {
    return NodeBlob(bytes.copyOfRange(start ?: 0, end ?: (bytes.size)), type)
  }

  @Polyglot override fun text(): JsPromise<String> {
    return JsPromise.resolved(bytes.toString(Charsets.UTF_8))
  }

  @Polyglot override fun stream(): ReadableStream {
    return ReadableStream.wrap(bytes)
  }

  protected object NewBlobOptions {
    fun nativeEndings(options: PolyglotValue?): Boolean {
      return options?.getMember("endings")?.asString() == "native"
    }

    fun type(options: PolyglotValue?): String? {
      return options?.getMember("type")?.asString()
    }
  }

  protected companion object {
    private const val BUFFER_MEMBER_KEY = "buffer"

    private val LineEndRegex = Regex("\r?\n")

    @JvmStatic @DelicateElideApi protected fun makeBlobBytes(
      sources: PolyglotValue? = null,
      options: PolyglotValue? = null
    ): ByteArray {
      val nativeEndings = NewBlobOptions.nativeEndings(options)

      val bytes = if (sources == null) ByteArray(0) else ByteArrayOutputStream().use { out ->
        require(sources.hasArrayElements()) { "Blob sources argument must be an array" }

        for (i in 0 until sources.arraySize) readSource(sources.getArrayElement(i), out, nativeEndings)
        out.toByteArray()
      }

      return bytes
    }

    private fun readSource(source: PolyglotValue, out: OutputStream, nativeLineEndings: Boolean) = when {
      // a String is the most used source in code samples and the simplest to support
      source.isString -> readStringSource(source, out, nativeLineEndings)
      // an ArrayBuffer can be read directly
      source.hasBufferElements() -> readBufferSource(source, out)
      // all TypedArray and DataView have a buffer we can use
      source.hasMembers() -> readObjectSource(source, out)
      // could be an instance of 'Blob', try to unwrap it
      source.isHostObject -> readBlobSource(source, out)
      // we've tried everything, this is an unknown source
      else -> throw IllegalArgumentException("Invalid source type")
    }

    private fun readStringSource(source: PolyglotValue, out: OutputStream, nativeLineEndings: Boolean) {
      val string = if (!nativeLineEndings) source.asString()
      else source.asString().replace(LineEndRegex, System.lineSeparator())

      out.write(string.toByteArray(Charsets.UTF_8))
    }

    private fun readBufferSource(source: PolyglotValue, out: OutputStream) {
      val bytes = ByteArray(source.bufferSize.toInt())
      source.readBuffer(/*byteOffset = */ 0, bytes, /*destinationOffset = */ 0, bytes.size)
      out.write(bytes)
    }

    private fun readObjectSource(source: PolyglotValue, out: OutputStream) {
      // requires a 'buffer' member, which has buffer elements, so we can just 'readBufferSource' with it
      val buffer = source.getMember(BUFFER_MEMBER_KEY) ?: throw IllegalArgumentException(
        "Source must be a String, ArrayBuffer, or have a 'buffer' member",
      )

      readBufferSource(buffer, out)
    }

    private fun readBlobSource(source: PolyglotValue, out: OutputStream) {
      // try to force a conversion but use a meaningful message on failure
      val blob = runCatching { source.asHostObject<NodeBlob>() }.getOrNull() ?: throw IllegalArgumentException(
        "Expected source to be a Blob instance",
      )

      out.write(blob.bytes)
    }
  }
}

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
package elide.runtime.node.buffer

import org.graalvm.polyglot.HostAccess.Implementable
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.node.BufferAPI
import elide.vm.annotations.Polyglot

import elide.runtime.intrinsics.js.Blob

/**
 * Implements the `Blob` type from the Node.js `buffer` built-in module. Blobs are read-only chunks of byte data which
 * can be used to derive buffers, strings, and other objects.
 */
@DelicateElideApi @Implementable public open class NodeBlob internal constructor(
  internal val bytes: ByteArray,
  @Polyglot override val type: String?
) : Blob, ProxyObject {
  /** Creates a new empty buffer. */
  @Polyglot public constructor() : this(sources = null, options = null)

  /**
   * Creates a new buffer filled by concatenating the provider [sources]. The [sources] object must have array elements
   * of a supported type, namely [buffers][PolyglotValue.hasBufferElements], or objects with a `buffer` member with the
   * same effect.
   */
  @Polyglot public constructor(sources: PolyglotValue) : this(sources, options = null)

  /**
   * Creates a new buffer filled by concatenating the provider [sources]. The [sources] object must have array elements
   * of a supported type, namely [buffers][PolyglotValue.hasBufferElements], or objects with a `buffer` member with the
   * same effect.
   *
   * The `options` object can be used to specify the content [type] of the blob by setting the `type` property to a
   * string value. If the `endings` property is set to `"native"`, all line endings in the sources will be converted
   * to the format of the current platform.
   */
  @Polyglot public constructor(sources: PolyglotValue?, options: PolyglotValue?) : this(
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

  override fun getMemberKeys(): Any {
    return members
  }

  override fun hasMember(key: String): Boolean {
    return members.binarySearch(key) >= 0
  }

  override fun getMember(key: String?): Any? = when (key) {
    "arrayBuffer" -> ProxyExecutable { arrayBuffer() }
    "size" -> size

    "slice" -> ProxyExecutable {
      slice(it.getOrNull(0)?.asInt(), it.getOrNull(1)?.asInt(), it.getOrNull(2)?.asString())
    }

    "stream" -> ProxyExecutable { stream() }
    "text" -> ProxyExecutable { text() }
    "type" -> type

    else -> null
  }

  override fun putMember(key: String?, value: PolyglotValue?) {
    throw UnsupportedOperationException("Cannot modify 'Blob' object")
  }

  /** Helper object used to extract values from constructor option structs. */
  protected object NewBlobOptions {
    /**
     * Reads the `endings` property from the given [options] object if it exists, returning `true` if its value is the
     * string 'native', and false in every other case.
     */
    internal fun nativeEndings(options: PolyglotValue?): Boolean {
      return options?.getMember("endings")?.asString() == "native"
    }

    /**
     * Reads the value of the `type` property from the given [options] object if it exists, returning `null` otherwise.
     */
    internal fun type(options: PolyglotValue?): String? {
      return options?.getMember("type")?.asString()
    }
  }

  protected companion object {
    /**
     * Name of a property commonly exposed by objects that wrap buffers (e.g. typed arrays), used to coerce a value
     * into a buffer (by using its wrapped buffer value instead) if it does not have buffer elements itself.
     */
    private const val BUFFER_MEMBER_KEY = "buffer"

    /** Simple Regular Expression used to detect line endings in source strings. */
    private val LineEndRegex = Regex("\r?\n")

    /** Static, sorted array of members visible to guest code. */
    private val members = arrayOf(
      "arrayBuffer",
      "size",
      "slice",
      "stream",
      "text",
      "type",
    ).apply { sort() }

    /**
     * Construct a [ByteArray] by concatenating all the [sources]. If the [options] object requests platform-specific
     * line endings, they will replace all line endings present in the source values.
     *
     * The [sources] value must have array elements of a supported type (buffer-like or exposing a `buffer` property),
     * or this operation will fail with an exception.
     */
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

    /**
     * Read from a string or buffer-like [source] into an [out] stream, optionally replacing line endings with a
     * platform-specific sequence. This method supports strings, buffers, typed arrays, data views, and blobs as
     * sources.
     */
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

    /**
     * Read a guest string [source] to an output stream, optionally replacing line endings with a platform-specific
     * sequence. If the value [is not a string][PolyglotValue.isString], an exception will be thrown.
     */
    private fun readStringSource(source: PolyglotValue, out: OutputStream, nativeLineEndings: Boolean) {
      val string = if (!nativeLineEndings) source.asString()
      else source.asString().replace(LineEndRegex, System.lineSeparator())

      out.write(string.toByteArray(Charsets.UTF_8))
    }

    /**
     * Read a guest buffer [source] to an output stream, optionally replacing line endings with a platform-specific
     * sequence. If the value [is not a buffer][PolyglotValue.hasBufferElements], an exception will be thrown.
     */
    private fun readBufferSource(source: PolyglotValue, out: OutputStream) {
      val bytes = ByteArray(source.bufferSize.toInt())
      source.readBuffer(/*byteOffset = */ 0, bytes, /*destinationOffset = */ 0, bytes.size)
      out.write(bytes)
    }

    /**
     * Read a guest object [source] to an output stream, optionally replacing line endings with a platform-specific
     * sequence. The value must have a 'buffer' member with [buffer elements][PolyglotValue.hasBufferElements], or
     * an exception will be thrown.
     */
    private fun readObjectSource(source: PolyglotValue, out: OutputStream) {
      // requires a 'buffer' member, which has buffer elements, so we can just 'readBufferSource' with it
      val buffer = source.getMember(BUFFER_MEMBER_KEY) ?: throw IllegalArgumentException(
        "Source must be a String, ArrayBuffer, or have a 'buffer' member",
      )

      readBufferSource(buffer, out)
    }

    /**
     * Read a guest blob [source] to an output stream, optionally replacing line endings with a platform-specific
     * sequence. The value must be a [host object][PolyglotValue.isHostObject] of type [NodeBlob], otherwise this
     * operation will fail.
     */
    private fun readBlobSource(source: PolyglotValue, out: OutputStream) {
      // try to force a conversion but use a meaningful message on failure
      val blob = runCatching { source.asHostObject<NodeBlob>() }.getOrNull() ?: throw IllegalArgumentException(
        "Expected source to be a Blob instance",
      )

      out.write(blob.bytes)
    }
  }
}

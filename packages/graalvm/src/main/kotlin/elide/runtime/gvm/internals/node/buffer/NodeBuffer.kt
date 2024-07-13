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

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.util.*
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.BufferAPI
import elide.runtime.intrinsics.js.node.buffer.BufferClass

/** Internal symbol where the bindings for the 'buffer' module are installed. */
private const val BUFFER_MODULE_SYMBOL_ROOT = "node_buffer"

/** Symbol at which the main module intrinsic is installed. */
private const val BUFFER_MODULE_SYMBOL = "${BUFFER_MODULE_SYMBOL_ROOT}_module"

/** Symbol at which the [NodeBlob] class is installed. */
private const val BLOB_SYMBOL = "Blob"

/** Symbol at which the [NodeBlob] class is installed. */
private const val FILE_SYMBOL = "File"

/** Symbol at which the [NodeBlob] class is installed. */
private const val BUFFER_TYPE_SYMBOL = "Buffer"

// Installs the Node `buffer` built-in module.
@Intrinsic internal class NodeBufferModule : AbstractNodeBuiltinModule() {
  @Inject lateinit var facade: NodeBufferModuleFacade

  @OptIn(DelicateElideApi::class)
  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[BUFFER_MODULE_SYMBOL.asJsSymbol()] = facade
    bindings[BLOB_SYMBOL.asPublicJsSymbol()] = NodeBlob::class.java
    bindings[FILE_SYMBOL.asPublicJsSymbol()] = NodeFile::class.java

    // A single NodeBufferClass instance acts as meta-object for the `Buffer` type;
    // it will also be exposed as part of the `node:buffer` module by guest init code
    bindings[BUFFER_TYPE_SYMBOL.asPublicJsSymbol()] = NodeBufferClass()
  }
}

// Module facade which satisfies the built-in `Buffer` module.
@Singleton internal class NodeBufferModuleFacade : BufferAPI, ProxyObject {
  override fun atob(data: Value): String {
    val base64 = if (data.isString) data.asString() else data.toString()
    val bytes = Base64.getDecoder().decode(base64)

    return bytes.toString(Charsets.ISO_8859_1)
  }

  override fun btoa(data: Value): String {
    val ascii = if (data.isString) data.asString() else data.toString()
    val bytes = ascii.toByteArray(Charsets.ISO_8859_1)

    return Base64.getEncoder().encodeToString(bytes)
  }

  override fun isAscii(input: Value): Boolean {
    val buffer = coerceIntoBuffer(input)
    if (buffer.bufferSize == 0L) return true

    for (i in 0 until buffer.bufferSize) {
      if (buffer.readBufferByte(i) <= 0) return false
    }

    return true
  }

  @Suppress("MagicNumber", "ReturnCount")
  override fun isUtf8(input: Value): Boolean {
    val buffer = coerceIntoBuffer(input)
    val bufferSize = buffer.bufferSize
    if (bufferSize == 0L) return true

    var expected = 0 // number of bytes remaining for current character
    for (i in 0 until bufferSize) {
      // fail early if not enough characters are left
      if (bufferSize - i < expected) return false

      val byte = buffer.readBufferByte(i).toInt()
      when (expected) {
        // no more bytes expected, begin new character
        0 -> expected = when {
          // single-byte character (0xxx xxxx)
          (byte shr UTF8_PREFIX_1B_OFFSET) == UTF8_PREFIX_1B -> 0

          // two-byte character (110x xxxx)
          (byte shr UTF8_PREFIX_2B_OFFSET) == UTF8_PREFIX_2B -> 1

          // three-byte character (1110 xxxx)
          (byte shr UTF8_PREFIX_3B_OFFSET) == UTF8_PREFIX_3B -> 2

          // four-byte character (1111 0xxx)
          (byte shr UTF8_PREFIX_4B_OFFSET) == UTF8_PREFIX_4B -> 3

          else -> return false // invalid UTF-8 sequence
        }

        // multi-byte character, each byte after the first starts with 10xxxxxx
        else -> {
          if (byte shr UTF8_PREFIX_MULTIBYTE_OFFSET != UTF8_PREFIX_MULTIBYTE) return false
          expected--
        }
      }
    }

    return true
  }

  override fun resolveObjectUrl(id: String): Value {
    error("Implementation provided by guest code")
  }

  override fun transcode(source: Value, fromEnc: String, toEnc: String): Value {
    // blocked until creating 'Buffer' instances from the host is available
    TODO("Not yet implemented")
  }

  /**
   * Attempt to coerce the given [value] into one that has [buffer elements][Value.hasBufferElements]:
   *
   * - If the value [has buffer elements][Value.hasBufferElements], it is returned.
   * - If it has a 'buffer' member, it is checked for buffer elements and returned.
   * - If none of the above is true, an exception is thrown.
   */
  private fun coerceIntoBuffer(value: Value): Value {
    if (value.hasBufferElements()) return value
    if (value.hasMembers() && value.hasMember("buffer")) {
      val buffer = value.getMember("buffer")
      if (buffer.hasBufferElements()) return buffer
    }

    error("Unable to read buffer elements from the specified value.")
  }

  override fun getMemberKeys(): Any {
    return moduleMembers
  }

  override fun hasMember(key: String): Boolean {
    return moduleMembers.binarySearch(key) >= 0
  }

  @Suppress("MagicNumber")
  override fun getMember(key: String?): Any? = when (key) {
    "atob" -> ProxyExecutable { atob(it.singleOrNull() ?: error("Expected exactly 1 argument for 'atob'")) }
    "btoa" -> ProxyExecutable { btoa(it.singleOrNull() ?: error("Expected exactly 1 argument for 'btoa'")) }
    "isAscii" -> ProxyExecutable { isAscii(it.singleOrNull() ?: error("Expected exactly 1 argument for 'isAscii'")) }
    "isUtf8" -> ProxyExecutable { isUtf8(it.singleOrNull() ?: error("Expected exactly 1 argument for 'isUtf8'")) }

    "transcode" -> ProxyExecutable {
      if (it.size != 3) error("Expected exactly 1 argument for 'transcode'")
      transcode(it[0], it[1].asString(), it[2].asString())
    }

    "resolveObjectUrl" -> ProxyExecutable {
      val id = it.singleOrNull()?.asString() ?: error("Expected exactly 1 argument for 'isUtf8'")
      resolveObjectUrl(id)
    }

    else -> null
  }

  override fun putMember(key: String?, value: Value?) {
    throw UnsupportedOperationException("Cannot modify 'buffer' module")
  }

  private companion object {
    /** Single-byte character bit prefix (0xxx xxxx) */
    private const val UTF8_PREFIX_1B = 0

    /** Number of discarded bits when checking UTF-8 validity for 1-byte characters. */
    private const val UTF8_PREFIX_1B_OFFSET = 7

    /** Two-byte character bit prefix (110x xxxx) */
    private const val UTF8_PREFIX_2B = 0x06

    /** Number of discarded bits when checking UTF-8 validity for 2-byte characters. */
    private const val UTF8_PREFIX_2B_OFFSET = 5

    /** Three-byte character bit prefix (1110 xxxx) */
    private const val UTF8_PREFIX_3B = 0x0E

    /** Number of discarded bits when checking UTF-8 validity for 3-byte characters. */
    private const val UTF8_PREFIX_3B_OFFSET = 4

    /** Four-byte character bit prefix (1111 0xxx) */
    private const val UTF8_PREFIX_4B = 0x1E

    /** Number of discarded bits when checking UTF-8 validity for 4-byte characters. */
    private const val UTF8_PREFIX_4B_OFFSET = 3

    /** Prefix used by follow-up bytes in a multibyte character (10xxxxxx). */
    private const val UTF8_PREFIX_MULTIBYTE = 2

    /** Number of discarded bits when checking UTF-8 validity of follow-up bytes in multibyte characters. */
    private const val UTF8_PREFIX_MULTIBYTE_OFFSET = 6

    /** Static list of members exposed to guest code. */
    private val moduleMembers = arrayOf(
      "atob",
      "btoa",
      "isAscii",
      "isUtf8",
      "transcode",
      "resolveObjectUrl",
    ).apply { sort() }
  }
}

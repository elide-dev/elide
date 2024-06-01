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
import java.util.*
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.BufferAPI

/** Internal symbol where the bindings for the 'buffer' module are installed. */
private const val BUFFER_MODULE_SYMBOL_ROOT = "__Elide_node_buffer__"

/** Symbol at which the main module intrinsic is installed. */
private const val BUFFER_MODULE_SYMBOL = "${BUFFER_MODULE_SYMBOL_ROOT}module"

/** Symbol at which the [NodeBlob] class is installed. */
private const val BLOB_SYMBOL = "${BUFFER_MODULE_SYMBOL_ROOT}Blob"

/** Symbol at which the [NodeBlob] class is installed. */
private const val FILE_SYMBOL = "${BUFFER_MODULE_SYMBOL_ROOT}File"

// Installs the Node `buffer` built-in module.
@Intrinsic internal class NodeBufferModule : AbstractNodeBuiltinModule() {
  @Inject lateinit var facade: NodeBufferModuleFacade

  @OptIn(DelicateElideApi::class)
  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[BUFFER_MODULE_SYMBOL.asJsSymbol()] = facade
    bindings[BLOB_SYMBOL.asJsSymbol()] = NodeBlob::class.java
    bindings[FILE_SYMBOL.asJsSymbol()] = NodeFile::class.java
  }
}

// Module facade which satisfies the built-in `Buffer` module.
@Singleton internal class NodeBufferModuleFacade : BufferAPI {
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
          (byte shr 7) == 0 -> 0

          // two-byte character (110x xxxx)
          (byte shr 5) == 0x06 -> 1

          // three-byte character (1110 xxxx)
          (byte shr 4) == 0x0E -> 2

          // four-byte character (1111 0xxx >> 3)
          (byte shr 3) == 0x1E -> 3

          else -> return false // invalid UTF-8 sequence
        }

        // multi-byte character, each byte after the first starts with 10xxxxxx
        else -> {
          if (byte shr 6 != 2) return false
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
}

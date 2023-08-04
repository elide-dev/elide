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

package elide.rpc.server.web

import com.google.common.annotations.VisibleForTesting
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.or
import elide.runtime.Logger
import elide.runtime.Logging


/**
 * Responsible for de-framing gRPC-Web messages which have been joined to account for HTTP/2 trailers and the potential
 * for multiple data segments.
 *
 * [processInput] takes a generic [InputStream] which is expected to contain one or more `DATA` frames, as demarcated by
 * a pattern of bytes assembled by [MessageFramer] or a similar implementation. Messages are extracted from their `DATA`
 * packing and held for downstream processing.
 */
internal class MessageDeframer {
  private val logging: Logger = Logging.of(MessageDeframer::class)
  private var length: Int = 0
  private var readSoFar: Int = 0
  private var frameCount: Int = 0
  private var frames: MutableList<ByteArray> = arrayListOf()
  private var msg: ByteArray? = null
  private var malformed: Boolean = false

  /** Size of the stream consumed by this deframer instance. */
  val size: Int get() = length

  /** Count of discovered gRPC-Web `DATA` frames consumed by this deframer instance. */
  val count: Int get() = frameCount

  // Retrieve the next frame's worth of bytes from the input stream.
  private fun getNextFrameBytes(input: ByteArray): Boolean {
    // first byte of the stream should be 0x00 (indicating a DATA frame)
    val firstByteValue: Int = (input[readSoFar] or RpcSymbol.DATA.value).toInt()
    return if (firstByteValue != 0) {
      logging.trace {
        "Finished consuming bytes from stream frame"
      }
      malformed = true
      false  // terminate
    } else {
      // following 4 bytes = length of the byte array containing message data
      var offset = readSoFar + 1
      val len = ByteBuffer.wrap(input, offset, 4).int

      // special case: empty messages
      if (len == 0) {
        frames.add(ByteArray(0))
        false  // terminate
      } else {
        val expectedNumBytes = len + 5 + readSoFar
        if (input.size < expectedNumBytes) {
          logging.warning {
            "Input stream did not provide enough bytes for decoding as gRPC-Web. " +
            "Expected: $expectedNumBytes, but found ${input.size}."
          }
          malformed = true
          false  // terminate
        } else {
          // read `len` into message
          length += len
          offset += 4
          val inputBytes: ByteArray = input.copyOfRange(offset, len + offset)
          frames.add(inputBytes)
          readSoFar += len + 5

          // if there are remaining bytes, there may be another frame to process
          return input.size > readSoFar
        }
      }
    }
  }

  /**
   * @return Byte array of the decoded [msg], or an empty byte array if no message was decoded.
   */
  fun toByteArray(): ByteArray = msg ?: ByteArray(0)

  /**
   * Process the input [stream] to extract any present `DATA` frames, formatted for expression via gRPC-Web; collect
   * each frame within the [frames] list, and return a boolean indicating whether the stream was well-formed.
   *
   * @param stream Input stream to consume `DATA` frames from.
   * @param format gRPC expression format to decode this stream as.
   * @return Whether the stream was well-formed. If `false` is returned, the resulting [frames] list will be empty and
   *   should not be used; an error should be raised to the invoking caller describing an invalid request condition.
   */
  @VisibleForTesting
  fun processInput(stream: InputStream, format: GrpcWebContentType): Boolean {
    // read stream into an array of bytes
    val messageBytes: ByteArray = try {
      (if (format == GrpcWebContentType.TEXT) {
        Base64.getDecoder().wrap(stream)
      } else {
        stream
      }).readAllBytes()
    } catch (ioe: IOException) {
      ioe.printStackTrace()
      logging.warning("Invalid input gRPC-Web message input", ioe)
      return false
    }

    // check minimum stream size
    if (messageBytes.size < 5) {
      logging.warning {
        "Invalid input gRPC-Web message input: expected at least 5 bytes, but got ${messageBytes.size}"
      }
      return false
    }

    while (getNextFrameBytes(messageBytes)) {
      // nothing: `getNextFrameBytes` will tell us when processing completes
    }
    frameCount = frames.size

    // often, there is just one frame (unary call that fits).
    if (frameCount == 1) {
      msg = frames.first()
    } else {
      val outStream = ByteArrayOutputStream()
      outStream.use { out ->
        frames.forEach { frame ->
          out.writeBytes(frame)
        }
      }
      msg = outStream.toByteArray()
      frames.clear()
    }
    return !malformed
  }
}

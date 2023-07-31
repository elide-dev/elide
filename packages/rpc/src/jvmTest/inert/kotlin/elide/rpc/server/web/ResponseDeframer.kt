package elide.rpc.server.web

import com.google.common.annotations.VisibleForTesting
import io.grpc.Metadata
import io.grpc.Status
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.experimental.or
import kotlin.experimental.xor
import kotlin.test.assertNotNull

/** gRPC Web response decoder -- only needed during testing. */
class ResponseDeframer {
  private var length: Int = 0
  private var readSoFar: Int = 0
  private var frameCount: Int = 0
  private var frames: MutableList<ByteArray> = arrayListOf()
  private var trailerDataReadSoFar: Int = 0
  private var msg: ByteArray? = null
  val trailers: Metadata = Metadata()
  var status: Status? = null

  companion object {
    private val colonByteUtf8 = ":".toByteArray(StandardCharsets.UTF_8).first()
    private val carriageReturnByteUtf8 = "\r".toByteArray(StandardCharsets.UTF_8).first()
  }

  /** Size of the stream consumed by this deframer instance. */
  val size: Int get() = length

  /** Count of discovered gRPC-Web `DATA` frames consumed by this deframer instance. */
  val count: Int get() = frameCount

  // Decode a single key=>value trailer of a gRPC Web response trailer stanza.
  private fun decodeSingleTrailer(input: ByteArray): Boolean {
    if (trailerDataReadSoFar >= input.size) {
      return false
    }
    val offsetInput = input.slice(trailerDataReadSoFar until input.size).toByteArray()
    val keyPos = offsetInput.indexOf(colonByteUtf8)
    if (keyPos != -1) {
      // decode string
      trailerDataReadSoFar += (keyPos + 1)  // account for `:` separator
      val returnPos = offsetInput.indexOf(carriageReturnByteUtf8)
      if (returnPos == -1) {
        throw IllegalStateException(
          "Failed to decode trailer key/value pair: carriage return not found in offset input: " +
          "Base64(${Base64.getEncoder().encodeToString(offsetInput)}) from original input " +
          "Base64(${Base64.getEncoder().encodeToString(input)}) with key slice position '${keyPos}'"
        )
      }

      // extract bytes for the value
      val valueBytes = offsetInput.slice(
        (keyPos + 1) until returnPos
      ).toByteArray()
      trailerDataReadSoFar += (returnPos - keyPos) + 1

      val key = String(offsetInput.slice(0 until keyPos).toByteArray(), StandardCharsets.UTF_8)
      if (key.endsWith(GrpcWeb.BINARY_HEADER_SUFFIX)) {
        // it's a binary trailer: decode the value as raw bytes.
        val metadataKey = Metadata.Key.of(
          key,
          Metadata.BINARY_BYTE_MARSHALLER
        )
        trailers.put(
          metadataKey,
          valueBytes,
        )
      } else {
        // it's a text trailer: decode the value as a UTF-8 string.
        val metadataKey = Metadata.Key.of(
          key,
          Metadata.ASCII_STRING_MARSHALLER
        )
        trailers.put(
          metadataKey,
          String(valueBytes, StandardCharsets.UTF_8)
        )

        // special case: if it's the status header, interpret it
        if (key == GrpcWeb.Headers.status) {
          // parse it as a status
          val statusAsNum = try {
            Integer.parseInt(String(valueBytes, StandardCharsets.UTF_8))
          } catch (err: IllegalArgumentException) {
            throw IllegalArgumentException(
              "Failed to decode header value as status code: '${String(valueBytes, StandardCharsets.UTF_8)}'"
            )
          }

          val statusValue = Status.Code.values().firstOrNull {
            it.value() == statusAsNum
          }
          assertNotNull(
            statusValue,
            "should get resolvable gRPC status value back from status code trailer"
          )
          status = Status.fromCode(statusValue)
        }
      }
      return trailerDataReadSoFar < input.size
    }
    throw IllegalStateException(
      "Failed to decode trailer key/value pair: key separator (`:`) not found in input: " +
      "Base64(${Base64.getEncoder().encodeToString(input)})"
    )
  }

  // Decode the trailer portion of a gRPC Web response.
  private fun decodeTrailerStanza(input: ByteArray): Boolean {
    val firstByte: Int = (input[0] xor RpcSymbol.TRAILER.value).toInt()
    return if (firstByte != 0) {
      throw IllegalStateException(
        "Trailer portion of response body did not start with TRAILER symbol byte"
      )
    } else {
      // grab next 4 bytes, which is the length of the trailer data stanza
      var offset = trailerDataReadSoFar + 1  // ignore the sentinel byte
      val len = ByteBuffer.wrap(input, offset, 4).int

      if (len == 0) {
        throw IllegalStateException(
          "gRPC Web responses must contain a trailer portion with their status, but found no trailer (size=0)"
        )
      }
      // we expect to see the prefix, plus the trailer size, plus the offset (ignoring symbolic bytes)
      val expectedNumBytes = len + 5 + trailerDataReadSoFar
      if (input.size != expectedNumBytes) {
       throw IllegalStateException(
         "Wrong number of bytes reported for trailer (expected $expectedNumBytes, but got ${input.size})"
       )
      }

      // advance to skip over the length tag
      offset += 4
      val trailerStanzaBytes: ByteArray = input.copyOfRange(offset, len + offset)

      while (decodeSingleTrailer(trailerStanzaBytes)) {
        // decoded a trailer, and should continue until `true` is returned, indicating that we're done.
      }
      false  // terminates outer loop
    }
  }

  // Retrieve the next frame's worth of bytes from the input stream.
  private fun getNextFrameBytes(input: ByteArray): Boolean {
    // first byte of the stream should be 0x00 (indicating a DATA frame)
    val firstByteValue: Int = (input[readSoFar] or RpcSymbol.DATA.value).toInt()
    val firstByteIsTrailers: Int = (input[readSoFar] xor RpcSymbol.TRAILER.value).toInt()
    return if (firstByteValue != 0) {
      if (firstByteIsTrailers == 0) {
        decodeTrailerStanza(
          input.slice(readSoFar until input.size).toByteArray()
        )
      } else {
        false  // terminate, stream is done and the next portion isn't a trailer
      }
    } else {
      // following 4 bytes = length of the byte array containing message data
      var offset = readSoFar + 1
      val len = ByteBuffer.wrap(input, offset, 4).int

      // special case: empty messages
      if (len == 0) {
        frames.add(ByteArray(0))
        readSoFar += 5

        // if there are remaining bytes, there may be trailers to read
        input.size > readSoFar
      } else {
        val expectedNumBytes = len + 5 + readSoFar
        if (input.size < expectedNumBytes) {
          throw IllegalStateException(
            "Input stream did not provide enough bytes for decoding as gRPC-Web. " +
            "Expected: $expectedNumBytes, but found ${input.size}."
          )
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
      throw ioe
    }

    // check minimum stream size
    if (messageBytes.size < 5) {
      throw IllegalStateException(
        "Stream was too short: expected at least 5 bytes, but got '${messageBytes.size}'"
      )
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
    return true
  }
}

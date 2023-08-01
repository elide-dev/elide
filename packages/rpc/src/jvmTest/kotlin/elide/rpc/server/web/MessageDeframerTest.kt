package elide.rpc.server.web

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.test.*

/** Tests for [MessageDeframer]. */
@MicronautTest(startApplication = false)
class MessageDeframerTest {
  private fun encodeAsFrame(message: String, format: GrpcWebContentType = GrpcWebContentType.BINARY): ByteArray {
    val output = ByteArrayOutputStream()
    output.write(ByteArray(1) { RpcSymbol.DATA.value })
    output.write(ByteBuffer.allocate(4).putInt(message.length).array())
    output.write(message.toByteArray(StandardCharsets.UTF_8))
    return if (format == GrpcWebContentType.TEXT) Base64.getEncoder().encode(
      output.toByteArray(),
    ) else output.toByteArray()
  }

  private fun encodeAsStream(message: String, format: GrpcWebContentType = GrpcWebContentType.BINARY): InputStream {
    return encodeAsFrame(message, format).inputStream()
  }

  @Test fun testCreateDeframer() {
    // should be able to construct a deframer directly
    val deframer = MessageDeframer()
    assertNotNull(deframer, "should be able to construct a gRPC-Web message deframer")
  }

  @Test fun testEmptyDeframerEmptyBytes() {
    // an empty deframer should provide an empty byte array
    val deframer = MessageDeframer()
    val bytes = deframer.toByteArray()
    assertEquals(
      0,
      bytes.size,
      "size of data array provided by `MessageDeframer` should be `0` to start",
    )
  }

  // Malformed Cases

  @Test fun testMalformedEmptyStream() {
    val deframer = MessageDeframer()
    assertFalse(
      deframer.processInput(ByteArray(0).inputStream(), GrpcWebContentType.BINARY),
      "empty stream should return `false` to indicate it is malformed",
    )
  }

  @Test fun testMalformedSwallowIOE() {
    val deframer = MessageDeframer()
    assertDoesNotThrow {
      assertFalse(
        deframer.processInput(
          object : InputStream() {
            override fun read(): Int {
              throw IOException("fake ioexception")
            }
          },
          GrpcWebContentType.BINARY,
        ),
        "empty stream should return `false` to indicate it is malformed",
      )
    }
  }

  @Test fun testMultiSegmentDoubleEncoded() {
    val format = GrpcWebContentType.TEXT
    val frameCount = 2
    val testFrames = ArrayList<String>(frameCount)
    for (i in 0 until frameCount) {
      testFrames.add("this is a test frame (number $i)")
    }

    val allbytes = ByteArrayOutputStream()
    testFrames.forEach { frame ->
      allbytes.writeBytes(encodeAsFrame(frame, format))  // deliberately encode with inner base64
    }
    val encodedBytes = Base64.getEncoder().encode(allbytes.toByteArray())
    val encodedInputStream = encodedBytes.inputStream()

    // with our test stream prepared, it's time to test the deframer
    val deframer = MessageDeframer()
    assertFalse(
      deframer.processInput(encodedInputStream, format),
      "deframer should indicate `false` (malformed stream) for double-encoded TEXT stream",
    )
    assertEquals(
      0,
      deframer.count,
      "count of frames from malformed stream should be `0`",
    )
  }

  @Test fun testMalformedSegmentLength() {
    val allbytes = ByteArrayOutputStream()
    val testString = "this will be malformed"
    allbytes.use {
      // write symbol
      allbytes.writeBytes(ByteArray(1) { RpcSymbol.DATA.value })
      allbytes.write(
        ByteBuffer.allocate(4).putInt(
        testString.length + 1,
      ).array(),
      )
      allbytes.write(testString.toByteArray(StandardCharsets.UTF_8))
    }
    val encodedInputStream = allbytes.toByteArray().inputStream()

    val deframer = MessageDeframer()
    assertFalse(
      deframer.processInput(encodedInputStream, GrpcWebContentType.BINARY),
      "deframer should indicate `false` (malformed stream) for invalid frame segment length",
    )
    assertEquals(
      0,
      deframer.count,
      "count of frames from malformed stream should be `0`",
    )
  }

  // Well-Formed Cases: Single-frame

  @Test fun testDecodeSingleFrameBinary() {
    val data = "this is some test data"
    val binaryDeframer = MessageDeframer()
    val byteLength = data.toByteArray(StandardCharsets.UTF_8).size
    val binarySample = encodeAsStream(data)
    assertNotNull(binarySample, "should be able to prepare test stream (binary)")
    assertTrue(
      binaryDeframer.processInput(binarySample, GrpcWebContentType.BINARY),
      "message deframer should process single frame in one call (binary)",
    )
    val resultingBytesBinary = binaryDeframer.toByteArray()
    val decoded = String(resultingBytesBinary, StandardCharsets.UTF_8)
    assertEquals(
      data,
      decoded,
      "single frame should decode correctly back into test data",
    )
    assertEquals(
      1,
      binaryDeframer.count,
      "count of frames should indicate 1 with exactly 1 binary frame",
    )
    assertEquals(
      byteLength,
      binaryDeframer.size,
      "size should match size of byte stream provided to deframer",
    )
  }

  @Test fun testDecodeSingleFrameText() {
    val data = "this is some test data"
    val textDeframer = MessageDeframer()
    val byteLength = data.toByteArray(StandardCharsets.UTF_8).size
    val textSample = encodeAsStream(data, GrpcWebContentType.TEXT)
    assertNotNull(textSample, "should be able to prepare test stream (text)")
    assertTrue(
      textDeframer.processInput(textSample, GrpcWebContentType.TEXT),
      "message deframer should process single frame in one call (text)",
    )
    val resultingBytesText = textDeframer.toByteArray()
    val decoded = String(resultingBytesText, StandardCharsets.UTF_8)
    assertEquals(
      data,
      decoded,
      "single frame should decode correctly back into test data",
    )
    assertEquals(
      1,
      textDeframer.count,
      "count of frames should indicate 1 with exactly 1 binary frame",
    )
    assertEquals(
      byteLength,
      textDeframer.size,
      "size should match size of byte stream provided to deframer",
    )
  }

  @Test fun testDecodeSingleFrameEmpty() {
    val allbytes = ByteArrayOutputStream()
    allbytes.use {
      // write symbol
      allbytes.writeBytes(ByteArray(1) { RpcSymbol.DATA.value })
      allbytes.write(
        ByteBuffer.allocate(4).putInt(
        0,  // indicate empty data message
      ).array(),
      )
    }
    val encodedInputStream = allbytes.toByteArray().inputStream()

    val deframer = MessageDeframer()
    assertTrue(
      deframer.processInput(encodedInputStream, GrpcWebContentType.BINARY),
      "deframer should indicate `true` (well-formed stream) for empty data segment",
    )
    assertEquals(
      1,
      deframer.count,
      "count of frames for well-formed 1-segment (empty frame) stream should be 1",
    )
  }

  // Well-Formed Cases: Multi-frame

  @CsvSource(
    "BINARY,2", "BINARY,3", "BINARY,5", "BINARY,10", "BINARY,20",
    "TEXT,2", "TEXT,3", "TEXT,5", "TEXT,10", "TEXT,20",
  )
  @ParameterizedTest
  fun testMultiSegmentDeframe(modeName: String, frameCount: Int) {
    // create a sample set of `n` frames, for `modeName` format
    val format = GrpcWebContentType.valueOf(modeName)
    val testFrames = ArrayList<String>(frameCount)
    for (i in 0 until frameCount) {
      testFrames.add("this is a test frame (number $i)")
    }

    val allbytes = ByteArrayOutputStream()
    val joinedInputs = testFrames.joinToString("") { frame ->
      allbytes.writeBytes(encodeAsFrame(frame))  // deliberately encode without base64
      frame
    }
    val encodedBytes = if (format == GrpcWebContentType.TEXT)
      Base64.getEncoder().encode(allbytes.toByteArray())
    else allbytes.toByteArray()

    val encodedInputStream = encodedBytes.inputStream()

    if (format != GrpcWebContentType.TEXT) {
      assertEquals(
        joinedInputs.length + testFrames.size * 5,
        allbytes.size(),
        "size of all packed bytes should be the length of joined input strings, plus count*5",
      )
    }

    // with our test stream prepared, it's time to test the deframer
    val deframer = MessageDeframer()
    assertTrue(
      deframer.processInput(encodedInputStream, format),
      "deframer should indicate `true` (well-formed stream) for test stream of " +
      "(n: $frameCount, format: ${format.name})",
    )
    assertEquals(
      frameCount,
      deframer.count,
      "count of frames should indicate correct count of frames (expected: $frameCount, got: ${deframer.count})",
    )

    val output: ByteArray = deframer.toByteArray()
    assertEquals(
      joinedInputs,
      String(output),
      "re-joined output should match original input",
    )
  }
}

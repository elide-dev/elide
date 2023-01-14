package elide.rpc.server.web

import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Message
import io.grpc.health.v1.HealthGrpc
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertNotNull

/** Corner case tests for the [ReflectiveMessageDeserializer]. */
@MicronautTest
class ReflectiveMessageDeserializerTest: GrpcWebBaseTest() {
  fun emptyStub() {
    // this space left intentionally blank
    System.out.println("hello from an empty method")
  }

  fun wrongParameterStub(yoo: String) {
    // this space left intentionally blank
    System.out.println("hello from an empty method: $yoo")
  }

  @Suppress("unused", "UNUSED_PARAMETER") class BrokenParserReturnsNull {
    object BrokenProtoReturnsNull {
      @JvmStatic
      fun parseFrom(bytes: ByteArray): Message? {
        return null
      }
    }

    fun check(proto: BrokenProtoReturnsNull) {
      // nothing
    }
  }

  @Suppress("unused", "UNUSED_PARAMETER") class BrokenParserThrowsUnexpectedException {
    object BrokenParserThrows {
      @JvmStatic
      fun parseFrom(bytes: ByteArray): Message? {
        throw IllegalArgumentException("something went wrong")
      }
    }

    fun check(proto: BrokenParserThrows) {
      // nothing
    }
  }

  @Suppress("unused", "UNUSED_PARAMETER") class BrokenParserReturnsNonMessage {
    object BrokenParserInvalidReturn {
      @JvmStatic
      fun parseFrom(bytes: ByteArray): String {
        return "surprise!"
      }
    }

    fun check(proto: BrokenParserInvalidReturn) {
      // nothing
    }
  }

  @Test fun testConstruct() {
    assertDoesNotThrow {
      ReflectiveMessageDeserializer()
    }
  }

  @Test fun testResolveInvalidMethod() {
    val deserializer = ReflectiveMessageDeserializer()
    // prevent the empty stub from being compiled out
    emptyStub()
    wrongParameterStub("wrong param")
    assertThrows<IllegalStateException> {
      deserializer.deserialize(
        this.javaClass.methods.find { it.name == "emptyStub" }!!,
        ByteArray(0),
      )
    }
    assertThrows<IllegalStateException> {
      deserializer.deserialize(
        this.javaClass.methods.find { it.name == "wrongParameterStub" }!!,
        ByteArray(0),
      )
    }
  }

  @Test fun testParseInvalidProtocolBuffer() {
    val deserializer = ReflectiveMessageDeserializer()
    val stub = HealthGrpc.newStub(runtime.inProcessChannel())

    // decoding an empty byte array should be fine
    assertNotNull(
      assertDoesNotThrow {
        deserializer.deserialize(
          stub.javaClass.methods.find { it.name == "check" }!!,
          ByteArray(0),
        )
      },
      "empty byte array should decode as an empty message instance",
    )

    // fail via decoding junk bytes
    assertThrows<InvalidProtocolBufferException> {
      deserializer.deserialize(
        stub.javaClass.methods.find { it.name == "check" }!!,
        "oooohhhh sayyy cann youuuuu seeeee".toByteArray(StandardCharsets.UTF_8),
      )
    }
    assertThrows<InvalidProtocolBufferException> {
      deserializer.deserialize(
        stub.javaClass.methods.find { it.name == "check" }!!,
        "><LKMJNBHGVYFTRDY%$&^*(U)I{OPKLM>N <JBHVGFCTRDF^T&*Y(UPI)OPKL:<".toByteArray(StandardCharsets.UTF_8),
      )
    }
  }

  @Test fun testParserReturnsNull() {
    val deserializer = ReflectiveMessageDeserializer()
    assertThrows<IllegalStateException> {
      deserializer.deserialize(
        BrokenParserReturnsNull::class.java.methods.find { it.name == "check" }!!,
        ByteArray(0),
      )
    }
  }

  @Test fun testParserReturnsNonMessage() {
    val deserializer = ReflectiveMessageDeserializer()
    assertThrows<IllegalStateException> {
      deserializer.deserialize(
        BrokenParserThrowsUnexpectedException::class.java.methods.find { it.name == "check" }!!,
        ByteArray(0),
      )
    }
  }

  @Test fun testInternalParserError() {
    val deserializer = ReflectiveMessageDeserializer()
    assertThrows<IllegalStateException> {
      deserializer.deserialize(
        BrokenParserReturnsNonMessage::class.java.methods.find { it.name == "check" }!!,
        ByteArray(0),
      )
    }
  }
}

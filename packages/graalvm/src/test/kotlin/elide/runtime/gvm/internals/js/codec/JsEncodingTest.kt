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
@file:OptIn(DelicateElideApi::class)
@file:Suppress("JSUnresolvedReference", "LargeClass")

package elide.runtime.gvm.internals.js.codec

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.charset.StandardCharsets
import java.util.stream.Stream
import kotlinx.io.bytestring.encodeToByteString
import kotlin.streams.asStream
import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.intrinsics.js.codec.*
import elide.runtime.gvm.internals.intrinsics.js.codec.JsEncodingIntrinsics
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.err.ValueError
import elide.testing.annotations.TestCase

/** Tests for [TextEncoder] and [TextDecoder]. */
@TestCase internal class JsEncodingTest : AbstractJsIntrinsicTest<JsEncodingIntrinsics>() {
  @Inject lateinit var encoding: JsEncodingIntrinsics
  override fun provide(): JsEncodingIntrinsics = JsEncodingIntrinsics()

  override fun testInjectable() {
    assertNotNull(encoding, "should be able to inject `JsEncodingIntrinsics`")
  }

  // ----- TextEncoder

  @Test fun `TextEncoder should be present in guest context`() = executeGuest {
    // language=javascript
    """
      TextEncoder;
    """
  }.thenAssert {
    assertNotNull(it.returnValue(), "should be able to access `TextEncoder` in the guest context")
  }

  @Test fun `TextEncoder should be constructable in host context`() {
    assertNotNull(TextEncoder())
    assertNotNull(TextEncoder.Factory.create())
    assertNotNull(TextEncoder.Factory.create(null))
    assertThrows<TypeError> { TextEncoder.Factory.create(Value.asValue(42)) }
  }

  @Test fun `TextEncoder Factory as ProxyInstantiable`() {
    assertNotNull(TextEncoder.Factory.newInstance())
    assertNotNull(TextEncoder.Factory.newInstance(null))
    assertNotNull(TextEncoder.Factory.newInstance(Value.asValue("utf-8")))
    assertThrows<TypeError> { TextEncoder.Factory.newInstance(Value.asValue(42)) }
  }

  @Test fun `TextEncoder should be constructable in guest context`() = executeGuest {
    // language=javascript
    """
      const instance = new TextEncoder();
      test(instance).isNotNull();
      instance;
    """
  }.thenAssert {
    assertNotNull(it.returnValue(), "should be able to create `TextEncoder` in the guest context")
  }

  @Test fun `TextEncoder should always return utf-8 as encoding`() = dual {
    val encoder = TextEncoder.Factory.create()
    assertEquals("utf-8", encoder.encoding)
  }.guest {
    // language=javascript
    """
      const encoder = new TextEncoder();
      test(encoder.encoding).isEqualTo("utf-8");
    """
  }

  @Test fun `TextEncoder should properly encode host strings`(): Unit = with(TextEncoder()) {
    assertContentEquals(byteArrayOf(0x48, 0x65, 0x6c, 0x6c, 0x6f), encode("Hello"))
    subjectStrings.forEach {
      val ref = it.encodeToByteArray()
      assertContentEquals(ref, encode(it))

      val byteArray = ByteArray(ref.size)
      encodeInto(it, byteArray)
      assertContentEquals(ref, byteArray)

      val byteBuffer = ByteBuffer.wrap(ByteArray(it.toByteArray(StandardCharsets.UTF_8).size))
      encodeInto(it, byteBuffer)
      assertContentEquals(ref, byteBuffer.array())

      val stream = ByteArrayOutputStream()
      stream.use { buf -> encodeInto(it, buf) }
      assertContentEquals(ref, stream.toByteArray())

      val stream2 = ByteArrayOutputStream()
      val channel = Channels.newChannel(stream2)
      channel.use { chan -> encodeInto(it, chan) }
      assertContentEquals(ref, stream2.toByteArray())
    }
  }

  private val subjectStrings = arrayOf(
    "hello",
    "testing",
    "testing123",
    ":.,123abc",
    "👋",
    "👋👋",
    "👋👋👋",
    "",
    " ",
    "  ",
    "   ",
    "abcdefghijklmnopqrstuvwxyz",
    "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
    "1234567890",
    "!@#$%^&*()",
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%^&*()",
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%^&*()👋",
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%^&*()👋👋",
    "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%^&*()👋👋👋",
  )

  private fun String.truncate(length: Int): String =
    if (length >= this.length) this else this.substring(0, length) + "..."

  private fun cleanSubject(subject: String) = subject
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .truncate(20)

  private suspend fun SequenceScope<DynamicTest>.encoderTest(
    encoder: TextEncoder,
    subject: String,
    expected: ByteArray = subject.toByteArray(StandardCharsets.UTF_8),
  ) {
    yield(
      dynamicTest("host `TextEncoder` should encode '${cleanSubject(subject)}' to expected utf-8 bytes") {
        assertContentEquals(expected, encoder.encode(subject))
      },
    )
  }

  private suspend fun SequenceScope<DynamicTest>.guestEncoderTest(
    subject: String,
    expected: ByteArray = subject.toByteArray(StandardCharsets.UTF_8),
  ) {
    yield(
      dynamicTest("guest `TextEncoder` should encode '${cleanSubject(subject)}' to expected utf-8 bytes") {
        executeGuest {
          // language=javascript
          """
          const encoder = new TextEncoder();
          const subject = "$subject";
          const encoded = encoder.encode(subject);
          encoded;
        """
        }.thenAssert {
          assertNotNull(
            it.returnValue(),
            "should be able to encode '${cleanSubject(subject)}' in the guest context"
          )
          assertContentEquals(expected, it.returnValue()?.asHostObject<ByteArray>())
        }
      },
    )
    yield(
      dynamicTest("guest `TextEncoder` writes '${cleanSubject(subject)}' expected bytes (Uint8Array)") {
        executeGuest {
          // language=javascript
          """
          const encoder = new TextEncoder();
          const subject = "$subject";
          const target = new Uint8Array(encoder.encode(subject).length);
          encoder.encodeInto(subject, target);
          target;
        """
        }.thenAssert {
          val result = assertNotNull(
            it.returnValue(),
            "should be able to encode '${cleanSubject(subject)}' in the guest context"
          )
          val expectedBytes = TextEncoder().encode(subject)
          assertTrue(result.hasArrayElements())
          assertEquals(expectedBytes.size.toLong(), result.arraySize)
          expectedBytes.forEachIndexed { index, byte ->
            assertEquals(byte, result.getArrayElement(index.toLong()).asInt().toByte())
          }
        }
      },
    )
    yield(
      dynamicTest("guest `TextEncoder` writes '${cleanSubject(subject)}' expected bytes (plain array)") {
        executeGuest {
          // language=javascript
          """
          const encoder = new TextEncoder();
          const subject = "$subject";
          const target = [];
          encoder.encodeInto(subject, target);
          target;
        """
        }.thenAssert {
          val result = assertNotNull(
            it.returnValue(),
            "should be able to encode '${cleanSubject(subject)}' in the guest context"
          )
          val expectedBytes = TextEncoder().encode(subject)
          assertTrue(result.hasArrayElements())
          assertEquals(expectedBytes.size.toLong(), result.arraySize)
          expectedBytes.forEachIndexed { index, byte ->
            assertEquals(byte, result.getArrayElement(index.toLong()).asInt().toByte())
          }
        }
      },
    )
  }

  @TestFactory fun `TextEncoder host string tests`(): Stream<DynamicTest> = sequence {
    subjectStrings.forEach {
      encoderTest(TextEncoder(), it)
      encoderTest(TextEncoder(null), it)
      encoderTest(TextEncoder.Factory.create(), it)
      encoderTest(TextEncoder.Factory.create(Value.asValue(null)), it)
      encoderTest(TextEncoder.Factory.create(Value.asValue("utf-8")), it)
    }
  }.asStream()

  @TestFactory fun `TextEncoder guest string tests`(): Stream<DynamicTest> = sequence {
    subjectStrings.forEach {
      guestEncoderTest(it)
    }
  }.asStream()

  @Test @Ignore("Not yet compliant") fun `TextEncoder should pass guest instanceof checks`() = executeGuest {
    // language=javascript
    """
      const encoder = new TextEncoder();
      test(encoder instanceof TextEncoder).isTrue();
    """
  }.doesNotFail()

  @Test fun `TextEncoder encode should throw with invalid types`() {
    // invalid: no arguments
    assertThrows<TypeError> {
      val method = TextEncoder().getMember("encode") as ProxyExecutable
      method.execute()
    }
    // invalid: null subject
    assertThrows<TypeError> {
      val method = TextEncoder().getMember("encode") as ProxyExecutable
      method.execute(null)
    }
    // invalid: null subject as value
    assertThrows<TypeError> {
      val method = TextEncoder().getMember("encode") as ProxyExecutable
      method.execute(Value.asValue(null))
    }
    // invalid: invalid type as subject
    assertThrows<TypeError> {
      val method = TextEncoder().getMember("encode") as ProxyExecutable
      method.execute(Value.asValue(5.5))
    }
  }

  @Test fun `TextEncoder encodeInto should throw with invalid types`() {
    // invalid: no arguments
    assertThrows<TypeError> {
      val method = TextEncoder().getMember("encodeInto") as ProxyExecutable
      method.execute()
    }
    // invalid: bad buffer type
    assertThrows<TypeError> {
      val method = TextEncoder().getMember("encodeInto") as ProxyExecutable
      method.execute(Value.asValue("hi"), Value.asValue(42))
    }
    // invalid: too many arguments
    assertThrows<TypeError> {
      val method = TextEncoder().getMember("encodeInto") as ProxyExecutable
      method.execute(Value.asValue("hi"), Value.asValue(42), Value.asValue(42))
    }
    // invalid: subject is null
    assertThrows<TypeError> {
      val method = TextEncoder().getMember("encodeInto") as ProxyExecutable
      method.execute(null)
    }
    // invalid: subject is null as guest value
    assertThrows<TypeError> {
      val method = TextEncoder().getMember("encodeInto") as ProxyExecutable
      method.execute(Value.asValue(null))
    }
    // invalid: subject is valid, buffer is null
    assertThrows<TypeError> {
      val method = TextEncoder().getMember("encodeInto") as ProxyExecutable
      method.execute(Value.asValue("hi"), null)
    }
    // invalid: subject is valid, buffer is guest-side null
    assertThrows<TypeError> {
      val method = TextEncoder().getMember("encodeInto") as ProxyExecutable
      method.execute(Value.asValue("hi"), Value.asValue(null))
    }
    // invalid: buffer value is invalid, but proper arity
    assertThrows<TypeError> {
      val method = TextEncoder().getMember("encodeInto") as ProxyExecutable
      method.execute(Value.asValue(null), Value.asValue(42))
    }
    // invalid: subject is invalid type, buffer is invalid type
    assertThrows<TypeError> {
      val method = TextEncoder().getMember("encodeInto") as ProxyExecutable
      method.execute(Value.asValue(5), Value.asValue(42))
    }
  }

  private fun ProxyObject.hasNonNullProperty(name: String) {
    assertTrue(hasMember(name), "should have member '$name'")
    assertNotNull(getMember(name), "should have non-null member '$name'")
  }

  @Test fun `TextEncoder ProxyObject compliance`() {
    val encoder = TextEncoder()
    encoder.hasNonNullProperty("encoding")
    encoder.hasNonNullProperty("encode")
    encoder.hasNonNullProperty("encodeInto")
    assertNull(encoder.getMember("lol"), "should not have member 'lol'")
    assertFalse(encoder.hasMember("lol"))
    assertFalse(encoder.hasMember(null))
    assertFalse(encoder.removeMember("test"))
    assertTrue(encoder.memberKeys.isNotEmpty())
    assertTrue("encoding" in encoder.memberKeys)
    assertTrue("encode" in encoder.memberKeys)
    assertTrue("encodeInto" in encoder.memberKeys)
    assertDoesNotThrow {
      encoder.putMember("test", Value.asValue(42))
    }
  }

  @Test fun `TextEncoder encodeInto should check byte array capacity`() {
    val encoder = TextEncoder()
    val subject = "hello"
    val ref = subject.toByteArray(StandardCharsets.UTF_8)
    val byteArray = ByteArray(ref.size - 1)
    assertThrows<IndexOutOfBoundsException> { encoder.encodeInto(subject, byteArray) }
  }

  @Test fun `TextEncoder encodeInto should check byte buffer capacity`() {
    val encoder = TextEncoder()
    val subject = "hello"
    val ref = subject.toByteArray(StandardCharsets.UTF_8)
    val byteArray = ByteArray(ref.size - 1)
    assertThrows<IndexOutOfBoundsException> { encoder.encodeInto(subject, ByteBuffer.wrap(byteArray)) }
  }

  @Test fun `TextEncoder encodeInto should not close streams`() {
    val encoder = TextEncoder()
    val subject = "hello"
    val stream = ByteArrayOutputStream()
    stream.use { buf ->
      encoder.encodeInto(subject, buf)
      assertDoesNotThrow {
        buf.write(0)
      }
    }
  }

  @TestFactory fun `TextEncoder assigned encoding`(): Stream<DynamicTest> = sequence {
    SupportedJsEncoding.entries.forEach { supportedEncoding ->
      val label = supportedEncoding.symbol

      yield(dynamicTest("host - TextEncoder should accurately reflect assigned encoding '$label'") {
        val encoding = JsEncoding.resolve(label)
        val encoder = TextEncoder(encoding)
        assertEquals(encoding.symbol.symbol, encoder.encoding)
      })
      yield(dynamicTest("guest - TextEncoder should accurately reflect assigned encoding '$label'") {
        executeGuest {
          // language=javascript
          """
            const encoding = "$label";
            const encoder = new TextEncoder(encoding);
            test(encoder.encoding).isEqualTo(encoding);
          """
        }.doesNotFail()
      })
    }
  }.asStream()

  // ----- Encodings

  private val encodingNames = SupportedJsEncoding.entries.map { it.symbol }

  @TestFactory fun `known encodings should be resolvable`(): Stream<DynamicTest> = sequence {
    SupportedJsEncoding.entries.forEach { encoding ->
      yield(dynamicTest("host - should be able to resolve known encoding '${encoding.symbol}'") {
        assertNotNull(JsEncoding.resolve(encoding.symbol))
        assertNotNull(SupportedJsEncoding.resolve(encoding.symbol))
        val named = JsEncoding.resolve(encoding.symbol).symbol.symbol
        assertEquals(named, JsEncoding.resolve(encoding.symbol).symbol.symbol)
        assertEquals(
          named.compareTo(named),
          named.compareTo(named),
        )
        assertEquals(
          JsEncoding.resolve(encoding.symbol),
          JsEncoding.resolve(encoding.symbol),
        )
        assertEquals(
          0,
          JsEncoding.resolve(encoding.symbol).compareTo(JsEncoding.resolve(encoding.symbol)),
        )
        assertNotEquals(
          0,
          JsEncoding.resolve(encoding.symbol).compareTo(
            JsEncoding.resolve(requireNotNull(SupportedJsEncoding.entries.find {
              it.symbol != encoding.symbol
            }))
          ),
        )
        assertEquals(
          named[0],
          JsEncoding.resolve(encoding.symbol).symbol.symbol[0],
        )
        assertEquals(
          named[0],
          JsEncoding.resolve(encoding.symbol)[0],
        )
        assertNotEquals(
          0,
          named.compareTo("something else"),
        )
        assertNotEquals(
          0,
          JsEncoding.resolve(encoding.symbol).length,
        )
        assertTrue(
          JsEncoding.resolve(encoding.symbol).subSequence(0, 2).isNotEmpty(),
        )
        assertTrue(
          JsEncoding.resolve(encoding.symbol).subSequence(0, 2).isNotBlank(),
        )
      })
      yield(dynamicTest("host - create `TextDecoder` with known encoding '${encoding.symbol}'") {
        assertNotNull(TextDecoder(JsEncoding.resolve(encoding.symbol)))
      })
    }
  }.asStream()

  @Test fun `resolving unknown encoding should throw`() {
    assertThrows<Throwable> { JsEncoding.resolve("lol") }
    assertThrows<Throwable> { SupportedJsEncoding.resolve("lol") }
    assertThrows<Throwable> { TextDecoder("lol") }
    assertThrows<Throwable> { TextDecoder.Factory.create("lol") }
  }

  // ----- TextDecoder

  @Test fun `TextDecoder should be present in guest context`() = executeGuest {
    // language=javascript
    """
      TextDecoder;
    """
  }.thenAssert {
    assertNotNull(it.returnValue(), "should be able to access `TextDecoder` in the guest context")
  }

  @Test fun `TextDecoder should be constructable in host context`() {
    assertNotNull(TextDecoder())
    assertNotNull(TextDecoder("utf-8"))
    assertNotNull(TextDecoder("utf-8", TextDecoder.Options.defaults()))
    assertNotNull(TextDecoder.Factory.create())
    assertNotNull(TextDecoder.Factory.create("utf-8"))
    assertNotNull(TextDecoder.Factory.create("utf-8", TextDecoder.Options.defaults()))
    assertThrows<Throwable> {
      assertNotNull(TextDecoder("lol"))
      assertNotNull(TextDecoder.Factory.create("lol"))
    }
  }

  @Test fun `TextDecoder should be constructable in guest context`() = executeGuest {
    // language=javascript
    """
      const instance = new TextDecoder();
      test(instance).isNotNull();
      instance;
    """
  }.thenAssert {
    assertNotNull(it.returnValue(), "should be able to create `TextDecoder` in the guest context")
  }

  @TestFactory fun `TextDecoder should accept supported encodings`(): Stream<DynamicTest> = sequence {
    encodingNames.forEach {
      yield(dynamicTest("host - should be able to resolve known encoding at name '$it'") {
        assertNotNull(JsEncoding.resolve(it))
        assertNotNull(SupportedJsEncoding.resolve(it))
        val encoding = assertNotNull(JsEncoding.resolve(SupportedJsEncoding.resolve(it)))
        assertTrue(assertNotNull(encoding.symbol.symbol).isNotEmpty())
        assertTrue(assertNotNull(encoding.symbol.symbol).isNotBlank())
        val symbol = assertNotNull(assertNotNull(encoding.symbol).symbol)
        val charset = assertNotNull(encoding.charset)
        assertTrue(charset.isRegistered)
        assertTrue(charset.canEncode())
        assertNotEquals(0, symbol.length)
        assertEquals(symbol.subSequence(0, 1), symbol.subSequence(0, 1))
      })
      yield(dynamicTest("host - should be able to create `TextDecoder` with known encoding '$it'") {
        assertNotNull(TextDecoder(JsEncoding.resolve(it)))
      })
      yield(dynamicTest("host - should be able to create `TextDecoder` with encoding name '$it'") {
        assertNotNull(TextDecoder(it))
      })
      yield(dynamicTest("host - should be able to create `TextDecoder` via factory with encoding '$it'") {
        assertNotNull(TextDecoder.Factory.create(it))
      })
      yield(dynamicTest("guest - should be able to create `TextDecoder` with encoding '$it'") {
        executeGuest {
          // language=javascript
          """
            const instance = new TextDecoder("$it");
            test(instance).isNotNull();
            instance;
          """
        }.thenAssert { jsValue ->
          assertNotNull(
            jsValue.returnValue(),
            "should be able to create `TextDecoder` with encoding '$it' in the guest context",
          )
        }
      })
    }
  }.asStream()

  @Test fun `TextDecoder options should provide defaults`() {
    val options = elide.runtime.intrinsics.js.encoding.TextDecoder.Options.DEFAULTS
    assertEquals(false, options.fatal)
    assertEquals(false, options.ignoreBOM)
  }

  @Test fun `TextDecoder options should provide defaults via concrete`() {
    val options = TextDecoder.Options.defaults()
    assertEquals(false, options.fatal)
    assertEquals(false, options.ignoreBOM)
  }

  @Test fun `TextDecoder options should provide objects via concrete`() {
    val options = TextDecoder.Options.of(fatal = true, ignoreBOM = true)
    assertEquals(true, options.fatal)
    assertEquals(true, options.ignoreBOM)
    TextDecoder.Options.of().let {
      assertEquals(false, it.fatal)
      assertEquals(false, it.ignoreBOM)
    }
    TextDecoder.Options.of(fatal = true).let {
      assertEquals(true, it.fatal)
      assertEquals(false, it.ignoreBOM)
    }
    TextDecoder.Options.of(ignoreBOM = true).let {
      assertEquals(false, it.fatal)
      assertEquals(true, it.ignoreBOM)
    }
  }

  @Test fun `TextDecoder decode options should provide defaults`() {
    val options = elide.runtime.intrinsics.js.encoding.TextDecoder.DecodeOptions.DEFAULTS
    assertEquals(false, options.stream)
    TextDecoder.DecodeOptions.defaults().let {
      assertEquals(false, it.stream)
    }
    TextDecoder.DecodeOptions.of().let {
      assertEquals(false, it.stream)
    }
    TextDecoder.DecodeOptions.of(stream = true).let {
      assertEquals(true, it.stream)
    }
  }

  @Test @Ignore("Not yet compliant") fun `TextDecoder should pass guest instanceof checks`() = executeGuest {
    // language=javascript
    """
      const encoder = new TextDecoder();
      test(encoder instanceof TextDecoder).isTrue();
    """
  }.doesNotFail()

  @Test fun `TextDecoder should throw for out-of-range bytes`() = executeGuest {
    // language=javascript
    """
      const decoder = new TextDecoder();
      const bytes = [${Int.MAX_VALUE.toLong() + 1}];
      decoder.decode(bytes);
    """
  }.fails()

  @TestFactory fun `TextDecoder should properly decode utf-8 host values`(): Stream<DynamicTest> = sequence {
    val encoder = TextEncoder()
    val decoder = TextDecoder()

    subjectStrings.forEach {
      yield(dynamicTest("host - `TextDecoder` with host types") {
        assertEquals(it, decoder.decode(it.toByteArray(StandardCharsets.UTF_8)))
        assertEquals(it, decoder.decode(ByteBuffer.wrap(it.toByteArray(StandardCharsets.UTF_8))))
        assertEquals(it, decoder.decode(
          ByteBuffer.wrap(it.toByteArray(StandardCharsets.UTF_8)),
          TextDecoder.DecodeOptions.defaults()
        ))
        assertEquals(it, decoder.decode(ByteArrayInputStream(it.toByteArray(StandardCharsets.UTF_8))))
        Channels.newChannel(ByteArrayInputStream(it.toByteArray(StandardCharsets.UTF_8))).use { channel ->
          assertEquals(it, decoder.decode(channel))
        }
      })
      yield(dynamicTest("host - `TextDecoder` with '${cleanSubject(it)}'") {
        assertEquals(it, decoder.decode(encoder.encode(it)))
      })
      yield(dynamicTest("host - `TextDecoder` with '${cleanSubject(it)}' (reference)") {
        val ref = it.toByteArray(StandardCharsets.UTF_8)
        assertEquals(it, decoder.decode(ref))
      })
      yield(dynamicTest("guest - `TextDecoder` with '${cleanSubject(it)}'") {
        executeGuest {
          // language=javascript
          """
            const encoder = new TextEncoder();
            const decoder = new TextDecoder();
            const subject = "$it";
            const encoded = encoder.encode(subject);
            const decoded = decoder.decode(encoded);
            decoded;
          """
        }.thenAssert { result ->
          val str = assertNotNull(result.returnValue())
          assertFalse(str.isNull)
          assertTrue(str.isString)
          assertEquals(it, str.asString())
        }
      })
    }
  }.asStream()

  // ----- End-to-end

  @TestFactory fun `js encoding e2e`(): Stream<DynamicTest> = sequence {
    for (knownEncoding in SupportedJsEncoding.entries) {
      val encoding = assertNotNull(JsEncoding.resolve(knownEncoding))
      val encoder = TextEncoder(encoding)
      val name = assertNotNull(assertNotNull(encoding.symbol).symbol)
      val decoder = TextDecoder(name)

      for (subject in subjectStrings) {
        val testSubject = cleanSubject(subject)
        val reference = subject.toByteArray(encoding.charset)

        yield(dynamicTest("e2e host - should be able to encode and decode with '$name': '${testSubject}'") {
          assertContentEquals(reference, encoder.encode(subject))
          assertEquals(decoder.decode(encoder.encode(subject)), decoder.decode(reference))
        })

        yield(dynamicTest("e2e guest - should be able to decode with '$name': '${testSubject}'") {
          val arrData = reference.joinToString(",")

          executeGuest {
            // language=javascript
            """
              const decoder = new TextDecoder("$name");
              const subject = new Uint8Array([${arrData}]);
              const decoded = decoder.decode(subject);
              decoded;
            """
          }.thenAssert { result ->
            val str = assertNotNull(result.returnValue())
            assertFalse(str.isNull)
            assertTrue(str.isString)
            assertEquals(
              // encode as byte array in target encoding, then build as string; this accounts for lossy conversion of
              // high-range symbols in encodings that do not support it
              String(subject.encodeToByteString(encoding.charset).toByteArray(), encoding.charset),
              str.asString(),
            )
          }
        })
      }
    }
  }.asStream()

  @TestFactory fun `TextDecoder assigned encoding`(): Stream<DynamicTest> = sequence {
    SupportedJsEncoding.entries.forEach { supportedEncoding ->
      val label = supportedEncoding.symbol

      yield(dynamicTest("host - TextDecoder should accurately reflect assigned encoding '$label'") {
        val encoding = JsEncoding.resolve(label)
        val decoder = TextDecoder(encoding)
        assertEquals(encoding.symbol.symbol, decoder.encoding)
      })
      yield(dynamicTest("guest - TextDecoder should accurately reflect assigned encoding '$label'") {
        executeGuest {
          // language=javascript
          """
            const encoding = "$label";
            const decoder = new TextDecoder(encoding);
            test(decoder.encoding).isEqualTo(encoding);
          """
        }.doesNotFail()
      })
    }
  }.asStream()

  @Test fun `TextDecoder ProxyObject compliance`() {
    val encoder = TextDecoder()
    encoder.hasNonNullProperty("encoding")
    encoder.hasNonNullProperty("decode")
    assertNull(encoder.getMember("lol"), "should not have member 'lol'")
    assertFalse(encoder.hasMember("lol"))
    assertFalse(encoder.hasMember(null))
    assertFalse(encoder.removeMember("test"))
    assertTrue(encoder.memberKeys.isNotEmpty())
    assertTrue("encoding" in encoder.memberKeys)
    assertTrue("decode" in encoder.memberKeys)
    assertDoesNotThrow {
      encoder.putMember("test", Value.asValue(42))
    }
  }

  @Test fun `TextDecoder decode should throw with invalid types`() {
    // invalid: bad subject as null
    assertThrows<ValueError> {
      val method = TextDecoder().getMember("decode") as ProxyExecutable
      method.execute(null)
    }
    // invalid: bad subject as guest null
    assertThrows<ValueError> {
      val method = TextDecoder().getMember("decode") as ProxyExecutable
      method.execute(Value.asValue(null))
    }
    // invalid: bad subject as bad type
    assertThrows<ValueError> {
      val method = TextDecoder().getMember("decode") as ProxyExecutable
      method.execute(Value.asValue(42))
    }
    // invalid: too many arguments
    assertThrows<TypeError> {
      val method = TextDecoder().getMember("decode") as ProxyExecutable
      method.execute(Value.asValue("hi"), Value.asValue(42), Value.asValue(1.5))
    }
    // invalid: bad options argument
    assertThrows<ValueError> {
      val method = TextDecoder().getMember("decode") as ProxyExecutable
      method.execute(Value.asValue("hi"), Value.asValue(42))
    }
    // invalid: bad subject as null with bad options
    assertThrows<ValueError> {
      val method = TextDecoder().getMember("decode") as ProxyExecutable
      method.execute(null, Value.asValue(42))
    }
    // invalid: bad subject as guest null
    assertThrows<ValueError> {
      val method = TextDecoder().getMember("decode") as ProxyExecutable
      method.execute(Value.asValue(null), Value.asValue(42))
    }
    // invalid: bad options as null
    assertThrows<ValueError> {
      val method = TextDecoder().getMember("decode") as ProxyExecutable
      method.execute(Value.asValue("hi"), null)
    }
    // invalid: bad options as guest null
    assertThrows<ValueError> {
      val method = TextDecoder().getMember("decode") as ProxyExecutable
      method.execute(Value.asValue("hi"), Value.asValue(null))
    }
    // invalid: bad options as bad type
    assertThrows<ValueError> {
      val method = TextDecoder().getMember("decode") as ProxyExecutable
      method.execute(Value.asValue("hi"), Value.asValue(42))
    }
    // valid: empty arguments
    assertDoesNotThrow {
      val method = TextDecoder().getMember("decode") as ProxyExecutable
      method.execute()
    }
    // valid: empty arguments (host)
    assertDoesNotThrow {
      TextDecoder().decode()
      assertEquals("", TextDecoder().decode())
    }
    // valid: empty arguments (guest)
    executeGuest {
      // language=javascript
      """
        const decoder = new TextDecoder();
        const result = decoder.decode();
        test(result).isEqualTo("");
      """
    }
  }

  @Test fun `TextDecoder Factory as ProxyInstantiable`() {
    assertNotNull(TextDecoder.Factory.newInstance())
    assertNotNull(TextDecoder.Factory.newInstance(null))
    assertNotNull(TextDecoder.Factory.newInstance(Value.asValue("utf-8")))
    assertThrows<TypeError> { TextEncoder.Factory.newInstance(Value.asValue(42)) }
    assertThrows<TypeError> {
      TextEncoder.Factory.newInstance(Value.asValue(42), Value.asValue(42), Value.asValue(42))
    }
    assertThrows<TypeError> {
      TextEncoder.Factory.newInstance(Value.asValue("utf-8"), Value.asValue(42), Value.asValue(42))
    }
    assertNotNull(TextDecoder.Factory.newInstance(Value.asValue("utf-8"), Value.asValue(null)))
    assertNotNull(TextDecoder.Factory.newInstance(Value.asValue("utf-8"), Value.asValue(
      TextDecoder.Options.of(fatal = true, ignoreBOM = true)
    )))
    assertNotNull(TextDecoder.Factory.newInstance(Value.asValue("utf-8"), Value.asValue(
      object: ProxyObject {
        override fun hasMember(key: String): Boolean = key == "fatal" || key == "ignoreBOM"
        override fun getMember(key: String): Value? = when (key) {
          "fatal" -> Value.asValue(true)
          "ignoreBOM" -> Value.asValue(true)
          else -> null
        }

        override fun getMemberKeys(): Array<String> = arrayOf("fatal", "ignoreBOM")
        override fun putMember(key: String, value: Value) = Unit
        override fun removeMember(key: String): Boolean = false
      }
    )))
  }
}

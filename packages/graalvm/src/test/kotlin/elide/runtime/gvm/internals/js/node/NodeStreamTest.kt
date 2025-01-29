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
package elide.runtime.gvm.internals.js.node

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.node.stream.NodeStreamModule
import elide.runtime.node.NodeModuleConformanceTest
import elide.runtime.intrinsics.js.node.stream.Readable
import elide.runtime.intrinsics.js.node.stream.Writable
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `stream` built-in module. */
@TestCase internal class NodeStreamTest : NodeModuleConformanceTest<NodeStreamModule>() {
  override val moduleName: String get() = "stream"
  override fun provide(): NodeStreamModule = NodeStreamModule()
  @Inject lateinit var stream: NodeStreamModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("Readable")
    yield("Duplex")
    yield("Transform")
    yield("finished")
    yield("pipeline")
    yield("compose")
    yield("destroy")
    yield("isDisturbed")
    yield("isErrored")
    yield("isReadable")
    yield("Writable")
    yield("Stream")
    yield("addAbortSignal")
    yield("getDefaultHighWaterMark")
    yield("setDefaultHighWaterMark")
    yield("promises")
    yield("web")
    yield("PassThrough")
  }

  @Test override fun testInjectable() {
    assertNotNull(stream)
  }

  @Test fun `Readable - consume data as raw bytes`() {
    val bytes = "hello".toByteArray(StandardCharsets.UTF_8)
    Readable.wrap(ByteArrayInputStream(bytes)).let {
      assertNotNull(it, "should not get `null` from `Readable.wrap`")
      assertTrue(it.readable, "`readable` should report as `true` initially")
      assertEquals(null, it.readableFlowing, "`readable` should not be flowing initially")
      val data = it.read()
      assertIs<ByteArray>(data, "should get byte array back from raw-bytes readable")
      val str = String(data, StandardCharsets.UTF_8)
      assertEquals("hello", str, "decoded string should be correct value from `readable`")
    }
  }

  @Test fun `Readable - consume data as raw bytes with explicit size`() {
    val bytes = "hello".toByteArray(StandardCharsets.UTF_8)
    Readable.wrap(ByteArrayInputStream(bytes)).let {
      assertNotNull(it, "should not get `null` from `Readable.wrap`")
      assertTrue(it.readable, "`readable` should report as `true` initially")
      assertEquals(null, it.readableFlowing, "`readable` should not be flowing initially")
      val data = it.read(4)
      assertIs<ByteArray>(data, "should get byte array back from raw-bytes readable")
      val str = String(data, StandardCharsets.UTF_8)
      assertEquals("hell", str, "decoded string should be correct value from `readable`")
    }
  }

  @Test fun `Readable - consume data as an encoded string`() {
    val bytes = "hello".toByteArray(StandardCharsets.UTF_8)
    Readable.wrap(ByteArrayInputStream(bytes), StandardCharsets.UTF_8).let {
      assertNotNull(it, "should not get `null` from `Readable.wrap`")
      assertTrue(it.readable, "`readable` should report as `true` initially")
      assertEquals(null, it.readableFlowing, "`readable` should not be flowing initially")
      val data = it.read()
      assertIs<String>(data, "should get byte array back from raw-bytes readable")
      assertEquals("hello", data, "decoded string should be correct value from `readable`")
    }
  }

  @Test fun `Readable - consume data as an encoded string with explicit size`() {
    val bytes = "hello".toByteArray(StandardCharsets.UTF_8)
    Readable.wrap(ByteArrayInputStream(bytes), StandardCharsets.UTF_8).let {
      assertNotNull(it, "should not get `null` from `Readable.wrap`")
      assertTrue(it.readable, "`readable` should report as `true` initially")
      assertEquals(null, it.readableFlowing, "`readable` should not be flowing initially")
      val data = it.read(4)
      assertIs<String>(data, "should get byte array back from raw-bytes readable")
      assertEquals("hell", data, "decoded string should be correct value from `readable`")
    }
  }

  @Test fun `Readable - consume data as events`() {
    val bytes = "hello".toByteArray(StandardCharsets.UTF_8)
    Readable.wrap(ByteArrayInputStream(bytes), StandardCharsets.UTF_8).let {
      assertNotNull(it, "should not get `null` from `Readable.wrap`")
      assertTrue(it.readable, "`readable` should report as `true` initially")
      assertEquals(null, it.readableFlowing, "`readable` should not be flowing initially")
      val buffer = StringBuilder()
      var didEnd = false
      var didSeeData = false
      it.addEventListener("end") {
        didEnd = true
      }
      it.addEventListener("data") { args ->
        didSeeData = true
        buffer.append(args.first() as String)
      }
      assertTrue(didSeeData, "should have seen `data` event")
      assertTrue(didEnd, "should have seen `end` event")
      assertEquals("hello", buffer.toString(), "should have consumed all data from `Readable`")
    }
  }

  @Test fun `Writable - write data as raw bytes`() {
    val bytes = "hello".toByteArray(StandardCharsets.UTF_8)
    val out = ByteArrayOutputStream()
    Writable.wrap(out).let {
      assertNotNull(it, "should not get `null` from `Writable.wrap`")
      assertTrue(it.writable, "`writable` should report as `true` initially")
      it.write(bytes)
      it.end()
      assertEquals("hello", out.toString(StandardCharsets.UTF_8), "should have written to `Writable`")
      assertFalse(it.writable)
      assertTrue(it.writableEnded)
    }
  }
}

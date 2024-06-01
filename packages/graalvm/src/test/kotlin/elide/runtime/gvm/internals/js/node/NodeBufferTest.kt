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
package elide.runtime.gvm.internals.js.node

import org.graalvm.polyglot.Value
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.*
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.node.buffer.NodeBlob
import elide.runtime.gvm.internals.node.buffer.NodeBufferModule
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.runtime.intrinsics.js.node.BufferAPI
import elide.runtime.intrinsics.js.node.buffer.Buffer
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for [Buffer]. */
@DelicateElideApi
@TestCase internal class NodeBufferTest : NodeModuleConformanceTest<NodeBufferModule>() {
  @Inject internal lateinit var buffer: BufferAPI
  @Inject internal lateinit var module: NodeBufferModule

  override val moduleName: String get() = "buffer"
  override fun provide(): NodeBufferModule = module

  override fun requiredMembers(): Sequence<String> = sequenceOf(
    "constants",
    "kMaxLength",
    "kStringMaxLength",
    "resolveObjectURL",
    "transcode",
    "isUtf8",
    "isAscii",
    "atob",
    "btoa",
    "Buffer",
    "Blob",
    "File",
    "SlowBuffer",
  )

  @Test override fun testInjectable() {
    assertNotNull(buffer)
  }

  @Test fun `atob compliance`() {
    val original = "Hello, world"
    val encoded = Base64.getEncoder().encodeToString(original.toByteArray(Charsets.ISO_8859_1))

    conforms {
      assertEquals(original, buffer.atob(Value.asValue(encoded)), "expected 'atob' to correctly decode message")
    }.guest {
      //language=javascript
      """
      const { atob } = require("buffer");
      const assert = require("assert");

      assert.equal("$original", atob("$encoded"), "expected 'atob' to decode message in guest context")
      """
    }
  }

  @Test fun `btoa compliance`() {
    val original = "Hello, world"
    val encoded = Base64.getEncoder().encodeToString(original.toByteArray(Charsets.ISO_8859_1))

    conforms {
      assertEquals(encoded, buffer.btoa(Value.asValue(original)), "expected 'btoa' to correctly decode message")
    }.guest {
      //language=javascript
      """
      const { btoa } = require("buffer");
      const assert = require("assert");

      assert.equal("$encoded", btoa("$original"), "expected 'btoa' to decode message in guest context")
      """
    }
  }

  @Test fun `isAscii() compliance`() = conforms {
    // requires guest types unavailable in the host
  }.guest {
    // language=javascript
    """
    const { isAscii } = require("buffer");
    const assert = require("assert");

    const validAscii = Buffer.from([0x68,0x65,0x6c,0x6c,0x6f]) // hello
    assert.equal(true, isAscii(validAscii));

    const invalidAscii = Buffer.from([0x68,0x65,0x6C,0x6C,0x6F,0x20,0xF0,0x9F,0x99,0x82]) // hello🙂
    assert.equal(false, isAscii(invalidAscii));
    """
  }

  @Test fun `isUtf8() compliance`() = conforms {
    // requires guest types unavailable in the host
  }.guest {
    // language=javascript
    """
    const { isUtf8 } = require("buffer");
    const assert = require("assert");

    const validUtf8 = Buffer.from([0x22,0x48,0x65,0x6c,0x6c,0x6f,0x21,0xf0,0x9f,0x99,0x82,0x22]) // Hello!🙂
    assert.equal(true, isUtf8(validUtf8));

    const invalidUtf8 = Buffer.from([0x4f,0x6f,0x70,0x73,0x21,0xc3,0xb0,0xc5,0xb8,0xc5]) // Oops!ðŸŒ
    assert.equal(false, isUtf8(invalidUtf8));
    """
  }

  @Ignore @Test fun `transcode() compliance`() = conforms {
    // tests require guest types unavailable in the host
  }.guest {
    // language=javascript
    """
    const { transcode } = require("buffer");
    const assert = require("assert");

    const encoder = new TextEncoder();

    // utf-8 to ascii
    assert.equal("?", transcode(encoder.encode("€"), "utf8", "ascii").toString("ascii"))
    assert.equal("hello!", transcode(encoder.encode("hello!"), "utf8", "ascii").toString("ascii"))

    // ascii to utf-8
    assert.equal("hello!", transcode(encoder.encode("hello!"), "ascii", "utf8").toString("utf8"))
    """
  }

  @Test fun `Blob compliance`() = conforms {
    // empty constructor
    assertDoesNotThrow("expected empty constructor not to throw") { NodeBlob() }
  }.guest {
    // TODO(@darvld): test for `stream()` once ReadableStream is implemented
    // TODO(@darvld): fix `arrayBuffer()` once we can create ArrayBuffer instances host-side
    // language=javascript
    """
    const { Blob } = require("buffer");
    const assert = require("assert");

    // empty
    new Blob();

    // mixed sources only
    new Blob(["hello", new ArrayBuffer(" world"), new Int8Array(8)]);

    // mixed sources and options
    const blob = new Blob(["hello", new ArrayBuffer(1), new Int8Array(1)], {
      type: "test",
      endings: "transparent"
    });

    // -- member tests --

    // type
    assert.equal("test", blob.type);

    // size
    assert.equal(7, blob.size);

    // text
    blob.text().then(t => assert.equal("hello\x00\x00", t));

    // slice
    const slice = blob.slice(0, 5, "new-type");
    assert.equal("new-type", slice.type);
    assert.equal(5, slice.size);
    slice.text().then(t => assert.equal("hello", t));

    // arrayBuffer
    // blob.arrayBuffer().then(buf => {
    //   const decoder = new TextDecoder();
    //   assert.equals("hello", decoder.decode(buf));
    // });
    """
  }

  @Test fun `File compliance`() = conforms {
    // all tests require guest code
  }.guest {
    // language=javascript
    """
    const { File } = require("buffer");
    const assert = require("assert");

    // basic
    new File(["hello"], "file.txt");

    // mixed sources
    new File(["hello", new ArrayBuffer(" world"), new Int8Array(8)], "file.txt");

    // mixed sources and options
    const file = new File(["hello", new ArrayBuffer(1), new Int8Array(1)], "file.txt", {
      type: "test",
      endings: "transparent",
      lastModified: 42
    });

    // -- unique member tests --
    assert.equal("file.txt", file.name);
    assert.equal(42, file.lastModified);

    // -- inherited member tests --

    // type
    assert.equal("test", file.type);

    // size
    assert.equal(7, file.size);

    // text
    file.text().then(t => assert.equal("hello\x00\x00", t));

    // slice
    const slice = file.slice(0, 5, "new-type");
    assert.equal("new-type", slice.type);
    assert.equal(5, slice.size);
    slice.text().then(t => assert.equal("hello", t));

    // arrayBuffer
    // file.arrayBuffer().then(buf => {
    //   const decoder = new TextDecoder();
    //   assert.equals("hello", decoder.decode(buf));
    // });
    """
  }
}

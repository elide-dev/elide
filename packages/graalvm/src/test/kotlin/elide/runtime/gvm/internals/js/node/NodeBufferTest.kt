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

  @Test fun `isAscii() compliance (valid)`() = conforms {
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

  @Test fun `isAscii() compliance (invalid)`() = conforms {
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

  @Ignore("Broken at PR-1034") @Test fun `isUtf8() compliance (valid)`() = conforms {
    // requires guest types unavailable in the host
  }.guest {
    // language=javascript
    """
    const { isUtf8 } = require("buffer");
    const assert = require("assert");

    const validUtf8 = Buffer.from([0x22,0x48,0x65,0x6c,0x6c,0x6f,0x21,0xf0,0x9f,0x99,0x82,0x22]) // Hello!🙂
    assert.equal(true, isUtf8(validUtf8));
    """
  }

  @Test fun `isUtf8() compliance (invalid)`() = conforms {
    // requires guest types unavailable in the host
  }.guest {
    // language=javascript
    """
    const { isUtf8 } = require("buffer");
    const assert = require("assert");

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

  @Ignore("Broken at PR-1034") @Test fun `File compliance`() = conforms {
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

  @Test fun `Buffer allocation`() = conforms {
    // coming soon
  }.guest {
    // language=javascript
    """
    const assert = require("node:assert");
    const buffer = require("node:buffer");

    // all 5 bytes should be 0
    let buf = buffer.Buffer.alloc(5);
    assert.equal(5, buf.length);
    for (let i = 0; i < 5; i++) { assert.equal(buf[i], 0); }

    // fill with single byte
    buf = buffer.Buffer.alloc(5, 5);
    for (let i = 0; i < 5; i++) { assert.equal(buf[i], 5); }

    // fill with coerced value (overflow)
    buf = buffer.Buffer.alloc(5, 256);
    for (let i = 0; i < 5; i++) { assert.equal(buf[i], 0); }

    // fill with coerced value (underflow)
    buf = buffer.Buffer.alloc(5, -1);
    for (let i = 0; i < 5; i++) { assert.equal(buf[i], 255); }

    // fill with string (fit)
    buf = buffer.Buffer.alloc(4, "abcd");
    for (let i = 0; i < 4; i++) { assert.equal(buf[i], 97 + i); }

    // fill with string (looped)
    buf = buffer.Buffer.alloc(8, "abcd");
    for (let i = 0; i < 8; i++) { assert.equal(buf[i], 97 + i % 4); }

    // sample array data
    const arrayData = new Uint8Array([-1, 0, 256, 512]);

    // fill with Uint8Array (fit)
    buf = buffer.Buffer.alloc(4, arrayData);
    for (let i = 0; i < 4; i++) { assert.equal(buf[i], arrayData[i]); }

    // fill with Uint8Array (looped)
    buf = buffer.Buffer.alloc(8, arrayData);
    for (let i = 0; i < 8; i++) { assert.equal(buf[i], arrayData[i % 4]); }

    // sample buffer data
    const bufferData = buffer.Buffer.alloc(4, arrayData);

    // fill with Buffer (fit)
    buf = buffer.Buffer.alloc(8, bufferData);
    for (let i = 0; i < 8; i++) { assert.equal(buf[i], bufferData[i % 4]); }

    // fill with Buffer (looped)
    buf = buffer.Buffer.alloc(8, bufferData);
    for (let i = 0; i < 8; i++) { assert.equal(buf[i], bufferData[i % 4]); }

    // alloc variants
    buf = buffer.Buffer.allocUnsafe(5);
    assert.equal(5, buf.length);

    buf = buffer.Buffer.allocUnsafeSlow(5);
    assert.equal(5, buf.length);
    """
  }

  @Test fun `Buffer (static members)`() = conforms {
    // coming soon
  }.guest {
    // language=javascript
    """
    const assert = require("node:assert");
    const { Buffer} = require("node:buffer");

    // helper for assertions
    function assertBufferContents(buffer, contents) {
      assert.equal(
        buffer.length, contents.length,
        "expected buffer length to be " + contents.length + " but was " + buffer.length
      );

      for (let i = 0; i < buffer.length; i++) {
        assert.equal(
          buffer[i],
          contents[i],
          "expected buffer value at " + i + " to be " + contents[i] + " but was " + buffer[i]
        );
      }
    }

    // byteLength (default encoding)
    assert.equal(Buffer.byteLength("Hello!🙂"), 10);

    // byteLength (explicit encoding)
    assert.equal(Buffer.byteLength("Hello ASCII", "ascii"), 11);

    // compare (Buffer/Buffer)
    assert.equal(Buffer.compare(Buffer.alloc(5, 1), Buffer.alloc(5, 1)), 0);
    assert.equal(Buffer.compare(Buffer.alloc(5, 1), Buffer.alloc(5, 0)), 1);
    assert.equal(Buffer.compare(Buffer.alloc(5, 1), Buffer.alloc(2, 1)), 1);
    assert.equal(Buffer.compare(Buffer.alloc(1, 1), Buffer.alloc(2, 1)), -1);

    // compare (Buffer/Uint8Array)
    assert.equal(Buffer.compare(Buffer.alloc(2, 1), new Uint8Array([1, 1])), 0);
    assert.equal(Buffer.compare(Buffer.alloc(2, 1), new Uint8Array([1, 0])), 1);
    assert.equal(Buffer.compare(Buffer.alloc(2, 1), new Uint8Array([1])), 1);
    assert.equal(Buffer.compare(Buffer.alloc(2, 1), new Uint8Array([3, 4])), -1);

    // compare (Uint8Array/Uint8Array)
    assert.equal(Buffer.compare(new Uint8Array([5, 1]), new Uint8Array([5, 1])), 0);
    assert.equal(Buffer.compare(new Uint8Array([5, 1]), new Uint8Array([5, 0])), 1);
    assert.equal(Buffer.compare(new Uint8Array([5, 1]), new Uint8Array([2])), 1);
    assert.equal(Buffer.compare(new Uint8Array([1, 1]), new Uint8Array([2, 1])), -1);

    // concat (computed length)
    assertBufferContents(
      Buffer.concat([Buffer.alloc(2, new Uint8Array([1, 2])), new Uint8Array([3, 4])]),
      [1, 2, 3, 4]
    );

    // concat (explicit length)
    assertBufferContents(
      Buffer.concat([Buffer.alloc(2, new Uint8Array([5, 6])), new Uint8Array([7, 8])], 4),
      [5, 6, 7, 8]
    );

    // copyBytesFrom
    assertBufferContents(
      Buffer.copyBytesFrom(new Uint8Array([1, 2, 3, 4]), 2, 2),
      [3, 4]
    );

    // from(Array)
    assertBufferContents(
      Buffer.from([1, 2, 3 ,4]),
      [1, 2, 3, 4]
    );

    // from(ArrayBuffer)
    const srcArray = new Uint8Array([1, 2, 3, 4]);
    assertBufferContents(
      Buffer.from(srcArray.buffer, srcArray.byteOffset + 2, 1),
      [3]
    );

    // from(Buffer)
    assertBufferContents(
      Buffer.from(Buffer.from([1, 2, 3, 4])),
      [1, 2, 3, 4]
    );

    // from(Uint8Array)
    assertBufferContents(
      Buffer.from(new Uint8Array([1, 2, 3, 4])),
      [1, 2, 3, 4]
    );

    // from(Object)
    // not supported

    // from(String)
    assertBufferContents(
      Buffer.from("Hello! 🙂"),
      [72, 101, 108, 108, 111, 33, 32, 240, 159, 153, 130]
    );

    // isBuffer
    assert.equal(Buffer.isBuffer(Buffer.alloc(5)), true)
    assert.equal(Buffer.isBuffer(new Uint8Array()), false)
    assert.equal(Buffer.isBuffer(new ArrayBuffer(8)), false)
    assert.equal(Buffer.isBuffer(5), false)
    assert.equal(Buffer.isBuffer("hello"), false)
    assert.equal(Buffer.isBuffer({}), false)

    // isEncoding
    assert.equal(Buffer.isEncoding(""), false)
    assert.equal(Buffer.isEncoding("hello"), false)
    assert.equal(Buffer.isEncoding("utf-16be"), false)

    const validEncodings = [
      "utf8", "UtF8", "utf-8", "UtF-8",
      "utf16le", "UtF16lE", "utf-16le", "UtF-16lE",
      "latin1", "base64", "base64url", "hex"
    ];

    for (const enc of validEncodings) {
      assert.equal(Buffer.isEncoding(enc), true, "expected encoding " + enc + " to be supported")
    }
    """
  }

  @Test fun `Buffer (instance members)`() = conforms {

  }.guest {
    """
    const assert = require("node:assert");
    const { Buffer} = require("node:buffer");

    // helper for assertions
    function assertContents(actual, expected) {
      assert.equal(
        actual.length, expected.length,
        "expected length to be " + expected.length + " but was " + actual.length
      );

      for (let i = 0; i < actual.length; i++) {
        assert.equal(
          actual[i],
          expected[i],
          "expected value at " + i + " to be " + expected[i] + " but was " + actual[i]
        );
      }
    }

    // copy (Buffer)
    let target = Buffer.alloc(4);
    Buffer.from([1, 2, 3, 4]).copy(target, 2, 1, 2);
    assertContents(target, [0, 0, 2, 0]);

    // copy (Uint8Array)
    target = new Uint8Array(4);
    Buffer.from([1, 2, 3, 4]).copy(target, 2, 1, 2);
    assertContents(target, [0, 0, 2, 0]);

    // entries
    // TODO(@darvld): remove workaround once assert.deepEqual is implemented
    const entries = [...Buffer.from([1, 2, 3, 4]).entries()];
    for (let i = 0; i < 4; i++) {
      assert.equal(entries[i][0], i);
      assert.equal(entries[i][1], i + 1);
    }

    // keys
    assertContents(
      [...Buffer.from([1, 2, 3, 4]).keys()],
      [0, 1, 2, 3],
    );

    // values
    assertContents(
      [...Buffer.from([1, 2, 3, 4]).values()],
      [1, 2, 3, 4],
    );

    // includes
    let buf = Buffer.from("abcd"); // Utf-8 [97, 98, 99, 100]

    assert.equal(buf.includes("bc", 1), true, "should include string");
    assert.equal(buf.includes("e", 2), false, "should not include string");

    assert.equal(buf.includes(98, 1), true, "should include number");
    assert.equal(buf.includes(1, 2), false, "should not include number");

    assert.equal(buf.includes(Buffer.from("bc"), 1), true, "should include buffer");
    assert.equal(buf.includes(Buffer.from("e"), 2), false, "should not include buffer");

    assert.equal(buf.includes(new Uint8Array([98, 99]), 1), true, "should include Uint8Array");
    assert.equal(buf.includes(new Uint8Array([1]), 2), false, "should not include Uint8Array");

    // indexOf
    buf = Buffer.from("abcddcba"); // Utf-8 [97, 98, 99, 100, 100, 99, 98, 97]
    assert.equal(buf.indexOf("c"), 2);
    assert.equal(buf.indexOf("dd"), 3);
    assert.equal(buf.indexOf("ba"), 6);
    assert.equal(buf.indexOf("z"), -1);

    assert.equal(buf.indexOf(Buffer.from("c")), 2);
    assert.equal(buf.indexOf(Buffer.from("dd")), 3);
    assert.equal(buf.indexOf(Buffer.from("ba")), 6);
    assert.equal(buf.indexOf(Buffer.from("z")), -1);

    assert.equal(buf.indexOf(new Uint8Array([99])), 2);
    assert.equal(buf.indexOf(new Uint8Array([100, 100])), 3);
    assert.equal(buf.indexOf(new Uint8Array([98, 97])), 6);
    assert.equal(buf.indexOf(new Uint8Array([1])), -1);
    assert.equal(buf.indexOf(new Uint8Array(20)), -1);

    assert.equal(buf.indexOf(98), 1);
    assert.equal(buf.indexOf(1), -1);

    // lastIndexOf
    assert.equal(buf.lastIndexOf("c"), 5);
    assert.equal(buf.lastIndexOf("dd"), 3);
    assert.equal(buf.lastIndexOf("ab"), 0);
    assert.equal(buf.lastIndexOf("z"), -1);

    assert.equal(buf.lastIndexOf(Buffer.from("c")), 5);
    assert.equal(buf.lastIndexOf(Buffer.from("dd")), 3);
    assert.equal(buf.lastIndexOf(Buffer.from("ab")), 0);
    assert.equal(buf.lastIndexOf(Buffer.from("z")), -1);

    assert.equal(buf.lastIndexOf(new Uint8Array([99])), 5);
    assert.equal(buf.lastIndexOf(new Uint8Array([100, 100])), 3);
    assert.equal(buf.lastIndexOf(new Uint8Array([97, 98])), 0);
    assert.equal(buf.lastIndexOf(new Uint8Array([1])), -1);
    assert.equal(buf.lastIndexOf(new Uint8Array(20)), -1);

    assert.equal(buf.lastIndexOf(98), 6);
    assert.equal(buf.lastIndexOf(1), -1);

    // read
    // 8 bytes (BigInt, Double)
    let data = Buffer.from("ff122334455667ff", "hex");
    assert.equal(data.readBigInt64BE(), -66952337048573953n);
    assert.equal(data.readBigUInt64BE(), 18379791736660977663n);

    assert.equal(data.readBigInt64LE(), -42970816209284353n);
    assert.equal(data.readBigUInt64LE(), 18403773257500267263n);

    assert.equal(data.readDoubleBE(), -1.2438083117537287e+304);
    assert.equal(data.readDoubleLE(), -5.121185661456928e+305);

    // 4 bytes (Int, Float)
    data = Buffer.from("ff1223ff", "hex");
    assert.equal(data.readInt32BE(), -15588353);
    assert.equal(data.readUInt32BE(), 4279378943);

    assert.equal(data.readInt32LE(), -14478593);
    assert.equal(data.readUInt32LE(), 4280488703);

    assert.equal(data.readFloatBE(), -1.9425418978909537e+38);
    assert.equal(data.readFloatLE(), -2.1676279667084385e+38);

    // 2 bytes (short)
    data = Buffer.from("ffabff", "hex");
    assert.equal(data.readInt16BE(), -85);
    assert.equal(data.readUInt16BE(), 65451);

    assert.equal(data.readInt16LE(), -21505);
    assert.equal(data.readUInt16LE(), 44031);

    // byte
    assert.equal(Buffer.from([-1]).readInt8(), -1);
    assert.equal(Buffer.from([-1]).readUInt8(), 255);

    // variable length (1 - 6)
    data = Buffer.from([-1]);
    assert.equal(data.readIntBE(0, 1), -1);
    assert.equal(data.readUIntBE(0, 1), 255);
    assert.equal(data.readIntLE(0, 1), -1);
    assert.equal(data.readUIntLE(0, 1), 255);

    data = Buffer.from("ff12", "hex");
    assert.equal(data.readIntBE(0, 2), -238);
    assert.equal(data.readUIntBE(0, 2), 65298);

    data = Buffer.from("13f6", "hex");
    assert.equal(data.readIntLE(0, 2), -2541);
    assert.equal(data.readUIntLE(0, 2), 62995);

    data = Buffer.from("ff56ff", "hex");
    assert.equal(data.readIntBE(0, 3), -43265);
    assert.equal(data.readUIntBE(0, 3), 16733951);

    data = Buffer.from("ff04ff", "hex");
    assert.equal(data.readIntLE(0, 3), -64257);
    assert.equal(data.readUIntLE(0, 3), 16712959);

    data = Buffer.from("ff5643ff", "hex");
    assert.equal(data.readIntBE(0, 4), -11123713);
    assert.equal(data.readUIntBE(0, 4), 4283843583);
    assert.equal(data.readIntLE(0, 4), -12364033);
    assert.equal(data.readUIntLE(0, 4), 4282603263);

    data = Buffer.from("ff235643ff", "hex");
    assert.equal(data.readIntBE(0, 5), -3702111233);
    assert.equal(data.readUIntBE(0, 5), 1095809516543);
    assert.equal(data.readIntLE(0, 5), -3165248513);
    assert.equal(data.readUIntLE(0, 5), 1096346379263);

    data = Buffer.from("ff12235643ff", "hex");
    assert.equal(data.readIntBE(0, 6), -1021609360385);
    assert.equal(data.readUIntBE(0, 6), 280453367350271);
    assert.equal(data.readIntLE(0, 6), -810303679745);
    assert.equal(data.readUIntLE(0, 6), 280664673030911);

    // write
    // string
    data = Buffer.alloc(10);
    assert.equal(data.write("hello", /*offset=*/ 5, /*limit=*/ 4, "latin1"), 4);
    assertContents(data, [0, 0, 0, 0, 0, 104, 101, 108, 108, 0]); // accurate description of this API

    // 8 bytes
    data = Buffer.alloc(8);
    assert.equal(data.writeBigInt64BE(-66952337048573953n), 8);
    assert.equal(data.readBigInt64BE(), -66952337048573953n);

    assert.equal(data.writeBigUInt64BE(18379791736660977663n), 8);
    assert.equal(data.readBigUInt64BE(), 18379791736660977663n);

    assert.equal(data.writeBigInt64LE(-42970816209284353n), 8);
    assert.equal(data.readBigInt64LE(), -42970816209284353n);

    assert.equal(data.writeBigUInt64LE(18403773257500267263n), 8);
    assert.equal(data.readBigUInt64LE(), 18403773257500267263n);

    assert.equal(data.writeDoubleBE(-1.2438083117537287e+304), 8);
    assert.equal(data.readDoubleBE(), -1.2438083117537287e+304);

    assert.equal(data.writeDoubleLE(-5.121185661456928e+305), 8);
    assert.equal(data.readDoubleLE(), -5.121185661456928e+305);

    // 4 bytes (Int, Float)
    data = Buffer.alloc(4);
    assert.equal(data.writeInt32BE(-15588353), 4);
    assert.equal(data.readInt32BE(), -15588353);
    assert.equal(data.writeUInt32BE(4279378943), 4);
    assert.equal(data.readUInt32BE(), 4279378943);

    assert.equal(data.writeInt32LE(-14478593), 4);
    assert.equal(data.readInt32LE(), -14478593);
    assert.equal(data.writeUInt32LE(4280488703), 4);
    assert.equal(data.readUInt32LE(), 4280488703);

    assert.equal(data.writeFloatBE(-1.9425418978909537e+38), 4);
    assert.equal(data.readFloatBE(), -1.9425418978909537e+38);
    assert.equal(data.writeFloatLE(-2.1676279667084385e+38), 4);
    assert.equal(data.readFloatLE(), -2.1676279667084385e+38);

    // 2 bytes (short)
    data = Buffer.alloc(2);
    assert.equal(data.writeInt16BE(-85), 2);
    assert.equal(data.readInt16BE(), -85);
    assert.equal(data.writeUInt16BE(65451), 2);
    assert.equal(data.readUInt16BE(), 65451);

    assert.equal(data.writeInt16LE(-21505), 2);
    assert.equal(data.readInt16LE(), -21505);
    assert.equal(data.writeUInt16LE(44031), 2);
    assert.equal(data.readUInt16LE(), 44031);

    // byte
    data = Buffer.alloc(1);
    assert.equal(data.writeInt8(-1), 1);
    assert.equal(data.readInt8(), -1);
    assert.equal(data.writeUInt8(255), 1);
    assert.equal(data.readUInt8(), 255);

    // swap 64
    data = Buffer.alloc(16); // signed and unsigned variants in the same buffer
    assert.equal(data.writeBigInt64BE(-66952337048573953n), 8);
    assert.equal(data.writeBigUInt64BE(18379791736660977663n, 8), 16);

    data.swap64(); // start as big endian, swap to little endian
    assert.equal(data.readBigInt64LE(), -66952337048573953n);
    assert.equal(data.readBigUInt64LE(8), 18379791736660977663n);

    data.swap64(); // swap back to big endian
    assert.equal(data.readBigInt64BE(), -66952337048573953n);
    assert.equal(data.readBigUInt64BE(8), 18379791736660977663n);

    data = Buffer.alloc(8);
    assert.equal(data.writeDoubleBE(-1.2438083117537287e+304), 8);
    data.swap64();
    assert.equal(data.readDoubleLE(), -1.2438083117537287e+304);
    data.swap64();
    assert.equal(data.readDoubleBE(), -1.2438083117537287e+304);

    // swap 32
    data = Buffer.alloc(8);
    assert.equal(data.writeInt32BE(-15588353), 4);
    assert.equal(data.writeUInt32BE(4279378943, 4), 8);

    data.swap32()
    assert.equal(data.readInt32LE(), -15588353);
    assert.equal(data.readUInt32LE(4), 4279378943);

    data.swap32()
    assert.equal(data.readInt32BE(), -15588353);
    assert.equal(data.readUInt32BE(4), 4279378943);

    data = Buffer.alloc(4);
    assert.equal(data.writeFloatBE(-1.9425418978909537e+38), 4);
    data.swap32()
    assert.equal(data.readFloatLE(), -1.9425418978909537e+38);
    data.swap32()
    assert.equal(data.readFloatBE(), -1.9425418978909537e+38);

    // swap 16
    data = Buffer.alloc(4);
    assert.equal(data.writeInt16BE(-85), 2);
    assert.equal(data.writeUInt16BE(65451, 2), 4);

    data.swap16();
    assert.equal(data.readInt16LE(), -85);
    assert.equal(data.readUInt16LE(), 65451);

    data.swap16();
    assert.equal(data.readInt16BE(), -85);
    assert.equal(data.readUInt16BE(), 65451);

    // subarray
    let original = Buffer.from([0, 1, 2, 3]);
    let view = original.subarray(1, 2);
    assertContents(view, [1]);
    original[1] = 5;
    assertContents(view, [5]);

    // toJSON
    const json = Buffer.from([1, 2, 3]).toJSON();
    assert.equal(json.type, "Buffer");
    assertContents(json.data, [1, 2, 3]);

    // toString
    assert.equal(Buffer.from("abc").toString(), "abc")
    assert.equal(Buffer.from([1, 2, 3, 4]).toString("hex", 1, 2), "02")
    """
  }
}

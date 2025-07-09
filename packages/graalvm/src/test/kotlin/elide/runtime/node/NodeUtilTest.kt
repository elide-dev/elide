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
@file:Suppress("JSUnresolvedReference", "JSUnusedLocalSymbols", "JSPrimitiveTypeWrapperUsage", "LargeClass")

package elide.runtime.node

import com.oracle.truffle.js.runtime.builtins.JSPromiseObject
import com.oracle.truffle.js.runtime.objects.Undefined
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.SortedSet
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.exec.GuestExecution
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.node.util.DebugLoggerImpl
import elide.runtime.node.util.InertDebugLogger
import elide.runtime.node.util.MimeTypes
import elide.runtime.node.util.NodeSystemErrors
import elide.runtime.node.util.NodeUtilModule
import elide.testing.annotations.TestCase

private val allTypeChecks = arrayOf(
  "isAnyArrayBuffer",
  "isArrayBufferView",
  "isArgumentsObject",
  "isArrayBuffer",
  "isAsyncFunction",
  "isBigInt64Array",
  "isBigIntObject",
  "isBigUint64Array",
  "isBooleanObject",
  "isBoxedPrimitive",
  "isCryptoKey",
  "isDataView",
  "isDate",
  "isExternal",
  "isFloat16Array",
  "isFloat32Array",
  "isFloat64Array",
  "isGeneratorFunction",
  "isGeneratorObject",
  "isInt8Array",
  "isInt16Array",
  "isInt32Array",
  "isKeyObject",
  "isMap",
  "isMapIterator",
  "isModuleNamespaceObject",
  "isNativeError",
  "isNumberObject",
  "isProxy",
  "isRegExp",
  "isSet",
  "isSetIterator",
  "isSharedArrayBuffer",
  "isStringObject",
  "isSymbolObject",
  "isTypedArray",
  "isUint8Array",
  "isUint8ClampedArray",
  "isUint16Array",
  "isUint32Array",
  "isWeakMap",
  "isWeakSet",
  "isPromise",
)

/** Tests for Elide's implementation of the Node `util` built-in module. */
@TestCase internal class NodeUtilTest : NodeModuleConformanceTest<NodeUtilModule>() {
  override val moduleName: String get() = "util"
  override fun provide(): NodeUtilModule = NodeUtilModule(GuestExecutorProvider {
    GuestExecution.direct()
  })

  @Inject lateinit var util: NodeUtilModule

  private inline fun withActivatedLoggers(suite: SortedSet<String> = sortedSetOf(), op: () -> Unit) {
    DebugLoggerImpl.Factory.resetActivatedLogs()
    DebugLoggerImpl.Factory.mountActivatedLogs(suite)

    try {
      op.invoke()
    } finally {
      DebugLoggerImpl.Factory.resetActivatedLogs()
      DebugLoggerImpl.Factory.mountActivatedLogs(DebugLoggerImpl.Factory.buildActivatedLogs())
    }
  }

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("callbackify")
    yield("debuglog")
    yield("debug")
    yield("deprecate")
    yield("diff")
    yield("format")
    yield("formatWithOptions")
    yield("getCallSites")
    yield("getSystemErrorName")
    yield("getSystemErrorMap")
    yield("inherits")
    yield("inspect")
    yield("isDeepStrictEqual")
    yield("MIMEType")
    yield("MIMEParams")
    yield("parseArgs")
    yield("parseEnv")
    yield("promisify")
    yield("stripVTControlCharacters")
    yield("styleText")
    yield("TextDecoder")
    yield("TextEncoder")
    yield("toUSVString")
    yield("transferableAbortController")
    yield("transferableAbortSignal")
    yield("aborted")
    yield("types")
    yield("_extend")
    yield("isArray")
  }

  @Test override fun testInjectable() {
    assertNotNull(util)
  }

  @Test fun `callbackify - requires a function`() {
    val mod = provide().provide()
    assertThrows<Throwable> { mod.callbackify(null) }
    assertThrows<Throwable> { mod.callbackify(Value.asValue(null)) }
    assertThrows<Throwable> { mod.callbackify(Value.asValue("sample")) }
    assertThrows<Throwable> { mod.callbackify(Value.asValue(42)) }
  }

  @Test fun `callbackify - handles resolution`() = executeESM {
    // language=javascript
    """
      import { callbackify } from "node:util";
      const sample = async () => "hello";

      test(callbackify).isNotNull();
      const callbacked = callbackify(sample);
      test(callbacked).isNotNull();
      let called = false;
      let err = null;
      let value = null;
      const promise = callbacked((e, v) => {
        called = true;
        err = e;
        value = v;
      });
      await promise;
      const out = {
        promise,
        interrogate: () => {
          return ({
            called,
            err,
            value
          })
        }
      };
      export default out;
    """
  }.thenAssert {
    val obj = assertNotNull(it.returnValue())
    val surface = assertNotNull(obj.getMember("default"))
    val fut = assertNotNull(surface.getMember("promise"))
    val grabber = assertNotNull(surface.getMember("interrogate"))
    val promise = fut.`as`<JSPromiseObject>(JSPromiseObject::class.java)
    assertNotNull(promise, "Expected a promise object")
    val grabbed = assertNotNull(grabber.execute())
    val called = assertNotNull(grabbed.getMember("called")).asBoolean()
    val err = assertNotNull(grabbed.getMember("err"))
    val value = assertNotNull(grabbed.getMember("value")).asString()

    assertTrue(called, "expected callback to be called")
    assertTrue(err.isNull, "expected no error to be passed to callback")
    assertEquals("hello", value, "expected callback to return 'hello'")
  }

  @Test fun `callbackify - passes extra args`() = executeESM {
    // language=javascript
    """
      import { callbackify } from "node:util";
      const sample = async (a, b) => a + b;

      test(callbackify).isNotNull();
      const callbacked = callbackify(sample);
      test(callbacked).isNotNull();
      let called = false;
      let err = null;
      let value = null;
      const promise = callbacked(40, 2, (e, v) => {
        called = true;
        err = e;
        value = v;
      });
      await promise;
      const out = {
        promise,
        interrogate: () => {
          return ({
            called,
            err,
            value
          })
        }
      };
      export default out;
    """
  }.thenAssert {
    val obj = assertNotNull(it.returnValue())
    val surface = assertNotNull(obj.getMember("default"))
    val fut = assertNotNull(surface.getMember("promise"))
    val grabber = assertNotNull(surface.getMember("interrogate"))
    val promise = fut.`as`<JSPromiseObject>(JSPromiseObject::class.java)
    assertNotNull(promise, "Expected a promise object")
    val grabbed = assertNotNull(grabber.execute())
    val called = assertNotNull(grabbed.getMember("called")).asBoolean()
    val err = assertNotNull(grabbed.getMember("err"))
    val value = assertNotNull(grabbed.getMember("value")).asInt()

    assertTrue(called, "expected callback to be called")
    assertTrue(err.isNull, "expected no error to be passed to callback")
    assertEquals(42, value, "expected callback to return 'hello'")
  }

  @Test fun `callbackify - handles rejection`() = executeESM {
    // language=javascript
    """
      import { callbackify } from "node:util";
      const sample = async () => {
        throw new Error("error!")
      };

      test(callbackify).isNotNull();
      const callbacked = callbackify(sample);
      test(callbacked).isNotNull();
      let called = false;
      let err = null;
      let value = null;
      let propagated = false;
      const promise = callbacked((e, v) => {
        called = true;
        err = e;
        value = v;
      });
      try {
        await promise;
      } catch (err) {
        propagated = true;
      }
      const out = {
        promise,
        interrogate: () => {
          return ({
            called,
            err,
            value,
            propagated
          })
        }
      };
      export default out;
    """
  }.thenAssert {
    val obj = assertNotNull(it.returnValue())
    val surface = assertNotNull(obj.getMember("default"))
    val fut = assertNotNull(surface.getMember("promise"))
    val grabber = assertNotNull(surface.getMember("interrogate"))
    val promise = fut.`as`<JSPromiseObject>(JSPromiseObject::class.java)
    assertNotNull(promise, "Expected a promise object")
    val grabbed = assertNotNull(grabber.execute())
    val called = assertNotNull(grabbed.getMember("called")).asBoolean()
    val propagated = assertNotNull(grabbed.getMember("propagated")).asBoolean()
    val err = assertNotNull(grabbed.getMember("err"))
    val value = assertNotNull(grabbed.getMember("value"))

    assertTrue(called, "expected callback to be called")
    assertFalse(propagated, "expected error to be propagated")
    assertFalse(err.isNull, "expected error to be passed to callback")
    assertTrue(value.isNull, "expected no value for errored callback")
  }

  @Test fun `promisify - requires a function`() {
    val mod = provide().provide()
    assertThrows<Throwable> { mod.promisify(null) }
    assertThrows<Throwable> { mod.promisify(Value.asValue(null)) }
    assertThrows<Throwable> { mod.promisify(Value.asValue("sample")) }
    assertThrows<Throwable> { mod.promisify(Value.asValue(42)) }
  }

  @Test fun `promisify - handles resolution`() = executeESM {
    // language=javascript
    """
      import { promisify } from "node:util";
      const sample = (cb) => {
        cb(null, 42);
      };

      test(promisify).isNotNull();
      const promisified = promisify(sample);
      test(promisified).isNotNull();
      const result = await promisified();
      export default result;
    """
  }.thenAssert {
    val result = assertNotNull(it.returnValue()).getMember("default").asInt()
    assertEquals(42, result, "expected promisified function to resolve to 42")
  }

  @Test fun `promisify - handles rejection`() = executeESM {
    // language=javascript
    """
      import { promisify } from "node:util";
      const sample = (cb) => {
        cb(new Error("error!"), null);
      };

      test(promisify).isNotNull();
      const promisified = promisify(sample);
      test(promisified).isNotNull();
      let err = null;
      let result = null;
      try {
        result = await promisified();
      } catch (e) {
        err = e;
      }
      export default {
        result,
        err
      };
    """
  }.thenAssert {
    val surface = assertNotNull(assertNotNull(it.returnValue()).getMember("default"), "no default export")
    val result = assertNotNull(surface.getMember("result"))
    val err = assertNotNull(surface.getMember("err"))
    assertTrue(result.isNull)
    assertFalse(err.isNull)
  }

  @Test fun `promisify - passes extra args`() = executeESM {
    // language=javascript
    """
      import { promisify } from "node:util";
      const sample = (a, b, cb) => {
        cb(null, a + b);
      };

      test(promisify).isNotNull();
      const promisified = promisify(sample);
      test(promisified).isNotNull();
      let err = null;
      let result = null;
      try {
        result = await promisified(40, 2);
      } catch (e) {
        err = e;
      }
      export default {
        result,
        err
      };
    """
  }.thenAssert {
    val surface = assertNotNull(assertNotNull(it.returnValue()).getMember("default"), "no default export")
    val result = assertNotNull(surface.getMember("result"))
    val err = assertNotNull(surface.getMember("err"))
    assertFalse(result.isNull)
    assertTrue(err.isNull)
    val out = result.asInt()
    assertEquals(42, out, "expected promisified function to resolve to 42")
  }

  @Test fun `debuglog - creating inert log`() {
    val logger = provide().provide().debuglog("test")
    assertNotNull(logger)
    assertIs<InertDebugLogger>(logger)
    assertDoesNotThrow {
      logger("here is a debug log which will be dropped")
    }
    assertFalse(logger.enabled)
  }

  @Test fun `debuglog - creating active log`() {
    withActivatedLoggers(sortedSetOf("test")) {
      val logger = provide().provide().debuglog("test")
      assertNotNull(logger)
      assertIsNot<InertDebugLogger>(logger)
      assertDoesNotThrow {
        logger("here is a debug log which will not be dropped")
      }
      assertTrue(logger.enabled)
      assertEquals("test", logger.loggerName)
    }
  }

  @Test fun `deprecate - requires a callable`() {
    val mod = provide().provide()
    assertThrows<Throwable>("guest null should throw") { mod.deprecate(Value.asValue(null)) }
    assertThrows<Throwable>("guest string should throw") { mod.deprecate(Value.asValue("sample")) }
    assertThrows<Throwable>("guest number should throw") { mod.deprecate(Value.asValue(42)) }
    val deprecated = mod.getMember("deprecate") as ProxyExecutable
    assertThrows<Throwable>("guest with no args should throw") { deprecated.execute() }
  }

  @Test fun `deprecate - requires a callable from guest`() = executeGuest {
    // language=javascript
    """
    const { deprecate } = require("node:util");
    deprecate();
    """
  }.fails()

  @Test fun `deprecate - create deprecated callable`() = executeGuest {
    // language=javascript
    """
      const { deprecate } = require("node:util");
      let called = false;
      const sample = () => {
        called = true;
        return "hello"
      };

      test(deprecate).isNotNull();
      const deprecated = deprecate(sample, "example deprecated method");
      test(deprecated).isNotNull();
      let result = deprecated();
      test(called).shouldBeTrue();
      ({
        result,
        called
      });
    """
  }.thenAssert {
    val retval = assertNotNull(it.returnValue())
    val result = assertNotNull(retval.getMember("result")).asString()
    val called = assertNotNull(retval.getMember("called")).asBoolean()
    assertTrue(called, "expected deprecated function to be called")
    assertEquals("hello", result, "expected deprecated function to return 'hello'")
  }

  @Test fun `deprecate - create deprecated callable with no message`() = executeGuest {
    // language=javascript
    """
      const { deprecate } = require("node:util");
      let called = false;
      const sample = () => {
        called = true;
        return "hello"
      };

      test(deprecate).isNotNull();
      const deprecated = deprecate(sample);
      test(deprecated).isNotNull();
      let result = deprecated();
      test(called).shouldBeTrue();
      ({
        result,
        called
      });
    """
  }.thenAssert {
    val retval = assertNotNull(it.returnValue())
    val result = assertNotNull(retval.getMember("result")).asString()
    val called = assertNotNull(retval.getMember("called")).asBoolean()
    assertTrue(called, "expected deprecated function to be called")
    assertEquals("hello", result, "expected deprecated function to return 'hello'")
  }

  @Test fun `deprecate - create deprecated callable with message and code`() = executeGuest {
    // language=javascript
    """
      const { deprecate } = require("node:util");
      let called = false;
      const sample = () => {
        called = true;
        return "hello"
      };

      test(deprecate).isNotNull();
      const deprecated = deprecate(sample, "deprecated method", "DEP0001");
      test(deprecated).isNotNull();
      let result = deprecated();
      test(called).shouldBeTrue();
      ({
        result,
        called
      });
    """
  }.thenAssert {
    val retval = assertNotNull(it.returnValue())
    val result = assertNotNull(retval.getMember("result")).asString()
    val called = assertNotNull(retval.getMember("called")).asBoolean()
    assertTrue(called, "expected deprecated function to be called")
    assertEquals("hello", result, "expected deprecated function to return 'hello'")
  }

  @Test fun `deprecate - deprecated callable passes args`() = executeGuest {
    // language=javascript
    """
      const { deprecate } = require("node:util");
      const sample = (a) => {
        return a;
      };

      test(deprecate).isNotNull();
      const deprecated = deprecate(sample, "example deprecated method");
      test(deprecated).isNotNull();
      let result = deprecated(5);
      ({
        result
      });
    """
  }.thenAssert {
    val retval = assertNotNull(it.returnValue())
    val result = assertNotNull(retval.getMember("result")).asInt()
    assertEquals(5, result, "expected deprecated function to return 5")
  }

  @Test fun `transferableAbortController - creates controller`() = executeGuest {
    // language=javascript
    """
      const { transferableAbortController } = require("node:util");
      const controller = transferableAbortController();
      test(controller).isNotNull();
    """
  }.doesNotFail()

  @Test fun `transferableAbortSignal - requires an argument`() = executeGuest {
    // language=javascript
    """
      const { transferableAbortSignal } = require("node:util");
      transferableAbortSignal();
    """
  }.fails()

  @Test fun `transferableAbortSignal - marks signal`() = executeGuest {
    // language=javascript
    """
      const { transferableAbortController, transferableAbortSignal } = require("node:util");
      const controller = transferableAbortController();
      test(controller).isNotNull();
      const signal = controller.signal;
      test(signal).isNotNull();
      const marked = transferableAbortSignal(signal);
      test(marked === signal).shouldBeTrue();
    """
  }.doesNotFail()

  @Test fun `isArray - works for guest arrays`() = executeGuest {
    // language=javascript
    """
      const { isArray } = require("node:util");
      const arr = [1, 2, 3];
      test(isArray(arr)).shouldBeTrue();
      test(isArray({})).shouldBeFalse();
      test(isArray("string")).shouldBeFalse();
    """
  }.doesNotFail()

  @Test fun `isArray - works for guest arrays on host`() = executeGuest {
    // language=javascript
    """
      const arr = [1, 2, 3];
      arr;
    """
  }.thenAssert {
    val arraySample = assertNotNull(it.returnValue())
    assertTrue(assertDoesNotThrow { provide().provide().isArray(arraySample) })
  }

  @Test fun `isArray - falsy for non-array types`() {
    val mod = provide().provide()
    assertFalse(mod.isArray(null))
    assertFalse(mod.isArray(Value.asValue(42)))
    assertFalse(mod.isArray(Value.asValue("a string")))
    assertFalse(mod.isArray(Value.asValue(5.5)))
  }

  @Test fun `inspect - basic primitives`() {
    val util = provide().provide()
    assertEquals("42", util.inspect(42))
    assertEquals("'42'", util.inspect("42"))
    assertEquals("5.5", util.inspect(5.5))
    assertEquals("'5.5'", util.inspect("5.5"))
    assertEquals("false", util.inspect(false))
    assertEquals("true", util.inspect(true))
    assertEquals("null", util.inspect(Value.asValue(null)))
    assertEquals("undefined", util.inspect(Undefined.instance))
  }

  @Test fun `inspect - host lists`() {
    val util = provide().provide()
    val list = listOf(1, 2, 3)
    assertEquals("[ 1, 2, 3 ]", util.inspect(list))
  }

  @Test fun `inspect - host maps`() {
    val util = provide().provide()
    val list = mapOf("hi" to 1, "example" to 2, "another" to 3)
    assertEquals("Map(3) { 'hi' => 1, 'example' => 2, 'another' => 3 }", util.inspect(list))
  }

  @Test fun `inspect - host sets`() {
    val util = provide().provide()
    val list = setOf("hi", "hello")
    assertEquals("Set(2) { 'hi', 'hello' }", util.inspect(list))
  }

  @Test fun `inspect - guest arrays`() = executeGuest {
    // language=javascript
    "[1, 2, 3]"
  }.thenAssert {
    val util = provide().provide()
    val list = assertNotNull(it.returnValue())
    assertEquals("[ 1, 2, 3 ]", util.inspect(list))
  }

  @Test fun `inspect - guest maps`() = executeGuest {
    // language=javascript
    """
    const map = new Map();
    map.set("hi", 1);
    map.set("example", 2);
    map.set("another", 3);
    map;
    """
  }.thenAssert {
    val util = provide().provide()
    val list = assertNotNull(it.returnValue())
    assertEquals("Map(3) { 'hi' => 1, 'example' => 2, 'another' => 3 }", util.inspect(list))
  }

  @Test fun `MIMETypes - parse from string`() {
    assertNotNull(MimeTypes.parse("text/html")).let {
      assertEquals("text", it.type)
      assertEquals("html", it.subtype)
      assertNull(it.params)
    }
  }

  @Test fun `MIMETypes - parse from string with parameters`() {
    assertNotNull(MimeTypes.parse("text/html;charset=utf-8")).let {
      assertEquals("text", it.type)
      assertEquals("html", it.subtype)
      assertNotNull(it.params)
      assertTrue(it.params!!.hasMember("charset"))
      assertTrue(it.params!!.memberKeys.contains("charset"))
      val charset = it.params!!.getMember("charset") as String
      assertEquals("utf-8", charset, "expected charset to be 'utf-8'")
    }
  }

  @CsvSource(
    "text/html,text,html",
    "application/json,application,json",
    "image/png,image,png",
    "audio/mpeg,audio,mpeg",
    "video/mp4,video,mp4",
    "text/html;charset=UTF-8,text,html",
    "application/json;charset=utf-8,application,json",
  )
  @ParameterizedTest fun `MIMETypes - parse known mime type`(mime: String, type: String, subtype: String) {
    assertNotNull(MimeTypes.parse(mime)).let {
      assertEquals(type, it.type)
      assertEquals(subtype, it.subtype)
      assertEquals(mime, it.essence)
      assertEquals(mime, it.toString())
    }
  }

  @Test fun `MIMETypes - parse from string via guest`() = executeGuest {
    // language=javascript
    """
      const { MIMEType } = require("node:util");
      const mime = new MIMEType("text/html;charset=utf-8");
      test(mime).isNotNull();
      test(mime.type).equals("text");
      test(mime.subtype).equals("html");
      test(mime.params).isNotNull();
      test(mime.params.get("charset")).equals("utf-8");
    """
  }.doesNotFail()

  @Test fun `getSystemErrorName - retrieves error name`() {
    val mod = provide().provide()
    // -13 to ("EACCES" to "permission denied")
    val known = assertNotNull(NodeSystemErrors[NodeSystemErrors.EACCES])
    val name = mod.getSystemErrorName(NodeSystemErrors.EACCES)
    assertEquals(NodeSystemErrors.EACCES, known.id)
    assertEquals("EACCES", known.name)
    assertEquals("permission denied", known.message)
    assertEquals("EACCES", name, "expected system error name to be 'EACCES'")
  }

  @TestFactory fun testGetSystemErrorName(): Stream<DynamicTest> = sequence<DynamicTest> {
    val mod = provide().provide()
    NodeSystemErrors.all().forEach { error ->
      yield(DynamicTest.dynamicTest("${error.id} - ${error.name}") {
        val info = NodeSystemErrors[error.id]
        assertNotNull(info)
        assertNotNull(NodeSystemErrors[error.name])
        val name = mod.getSystemErrorName(error.id)
        assertEquals(error.id, info.id)
        assertEquals(error.name, info.name)
        assertTrue(error.name.isNotEmpty())
        assertEquals(error.message, info.message)
        assertTrue(error.message.isNotEmpty())
        assertEquals(error.name, name, "expected system error name to be '${error.name}'")
      })
    }
  }.asStream()

  private fun SequenceScope<DynamicTest>.testGuestType(
    method: String,
    desc: String? = null,
    pass: Boolean = true,
    obtainer: SequenceScope<DynamicTest>.() -> String,
  ): DynamicTest {
    val label = "types.$method${if (desc != null) " - $desc" else ""}"
    return DynamicTest.dynamicTest(label) {
      val code = obtainer.invoke(this)
      executeGuest {
        // language=javascript
        """
            const { types } = require("node:util");
            $code
            types.$method(subject);
          """
      }.let {
        it.thenAssert { inner ->
          val ret = assertNotNull(inner.returnValue(), "expected return value from inner type check function")
          assertTrue(ret.isBoolean, "expected boolean return value from type check for '$method'")
          if (pass) {
            assertTrue(ret.asBoolean(), "expected type check to pass for '$method'")
          } else {
            assertFalse(ret.asBoolean(), "expected type check to fail for '$method'")
          }
        }
      }
    }
  }

  private suspend fun SequenceScope<DynamicTest>.testIsPromise() {
    yield(testGuestType("isPromise") {
      // language=javascript
      """
        const fn = async () => "hello";
        const subject = fn();
      """
    })

    yield(testGuestType("isPromise", "not a number", pass = false) {
      // language=javascript
      """
        const subject = 5;
      """
    })

    yield(testGuestType("isPromise", "not a string", pass = false) {
      // language=javascript
      """
        const subject = 5;
      """
    })
  }

  private suspend fun SequenceScope<DynamicTest>.testIsStringObject() {
    yield(testGuestType("isStringObject") {
      // language=javascript
      """
        const subject = new String("hi");
      """
    })

    yield(testGuestType("isStringObject", "not a raw string", pass = false) {
      // language=javascript
      """
        const subject = "hi";
      """
    })

    yield(testGuestType("isStringObject", "not a raw number", pass = false) {
      // language=javascript
      """
        const subject = "hi";
      """
    })
  }

  private suspend fun SequenceScope<DynamicTest>.testIsRegExp() {
    yield(testGuestType("isRegExp") {
      // language=javascript
      """
        const subject = new RegExp("hi");
      """
    })

    yield(testGuestType("isRegExp", "not a raw string", pass = false) {
      // language=javascript
      """
        const subject = "hi";
      """
    })

    yield(testGuestType("isRegExp", "not a raw number", pass = false) {
      // language=javascript
      """
        const subject = 42;
      """
    })
  }

  private suspend fun SequenceScope<DynamicTest>.testIsMap() {
    yield(testGuestType("isMap") {
      // language=javascript
      """
        const subject = new Map();
      """
    })

    yield(testGuestType("isMap", "not an object", pass = false) {
      // language=javascript
      """
        const subject = {};
      """
    })
  }

  private suspend fun SequenceScope<DynamicTest>.testIsSet() {
    yield(testGuestType("isSet") {
      // language=javascript
      """
        const subject = new Set();
      """
    })

    yield(testGuestType("isSet", "not an object", pass = false) {
      // language=javascript
      """
        const subject = {};
      """
    })
  }

  private suspend fun SequenceScope<DynamicTest>.testIsWeakMap() {
    yield(testGuestType("isWeakMap") {
      // language=javascript
      """
        const subject = new WeakMap();
      """
    })

    yield(testGuestType("isWeakMap", "not a regular map", pass = false) {
      // language=javascript
      """
        const subject = new Map();
      """
    })
  }

  private suspend fun SequenceScope<DynamicTest>.testIsWeakSet() {
    yield(testGuestType("isWeakSet") {
      // language=javascript
      """
        const subject = new WeakSet();
      """
    })

    yield(testGuestType("isWeakSet", "not a regular set", pass = false) {
      // language=javascript
      """
        const subject = new Set();
      """
    })
  }

  private suspend fun SequenceScope<DynamicTest>.testIsDate() {
    yield(testGuestType("isDate") {
      // language=javascript
      """
        const subject = new Date();
      """
    })

    yield(testGuestType("isDate", "not an object", pass = false) {
      // language=javascript
      """
        const subject = {};
      """
    })

    yield(testGuestType("isDate", "not a number", pass = false) {
      // language=javascript
      """
        const subject = 42;
      """
    })

    yield(testGuestType("isDate", "not a string", pass = false) {
      // language=javascript
      """
        const subject = (new Date()).toString();
      """
    })
  }

  private suspend fun SequenceScope<DynamicTest>.testIsBooleanObject() {
    yield(testGuestType("isBooleanObject", "truthy") {
      // language=javascript
      """
        const subject = new Boolean(true);
      """
    })

    yield(testGuestType("isBooleanObject", "falsy") {
      // language=javascript
      """
        const subject = new Boolean(false);
      """
    })
  }

  private suspend fun SequenceScope<DynamicTest>.testIsNumberObject() {
    yield(testGuestType("isNumberObject") {
      // language=javascript
      """
        const subject = new Number(42);
      """
    })

    yield(testGuestType("isNumberObject", "not a raw int", pass = false) {
      // language=javascript
      """
        const subject = 42;
      """
    })

    yield(testGuestType("isNumberObject", "not a string number", pass = false) {
      // language=javascript
      """
        const subject = "42";
      """
    })

    yield(testGuestType("isNumberObject", "not a string float", pass = false) {
      // language=javascript
      """
        const subject = "42";
      """
    })
  }

  private suspend fun SequenceScope<DynamicTest>.testIsSymbolObject() {
    yield(testGuestType("isSymbolObject") {
      // language=javascript
      """
        const subject = Symbol("hello");
      """
    })

    yield(testGuestType("isSymbolObject", "not a raw string", pass = false) {
      // language=javascript
      """
        const subject = "hello";
      """
    })
  }

  private suspend fun SequenceScope<DynamicTest>.testIsBigIntObject() {
    yield(testGuestType("isBigIntObject", "only a bigint-wrapped object") {
      // language=javascript
      """
        const subject = Object(BigInt(9007199254740991n));
      """
    })

    yield(testGuestType("isBigIntObject", "not a bigint literal", pass = false) {
      // language=javascript
      """
        const subject = 9007199254740991n;
      """
    })

    yield(testGuestType("isBigIntObject", "not a BigInt object", pass = false) {
      // language=javascript
      """
        const subject = BigInt(9007199254740991n);
      """
    })
  }

  private suspend fun SequenceScope<DynamicTest>.testIsAsyncFunction() {
    yield(testGuestType("isAsyncFunction") {
      // language=javascript
      """
        const subject = async () => "hello";
      """
    })

    yield(testGuestType("isAsyncFunction", "not a promise", pass = false) {
      // language=javascript
      """
        const op = async () => "hello";
        const subject = op();
      """
    })

    yield(testGuestType("isAsyncFunction", "not a regular function", pass = false) {
      // language=javascript
      """
        const subject = () => "hello";
      """
    })
  }

  private suspend fun SequenceScope<DynamicTest>.testIsGeneratorFunction() {
    yield(testGuestType("isGeneratorFunction") {
      // language=javascript
      """
      const subject = function* () { yield "hello"; };
      """
    })

    yield(testGuestType("isGeneratorFunction", "not an async function", pass = false) {
      // language=javascript
      """
      const subject = async () => "hello";
      """
    })

    yield(testGuestType("isGeneratorFunction", "not a regular function", pass = false) {
      // language=javascript
      """
      const subject = () => "hello";
      """
    })
  }

  private suspend fun SequenceScope<DynamicTest>.testIsArgumentsObject() {
    // Always `false`.
    yield(testGuestType("isArgumentsObject", pass = false) {
      // language=javascript
      """
      const subject = (() => arguments)();
      """
    })
  }

  private suspend fun SequenceScope<DynamicTest>.testTypedArraysAndBuffers() {
    // isArrayBuffer
    yield(testGuestType("isArrayBuffer") {
      // language=javascript
      """
      const subject = new ArrayBuffer(8);
      """
    })
    yield(testGuestType("isArrayBuffer", "not a view", pass = false) {
      // language=javascript
      """
      const buf = new ArrayBuffer(8);
      const subject = new Uint8Array(buf);
      """
    })

    // isArrayBufferView / isTypedArray
    val arrayTypes = sortedSetOf(
      "Uint8Array",
      "Uint8ClampedArray",
      "Uint16Array",
      "Uint32Array",
      "Int8Array",
      "Int16Array",
      "Int32Array",
      "Float32Array",
      "Float64Array",
      "BigInt64Array",
      "BigUint64Array",
    )
    arrayTypes.forEach { arrayType ->
      yield(testGuestType("is$arrayType") {
        // language=javascript
        """
            const buf = new ArrayBuffer(8);
            const subject = new $arrayType(buf);
        """
      })
      yield(testGuestType("isArrayBufferView", "as $arrayType") {
        // language=javascript
        """
            const buf = new ArrayBuffer(8);
            const subject = new $arrayType(buf);
        """
      })
      yield(testGuestType("isAnyArrayBuffer", "as $arrayType") {
        // language=javascript
        """
            const buf = new ArrayBuffer(8);
            const subject = new $arrayType(buf);
        """
      })
      yield(testGuestType("isTypedArray", "as $arrayType") {
        // language=javascript
        """
          const buf = new ArrayBuffer(8);
          const subject = new $arrayType(buf);
        """
      })
      (arrayTypes - arrayType).forEach { otherType ->
        yield(testGuestType("is$arrayType", "not a $otherType", pass = false) {
          // language=javascript
          """
            const buf = new ArrayBuffer(8);
            const subject = new $otherType(buf);
        """
        })
      }
    }

    yield(testGuestType("isAnyArrayBuffer", "as ArrayBuffer") {
      // language=javascript
      """
      const subject = new ArrayBuffer(8);
      """
    })
    yield(testGuestType("isArrayBufferView", "not an array", pass = false) {
      // language=javascript
      """
      const subject = new ArrayBuffer(8);
      """
    })

    // isSharedArrayBuffer
  }

  private suspend fun SequenceScope<DynamicTest>.allSpecificTypeCheckTests() {
    testIsPromise()
    testIsStringObject()
    testIsRegExp()
    testIsMap()
    testIsSet()
    testIsWeakMap()
    testIsWeakSet()
    testIsDate()
    testIsBooleanObject()
    testIsNumberObject()
    testIsSymbolObject()
    testIsBigIntObject()
    testIsAsyncFunction()
    testIsGeneratorFunction()
    testIsArgumentsObject()
    testTypedArraysAndBuffers()
  }

  private val implementedTypeChecks = arrayOf(
    "isPromise",
    "isStringObject",
    "isRegExp",
    "isMap",
    "isSet",
    "isDate",
    "isBooleanObject",
    "isNumberObject",
    "isWeakMap",
    "isWeakSet",
    "isSymbolObject",
    "isBigIntObject",
    "isAsyncFunction",
    "isGeneratorFunction",
    "isArgumentsObject",
    "isArrayBuffer",
    "isArrayBufferView",
    "isSharedArrayBuffer",
    "isTypedArray",
    "isUint8Array",
    "isUint8ClampedArray",
    "isUint16Array",
    "isUint32Array",
    "isInt8Array",
    "isInt16Array",
    "isInt32Array",
    "isFloat16Array",
    "isFloat32Array",
    "isFloat64Array",
    "isUint8ClampedArray",
    "isAnyArrayBuffer",
    "isBigInt64Array",
    "isBigUint64Array",
    "isSharedArrayBuffer",
  )

  @TestFactory fun testTypechecks(): Stream<DynamicTest> = sequence<DynamicTest> {
    allTypeChecks.forEach { method ->
      yield(DynamicTest.dynamicTest("$method - exists") {
        executeGuest {
          // language=javascript
          """
            const { types } = require("node:util");
            const { $method: method } = types;
            test(method).isNotNull();
          """
        }.doesNotFail()
      })

      yield(testGuestType(method, "`false` for `null`", pass = false) {
        Assumptions.assumeTrue(method in implementedTypeChecks) {
          "check '$method' is not implemented yet"
        }

        // language=javascript
        """
          const subject = null;
          types.$method(subject);
        """
      })
    }

    allSpecificTypeCheckTests()
  }.asStream()
}

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
@file:Suppress("JSUnresolvedReference")

package elide.runtime.node

import com.oracle.truffle.js.runtime.builtins.JSPromiseObject
import org.graalvm.polyglot.Value
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.SortedSet
import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.exec.GuestExecution
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.node.util.DebugLoggerImpl
import elide.runtime.node.util.InertDebugLogger
import elide.runtime.node.util.MimeTypes
import elide.runtime.node.util.NodeUtilModule
import elide.testing.annotations.TestCase

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
}

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
package elide.runtime.node

import com.oracle.truffle.js.runtime.builtins.JSPromiseObject
import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.exec.GuestExecution
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.node.util.NodeUtilModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `util` built-in module. */
@TestCase internal class NodeUtilTest : NodeModuleConformanceTest<NodeUtilModule>() {
  override val moduleName: String get() = "util"
  override fun provide(): NodeUtilModule = NodeUtilModule(GuestExecutorProvider {
    GuestExecution.direct()
  })

  @Inject lateinit var util: NodeUtilModule

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
}

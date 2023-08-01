@file:Suppress("JSUnusedLocalSymbols")

package elide.runtime.gvm.internals.js

import org.graalvm.polyglot.Value
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.*
import elide.runtime.gvm.ExecutableScript.ScriptSource
import elide.runtime.gvm.internals.GVMInvocationBindings
import elide.runtime.gvm.js.AbstractJsTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for resolving invocation bindings from JavaScript guest code. */
@TestCase internal class JsInvocationBindingsTest : AbstractJsTest() {
  // Stubbed literal script.
  private val script = JsExecutableScript.of(
    ScriptSource.LITERAL,
    JsExecutableScript.JS_MODULE,
    "/* (stubbed) */",
  )

  private fun applyAssertsExampleModule(evaluated: Value?) {
    assertNotNull(evaluated, "should not get `null` from evaluation of ESM module with exports")
    assertFalse(evaluated.isNull)
    assertTrue(evaluated.hasMembers(), "evaluated module should have members")
    val keys = evaluated.memberKeys
    assertNotNull(keys)
    assertTrue(keys.isNotEmpty())
    assertTrue(keys.contains("example"))
    val obj = evaluated.getMember("example")
    assertNotNull(obj, "should be able to `getMember` for exported JS module member")
    assertTrue(obj.hasMembers(), "exported JS module member should have members")
    val objKeys = obj.memberKeys
    assertNotNull(objKeys)
    assertTrue(objKeys.isNotEmpty())
    assertTrue(objKeys.contains("hello"))
    val hello = obj.getMember("hello")
    assertNotNull(hello, "should be able to `getMember` for exported JS module member")
    assertTrue(hello.isString, "exported JS module member should be a string")
    assertTrue(hello.asString().isNotEmpty())
    assertEquals("Hi", hello.asString(), "exported string should be expected value for ESM module")
  }

  private fun applyAssertsExampleDefaultExport(evaluated: Value?) {
    assertNotNull(evaluated, "should not get `null` from evaluation of ESM module with exports")
    assertFalse(evaluated.isNull)
    assertTrue(evaluated.hasMembers(), "evaluated module should have members")
    val keys = evaluated.memberKeys
    assertNotNull(keys)
    assertTrue(keys.isNotEmpty())
    assertTrue(keys.contains("default"))
    val obj = evaluated.getMember("default")
    val objKeys = obj.memberKeys
    assertNotNull(objKeys)
    assertTrue(objKeys.isNotEmpty())
    assertTrue(objKeys.contains("hello"))
    val hello = obj.getMember("hello")
    assertNotNull(hello, "should be able to `getMember` for exported default JS module member")
    assertTrue(hello.isString, "exported JS module member should be a string")
    assertTrue(hello.asString().isNotEmpty())
    assertEquals("Hi", hello.asString(), "exported string should be expected value for ESM module")
  }

  @Test fun testEvaluateExports() = executeESM {
    // language=javascript
    """
      export const example = {"hello": "Hi"};
    """
  }.thenAssert {
    applyAssertsExampleModule(it.returnValue())
  }

  @Test fun testEvaluateExportDefault() = executeESM {
    // language=javascript
    """
      export const example = {"hello": "Hi"};
      export default example;
    """
  }.thenAssert {
    applyAssertsExampleModule(it.returnValue())
    applyAssertsExampleDefaultExport(it.returnValue())
  }

  @Test fun testEvaluateExportFn() = executeESM {
    // language=javascript
    """
      export const example = {"hello": "Hi"};
      export default function() {
        return example;
      }
    """
  }.thenAssert {
    applyAssertsExampleModule(it.returnValue())
    val mod = it.returnValue()
    assertNotNull(mod)
    assertTrue(mod.hasMembers())
    val keys = mod.memberKeys
    assertNotNull(keys)
    assertTrue(keys.isNotEmpty())
    assertTrue(keys.contains("default"))
    val fn = mod.getMember("default")
    assertNotNull(fn)
    assertTrue(fn.canExecute(), "should be able to execute a default exported function")
  }

  @Test fun testEvaluateExportNamedFn() = executeESM {
    // language=javascript
    """
      export const example = {"hello": "Hi"};
      export default function something() {
        return example;
      }
    """
  }.thenAssert {
    applyAssertsExampleModule(it.returnValue())
    val mod = it.returnValue()
    assertNotNull(mod)
    assertTrue(mod.hasMembers())
    val keys = mod.memberKeys
    assertNotNull(keys)
    assertTrue(keys.isNotEmpty())
    assertTrue(keys.contains("default"))
    val fn = mod.getMember("default")
    assertNotNull(fn)
    assertTrue(fn.canExecute(), "should be able to execute a default exported named function")
  }

  @Test fun testEvaluateExportFetch() = executeESM {
    // language=javascript
    """
      export default {
        fetch: function() {
          return true;  // stubbed, should be a response
        }
      };
    """
  }.thenAssert {
    val mod = it.returnValue()
    assertNotNull(mod, "should be able to evaluate an ESM module and obtain the return value")
    assertTrue(mod.hasMembers(), "should be able to obtain members from the return value")
    val keys = mod.memberKeys
    assertNotNull(keys)
    assertTrue(keys.isNotEmpty())
    assertTrue(keys.contains("default"))
    val entrypoint = mod.getMember("default")
    assertNotNull(entrypoint, "should be able to obtain the default export from the module")
    assertFalse(entrypoint.canExecute())
    assertTrue(entrypoint.hasMembers())
    val entrypointKeys = entrypoint.memberKeys
    assertNotNull(entrypointKeys)
    assertTrue(entrypointKeys.isNotEmpty())
    assertTrue(entrypointKeys.contains("fetch"))
    val fetchFn = entrypoint.getMember("fetch")
    assertNotNull(fetchFn, "should be able to obtain the `fetch` member from the default export")
    assertTrue(fetchFn.canExecute())
    val exec = fetchFn.execute()
    assertNotNull(exec)
    assertTrue(exec.isBoolean)
    assertTrue(exec.asBoolean())
  }

  @Test fun testBindingsResolveFunction() = executeESM {
    // language=javascript
    """
      export default function something() {
        return "i am a cool result";
      }
    """
  }.thenAssert {
    val exec = it.returnValue() ?: error("Failed to resolve return value from guest script for binding resolution test")
    val resolved = assertDoesNotThrow {
      JsInvocationBindings.entrypoint(
        script,
        exec,
      )
    }
    assertNotNull(resolved, "should not get `null` from `JsInvocationBindings.entrypoint`")
    assertNotNull(resolved.bindings.mapped)
    assertNotNull(resolved.bindings.types)
    val bindings = resolved.bindings
    assertTrue(bindings is JsInvocationBindings.JsFunction, "resolved binding should be JS function")
    assertEquals(JsInvocationBindings.JsEntrypointType.FUNCTION, bindings.types.first())
  }

  @Test fun testBindingsResolveAsyncFunction() = executeESM {
    // language=javascript
    """
      export default async function something() {
        return "i am a cool result";
      }
    """
  }.thenAssert {
    val exec = it.returnValue() ?: error("Failed to resolve return value from guest script for binding resolution test")
    val resolved = assertDoesNotThrow {
      JsInvocationBindings.entrypoint(
        script,
        exec,
      )
    }
    assertNotNull(resolved, "should not get `null` from `JsInvocationBindings.entrypoint`")
    val bindings = resolved.bindings
    assertTrue(bindings is JsInvocationBindings.JsFunction, "resolved binding should be JS function")
    assertEquals(JsInvocationBindings.JsEntrypointType.ASYNC_FUNCTION, bindings.types.first())
  }

  @Test fun testBindingsResolveServer() = executeESM {
    // language=javascript
    """
      export default {
        async fetch(request) {
          return true;  // stubbed, should be a response
        }
      };
    """
  }.thenAssert {
    val exec = it.returnValue() ?: error("Failed to resolve return value from guest script for binding resolution test")
    val resolved = assertDoesNotThrow {
      JsInvocationBindings.entrypoint(
        script,
        exec,
      )
    }
    assertNotNull(resolved, "should not get `null` from `JsInvocationBindings.entrypoint`")
    val bindings = resolved.bindings
    assertTrue(bindings is JsInvocationBindings.JsServer, "resolved binding should be JS server")
    assertEquals(JsInvocationBindings.JsEntrypointType.SERVER, bindings.types.first())
  }

  @Test fun testBindingsResolveRender() = executeESM {
    // language=javascript
    """
      export default {
        async render(request, responder, context) {
          return true;  // stubbed, should be a response
        }
      };
    """
  }.thenAssert {
    val exec = it.returnValue() ?: error("Failed to resolve return value from guest script for binding resolution test")
    val resolved = assertDoesNotThrow {
      JsInvocationBindings.entrypoint(
        script,
        exec,
      )
    }
    assertNotNull(resolved, "should not get `null` from `JsInvocationBindings.entrypoint`")
    val bindings = resolved.bindings
    assertTrue(bindings is JsInvocationBindings.JsRender, "resolved binding should be JS render")
    assertEquals(JsInvocationBindings.JsEntrypointType.RENDER, bindings.types.first())
  }

  @Test fun testBindingsResolveCompound() = executeESM {
    // language=javascript
    """
      export default {
        // server fetch responder
        async fetch(request) {
          return true;  // stubbed, should be a response
        },

        // SSR render function
        async render(request, responder, context) {
          return true;  // stubbed, should be a response
        }
      };
    """
  }.thenAssert {
    val exec = it.returnValue() ?: error("Failed to resolve return value from guest script for binding resolution test")
    val resolved = assertDoesNotThrow {
      JsInvocationBindings.entrypoint(
        script,
        exec,
      )
    }
    assertNotNull(resolved, "should not get `null` from `JsInvocationBindings.entrypoint`")
    val bindings = resolved.bindings
    assertTrue(bindings is JsInvocationBindings.JsCompound, "resolved binding should be JS compound")
    assertTrue(
      bindings.supported().contains(GVMInvocationBindings.DispatchStyle.RENDER),
      "compound binding should declare support for `RENDER` entrypoint"
    )
    assertTrue(
      bindings.supported().contains(GVMInvocationBindings.DispatchStyle.SERVER),
      "compound binding should declare support for `SERVER` entrypoint"
    )
  }

  @Test fun testBindingsRenderKotlinJSStyle() = executeESM {
    // language=javascript
    """
      export async function render(reques, responder, context) {  // intentionally not default
        return true;  // stubbed, should be a response
      };
    """
  }.thenAssert {
    val exec = it.returnValue() ?: error("Failed to resolve return value from guest script for binding resolution test")
    val resolved = assertDoesNotThrow {
      JsInvocationBindings.entrypoint(
        script,
        exec,
      )
    }
    assertNotNull(resolved, "should not get `null` from `JsInvocationBindings.entrypoint`")
    val bindings = resolved.bindings
    assertTrue(bindings is JsInvocationBindings.JsRender, "resolved binding should be JS render")
    assertEquals(JsInvocationBindings.JsEntrypointType.RENDER, bindings.types.first())
  }

  @Test fun testBindingServerFailsIfNotAsync() = executeESM {
    // language=javascript
    """
      export default {
        fetch(request) {  // intentionally not async
          return true;  // stubbed, should be a response
        }
      };
    """
  }.thenAssert {
    val exec = it.returnValue() ?: error("Failed to resolve return value from guest script for binding resolution test")
    assertFailsWith<IllegalStateException> {
      JsInvocationBindings.entrypoint(
        script,
        exec,
      )
    }
  }

  @Test fun testBindingServerFailsIfNotDefault() = executeESM {
    // language=javascript
    """
      export async function fetch(request) {  // intentionally not default
        return true;  // stubbed, should be a response
      };
    """
  }.thenAssert {
    val exec = it.returnValue() ?: error("Failed to resolve return value from guest script for binding resolution test")
    assertFailsWith<IllegalStateException> {
      JsInvocationBindings.entrypoint(
        script,
        exec,
      )
    }
  }

  @Test fun testBindingFailsIfExotic() = executeESM {
    // language=javascript
    """
      5;
    """
  }.thenAssert {
    val exec = it.returnValue() ?: error("Failed to resolve return value from guest script for binding resolution test")
    assertFailsWith<IllegalStateException> {
      JsInvocationBindings.entrypoint(
        script,
        exec,
      )
    }
  }

  @Test fun testBindingFailsIfExoticDefault() = executeESM {
    // language=javascript
    """
      export default 5;
    """
  }.thenAssert {
    val exec = it.returnValue() ?: error("Failed to resolve return value from guest script for binding resolution test")
    assertFailsWith<IllegalStateException> {
      JsInvocationBindings.entrypoint(
        script,
        exec,
      )
    }
  }

  @Test fun testBindingFailsIfExoticNonFunction() = executeESM {
    // language=javascript
    """
      export default {
        fetch: 5
      };
    """
  }.thenAssert {
    val exec = it.returnValue() ?: error("Failed to resolve return value from guest script for binding resolution test")
    assertFailsWith<IllegalStateException> {
      JsInvocationBindings.entrypoint(
        script,
        exec,
      )
    }
  }
}

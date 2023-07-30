package elide.runtime.gvm.internals.intrinsics.js.express

import elide.runtime.gvm.internals.intrinsics.js.JsProxy
import elide.testing.annotations.Test
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExpressAppIntrinsicTest {
  @Test fun testRouteMatchers() {
    // helper array used to generate test cases
    val methods = arrayOf("GET", "POST", "PUT", "DELETE", "UPDATE", "PATCH", "OPTIONS", "HEAD")

    fun emptyProxy(): ProxyObject = JsProxy.build { putObject("params") }
    fun testAll(vararg paths: String, block: (path: String, method: String) -> Unit) {
      methods.zip(paths).forEach { (method, path) -> block(path, method) }
    }

    // global matcher
    val globalMatcher = ExpressAppIntrinsic.requestMatcher()
    testAll("/", "/hello", "/hello/name") { path, method ->
      assertTrue(
        actual = globalMatcher(path, method, emptyProxy()),
        message = "global matcher should match $path",
      )
    }
    
    // specific matcher
    val basicMatcher = ExpressAppIntrinsic.requestMatcher("/hello")
    testAll("/", "/abc", "/hello/nested") { path, method ->
      assertFalse(
        actual = basicMatcher(path, method, emptyProxy()),
        message = "basic matcher should not match $path",
      )
    }
    
    testAll("/hello") { path, method ->
      assertTrue(
        actual = basicMatcher(path, method, emptyProxy()),
        message = "basic matcher should match $path",
      )
    }
    
    // method matchers
    methods.forEach { method ->
      // method-specific matcher
      val matcher = ExpressAppIntrinsic.requestMatcher(method = method)
      
      assertTrue(
        actual = matcher("/", method, emptyProxy()),
        message = "method matcher should match path '/' with method $method"
      )
      
      // don't use an actual HTTP method, otherwise it will match in some cases
      assertFalse(
        actual = matcher("/", "TEST", emptyProxy()),
        message = "method matcher not should match path '/' with method $method"
      )
    }
    
    // path + method matchers
    methods.forEach { matcherMethod ->
      val matcher = ExpressAppIntrinsic.requestMatcher("/hello", matcherMethod)
      
      testAll("/", "/abc", "/hello/nested") { path, method ->
        assertFalse(
          actual = matcher(path, method, emptyProxy()),
          message = "compound matcher should not match $path",
        )
      }

      testAll("/hello") { path, method ->
        assertEquals(
          expected = method == matcherMethod,
          actual = matcher(path, method, emptyProxy()),
          message = "should${if(method == matcherMethod) "" else " (not) "}match $path with method $method",
        )
      }
    }
    
    // path variable matchers
    val variableMatcher = ExpressAppIntrinsic.requestMatcher("/hello/:name")
    testAll("/", "/hello", "/abc") { path, method ->
      assertFalse(
        actual = variableMatcher(path, method, emptyProxy()),
        message = "variable matcher should not match $path",
      )
    }
    
    testAll("/hello/value") { path, method ->
      val proxy = emptyProxy()
      val params = proxy.getMember("params") as ProxyObject

      assertTrue(
        actual = variableMatcher(path, method, proxy),
        message = "variable matcher should match $path",
      )
      
      assertTrue(
        actual = params.hasMember("name"),
        message = "variable matcher should set the value in the proxy for path $path"
      )
      
      assertEquals(
        expected = "value",
        actual = (params.getMember("name") as? Value)?.asString(),
        message = "variable matcher should extract the value for path $path"
      )
    }
  }
}
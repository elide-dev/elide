package elide.runtime.gvm.internals.js

import elide.annotations.Inject
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import io.micronaut.context.BeanContext
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

/** Tests for the V3 JS runtime implementation, on top of GraalVM. */
@TestCase class JsRuntimeTest {
  // JS runtime singleton.
  @Inject internal lateinit var runtime: JsRuntime.JsRuntimeFactory

  // Micronaut bean context.
  @Inject internal lateinit var beanContext: BeanContext

  @Test fun testInjectable() {
    assertNotNull(runtime, "should be able to inject JS runtime instance")
  }

  @Test fun testSingleton() {
    assertNotNull(runtime, "should be able to inject JS runtime factory instance")
    assertNotNull(beanContext, "should be able to inject bean context")
    val first = runtime.acquire()
    assertNotNull(first, "should be able to create JS runtime instance")
    val anotherOne = beanContext.getBean(JsRuntime.JsRuntimeFactory::class.java)
    assertNotNull(anotherOne, "should be able to acquire another injected handle to JS runtime factory")
    assertTrue(anotherOne === runtime, "should get exact same object (singleton)")

    val anotherRuntime = anotherOne.acquire()
    assertNotNull(anotherRuntime, "should be able to acquire another injected handle to JS runtime")
    assertTrue(anotherRuntime === first, "should get exact same object (singleton)")
  }
}

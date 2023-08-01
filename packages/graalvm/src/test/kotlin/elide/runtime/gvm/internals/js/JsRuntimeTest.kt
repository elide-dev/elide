package elide.runtime.gvm.internals.js

import io.micronaut.context.BeanContext
import org.junit.jupiter.api.Assertions.assertNotNull
import elide.annotations.Inject
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for the V3 JS runtime implementation, on top of GraalVM. */
@TestCase class JsRuntimeTest {
  // JS runtime singleton.
  @Inject internal lateinit var runtime: JsRuntime

  // Micronaut bean context.
  @Inject internal lateinit var beanContext: BeanContext

  @Test fun testInjectable() {
    assertNotNull(runtime, "should be able to inject JS runtime instance")
  }

  @Test fun testSingleton() {
    assertNotNull(runtime, "should be able to inject JS runtime factory instance")
    assertNotNull(beanContext, "should be able to inject bean context")
    assertNotNull(runtime, "should be able to create JS runtime instance")
  }
}

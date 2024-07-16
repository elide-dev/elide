package elide.runtime.gvm.internals.js.node

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.js.AbstractJsTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

@TestCase
@OptIn(DelicateElideApi::class)
internal class NodeBufferTypeTest : AbstractJsTest() {
  @Test fun `should alloc buffer from string`() = executeGuest {
    // language=javascript
    """
      const buf = Buffer.from("hello 🙂")
      export const result = buf.toString()
    """
  }.thenAssert {
    val module = assertNotNull(it.returnValue())
    assertTrue(module.hasMembers())

    val result = assertNotNull(module.getMember("result"))
    assertTrue(result.isString)

    assertEquals("hello 🙂", result.asString())
  }
  
  @Test fun `should alloc buffer from string (built-in module)`() = executeGuest {
    // language=javascript
    """
      const { Buffer } = require("node:buffer")
      
      const buf = Buffer.from("hello 🙂")
      export const result = buf.toString()
    """
  }.thenAssert {
    val module = assertNotNull(it.returnValue())
    assertTrue(module.hasMembers())

    val result = assertNotNull(module.getMember("result"))
    assertTrue(result.isString)

    assertEquals("hello 🙂", result.asString())
  }
}

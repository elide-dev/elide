package sample

import kotlin.test.*

class SampleTest {
  @Test fun testGreeting() {
    val greeting = render_greeting()
    assertNotNull(greeting)
    assertEquals("Hello World", greeting)
  }
}

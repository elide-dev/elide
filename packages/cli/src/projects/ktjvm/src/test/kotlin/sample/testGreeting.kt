package sample

import kotlin.test.*

@Test fun testGreeting() {
  val greeting = render_greeting()
  assertNotNull(greeting)
  assertEquals("Hello, World!", greeting)
}

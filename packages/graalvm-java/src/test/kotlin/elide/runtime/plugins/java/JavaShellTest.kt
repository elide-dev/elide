package elide.runtime.plugins.java

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.plugins.java.shell.GuestJavaInterpreter

@OptIn(DelicateElideApi::class) class JavaShellTest {
  /** Acquire a [PolyglotEngine] configured with the [Java] plugin. */
  private fun configureEngine() = PolyglotEngine {
    install(Java)
  }

  @Test fun testInteractiveJava() {
    val context = configureEngine().acquire()
    val interpreter = GuestJavaInterpreter(context)

    assertDoesNotThrow("should allow variable declaration") {
      interpreter.evaluate("int a = 2;")
    }

    assertDoesNotThrow("should allow references to declared variables") {
      interpreter.evaluate("int b = a + 3;")
    }

    assertEquals(
      expected = "5",
      actual = interpreter.evaluate("b;"),
      message = "should return value of declared variable.",
    )
  }
}
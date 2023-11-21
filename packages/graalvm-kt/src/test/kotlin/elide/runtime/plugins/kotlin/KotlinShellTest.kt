package elide.runtime.plugins.kotlin

import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.plugins.kotlin.shell.GuestKotlinInterpreter
import elide.testing.annotations.Test

@OptIn(DelicateElideApi::class) class KotlinShellTest {
  /** Acquire a [PolyglotEngine] configured with the [Kotlin] plugin. */
  private fun configureEngine() =PolyglotEngine {
    install(Kotlin)
  }

  @Test fun testInteractiveKotlin() {
    val context = configureEngine().acquire()
    val interpreter = GuestKotlinInterpreter(context)

    assertDoesNotThrow("should allow variable declaration") {
      interpreter.evaluate("val a = 2")
    }

    assertDoesNotThrow("should allow references to declared variables") {
      interpreter.evaluate("val b = a + 3")
    }

    assertEquals(
      expected = 5,
      actual = interpreter.evaluate("b")?.asInt(),
      message = "should return value of previously declared variable",
    )
  }
}
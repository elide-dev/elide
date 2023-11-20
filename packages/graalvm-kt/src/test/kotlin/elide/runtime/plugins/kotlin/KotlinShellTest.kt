package elide.runtime.plugins.kotlin

import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertEquals
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.core.PolyglotValue
import elide.runtime.plugins.kotlin.shell.GuestKotlinScriptEngineFactory
import elide.testing.annotations.Test

@OptIn(DelicateElideApi::class) class KotlinShellTest {
  /** Acquire a [PolyglotEngine] configured with the [Kotlin] plugin. */
  private fun configureEngine() = PolyglotEngine {
    install(Kotlin)
  }

  @Test fun testInteractiveKotlin() {
    val context = configureEngine().acquire()
    val scriptEngine = GuestKotlinScriptEngineFactory(context).scriptEngine

    assertDoesNotThrow("should allow variable declaration") {
      scriptEngine.eval("val a = 2")
    }

    assertDoesNotThrow("should allow references to declared variables") {
      scriptEngine.eval("val b = a + 3")
    }

    assertEquals(
      expected = 5,
      actual = (scriptEngine.eval("b") as PolyglotValue).asInt(),
      message = "should return value of previously declared variable",
    )
  }
}
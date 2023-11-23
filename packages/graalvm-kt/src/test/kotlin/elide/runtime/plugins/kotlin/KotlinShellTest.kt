package elide.runtime.plugins.kotlin

import org.graalvm.polyglot.Source
import org.graalvm.polyglot.io.ByteSequence
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.plugins.kotlin.shell.GuestKotlinEvaluator
import elide.testing.annotations.Test

@OptIn(DelicateElideApi::class) class KotlinShellTest {
  /** Acquire a [PolyglotEngine] configured with the [Kotlin] plugin. */
  private fun configureEngine() = PolyglotEngine {
    install(Kotlin)
  }

  @Test fun testKotlinInterpreter() {
    val context = configureEngine().acquire()
    val interpreter = GuestKotlinEvaluator(context)

    fun String.asInteractiveSource() = Source.newBuilder(Kotlin.languageId, this, "snippet.kts")
      .interactive(true)
      .build()

    assertDoesNotThrow("should allow variable declaration") {
      interpreter.evaluate("val a = 2".asInteractiveSource(), context)
    }

    assertDoesNotThrow("should allow references to declared variables") {
      interpreter.evaluate("val b = a + 3".asInteractiveSource(), context)
    }

    assertEquals(
      expected = 5,
      actual = interpreter.evaluate("b".asInteractiveSource(), context).asInt(),
      message = "should return value of previously declared variable",
    )
  }

  @Test fun testRejectInvalidSources() {
    val context = configureEngine().acquire()
    val interpreter = GuestKotlinEvaluator(context)

    assertThrows<IllegalArgumentException>("should reject binary sources") {
      val binarySource =
        Source.newBuilder(Kotlin.languageId, ByteSequence.create(ByteArray(10)), "invalid.class")
          .interactive(true)
          .build()

      interpreter.evaluate(binarySource, context)
    }
  }

  @Test fun testEvaluateKotlinInteractive() {
    val context = configureEngine().acquire()
    val source = Source.newBuilder(Kotlin.languageId, "5.let { it * 5 }", "interactive.kt")
      .interactive(true)
      .build()

    val result = assertDoesNotThrow("should allow evaluating java source code") {
      context.evaluate(source)
    }

    assertEquals(
      expected = 25,
      actual = result.asInt(),
      message = "should return wrapped result",
    )
  }

  @Test fun testEvaluateKotlinScript() {
    val context = configureEngine().acquire()
    val scriptFile = File.createTempFile("elide", ".kts")
    scriptFile.writeText(
      """
      fun someNumber(): Int {
        return 5
      }
      
      val result = someNumber() + 10
      """.trimIndent(),
    )

    val source = Source.newBuilder(Kotlin.languageId, scriptFile).build()
    val result = assertDoesNotThrow("should allow evaluating kotlin script") {
      context.evaluate(source)
    }

    assertFalse(result.isNull, message = "should return script instance")
    assertTrue(result.hasMember("someNumber"), message = "should have declared functions")
    assertTrue(result.hasMember("getResult"), message = "should have declared variables")
  }
}

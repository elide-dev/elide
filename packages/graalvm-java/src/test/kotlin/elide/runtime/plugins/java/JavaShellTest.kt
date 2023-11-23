package elide.runtime.plugins.java

import org.graalvm.polyglot.Source
import org.graalvm.polyglot.io.ByteSequence
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.plugins.java.shell.GuestJavaEvaluator

@OptIn(DelicateElideApi::class) class JavaShellTest {
  /** Acquire a [PolyglotEngine] configured with the [Java] plugin. */
  private fun configureEngine() = PolyglotEngine {
    install(Java)
  }

  @Test fun testJavaInterpreter() {
    val context = configureEngine().acquire()
    val interpreter = GuestJavaEvaluator(context)

    fun String.asSource() = Source.newBuilder(Java.languageId, this, "snippet.java")
      .interactive(true)
      .build()

    assertDoesNotThrow("should allow variable declaration") {
      interpreter.evaluate("int a = 2;".asSource(), context)
    }

    assertDoesNotThrow("should allow references to declared variables") {
      interpreter.evaluate("int b = a + 3;".asSource(), context)
    }

    assertEquals(
      expected = "5",
      actual = interpreter.evaluate("b;".asSource(), context).asString(),
      message = "should return value of declared variable.",
    )
  }

  @Test fun testSourceValidation() {
    val context = configureEngine().acquire()
    val interpreter = GuestJavaEvaluator(context)

    val validSource = Source.newBuilder(Java.languageId, "int a = 5;", "snippet.java").interactive(true).build()
    assertTrue(interpreter.accepts(validSource), "should accept interactive character-based sources")

    val binarySource = Source.newBuilder(Java.languageId, ByteSequence.create(ByteArray(10)), "invalid.class")
      .interactive(true)
      .build()

    assertFalse(interpreter.accepts(binarySource), "should not accept binary sources")
    assertThrows<IllegalArgumentException>("should reject binary sources") {
      interpreter.evaluate(binarySource, context)
    }

    val nonInteractiveSource = Source.newBuilder(Java.languageId, "...", "invalid.java")
      .interactive(false)
      .build()

    assertFalse(interpreter.accepts(nonInteractiveSource), "should not non-interactive sources")
    assertThrows<IllegalArgumentException>("should reject non-interactive sources") {
      interpreter.evaluate(nonInteractiveSource, context)
    }
  }

  @Test fun testEvaluateJava() {
    val context = configureEngine().acquire()
    val source = Source.newBuilder(Java.languageId, "Math.abs(-5)", "interactive.java")
      .interactive(true)
      .build()

    val result = assertDoesNotThrow("should allow evaluating java source code") {
      context.evaluate(source)
    }

    assertEquals(
      expected = "5",
      actual = result.asString(),
      message = "should return wrapped result",
    )
  }
}

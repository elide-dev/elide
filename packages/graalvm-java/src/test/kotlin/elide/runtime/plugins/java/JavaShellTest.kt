package elide.runtime.plugins.java

import jdk.jshell.JShell
import jdk.jshell.SnippetEvent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotEngine
import elide.runtime.plugins.java.shell.GuestExecutionControlProvider

@OptIn(DelicateElideApi::class) class JavaShellTest {
  /** Acquire a [PolyglotEngine] configured with the [Java] plugin. */
  private fun configureEngine() = PolyglotEngine {
    install(Java)
  }

  /**
   * Attempt to "interpret" this event, throwing an exception if it represents an error, or returning a value if it
   * provides one.
   */
  private fun SnippetEvent.interpret(): String? {
    exception()?.let { throw it }
    return this.value()
  }

  /** Throw any exceptions represented by events in this list. */
  private fun List<SnippetEvent>.interpret() {
    forEach { it.interpret() }
  }

  @Test fun testInteractiveJava() {
    val context = configureEngine().acquire()
    val provider = GuestExecutionControlProvider(context)

    val shell = JShell.builder()
      .executionEngine(provider, mutableMapOf())
      .build()

    assertDoesNotThrow("should allow variable declaration") {
      shell.eval("int a = 2").interpret()
    }

    assertDoesNotThrow("should allow references to declared variables") {
      shell.eval("int b = a + 3").interpret()
    }
  }
}
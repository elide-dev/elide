package elide.runtime.plugins.java.shell

import jdk.jshell.JShell
import jdk.jshell.Snippet.Status.DROPPED
import jdk.jshell.Snippet.Status.REJECTED
import jdk.jshell.SnippetEvent
import kotlin.streams.asSequence
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext

@DelicateElideApi public class GuestJavaInterpreter(private val context: PolyglotContext) {
  /** A cached [JShell] instance configured with the [GuestExecutionProvider], using the [context] for execution. */
  private val shell: JShell by lazy {
    JShell.builder().executionEngine(
      /* executionControlProvider = */ GuestExecutionProvider(context),
      /* executionControlParameters = */ mapOf(),
    ).build()
  }

  /**
   * Attempt to "interpret" this [event], throwing an exception if it represents an error, or returning a value if it
   * provides one.
   */
  private fun interpret(event: SnippetEvent): String? = when (event.status()) {
    DROPPED, REJECTED -> {
      val diagnostics = shell.diagnostics(event.snippet()).asSequence().joinToString("\n") {
        it.getMessage(null)
      }

      error("Snippet evaluation resulted in error: $diagnostics")
    }

    else -> {
      event.exception()?.let { throw it }
      event.value()
    }
  }

  /**
   * Compile and evaluate a [snippet] of Java code, returning the result as a string, `null` if it does not return a
   * value, or throwing an exception if an error occurs during compilation or evaluation.
   *
   * There is currently no support for return values of a type other than [String] due to limitations in the
   * underlying [JShell] engine.
   *
   * @param snippet A valid fragment of Java code to be evaluated.
   * @return A [String] representing the result value of the [snippet], or `null` if no value is returned.
   */
  public fun evaluate(snippet: String): String? {
    // JShell will return a list of snippet events; we need to interpret those, throwing any exceptions, etc.,
    // and then select the last event that has a non-null value
    return shell.eval(snippet).map(::interpret).lastOrNull { it != null }
  }
}
/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.plugins.java.shell

import jdk.jshell.JShell
import jdk.jshell.Snippet.Status.DROPPED
import jdk.jshell.Snippet.Status.REJECTED
import jdk.jshell.SnippetEvent
import kotlin.streams.asSequence
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext

/**
 * An interactive Java interpreter capable of compiling and evaluating snippets of code, for use in a REPL.
 *
 * Use the [evaluate] method to execute a Java snippet and receive the result.
 *
 * This class uses [JShell] with a [GuestExecutionControl] backend, which wraps around a guest [context] to execute
 * code using Espresso. Some current limitations apply:
 * - Startup is currently very slow: both because of Espresso's experimental state *and* JShell's normal overhead.
 * - Return values are not supported yet, since JShell only returns [String] as an evaluation result.
 *
 * Note that while a [GuestExecutionProvider] is implemented as part of the internal pipeline, it cannot be used with
 * JShell's service loader, since it requires a [PolyglotContext] as a constructor argument.
 *
 * #### How it works
 *
 * JShell separates snippet compilation from execution, allowing custom execution backends to be implemented. The
 * default implementation simply runs the snippet in the same JVM, while others may choose to send snippets over the
 * network or to a different process for execution.
 *
 * In this case, the [GuestExecutionControl] implementation wraps around a `LocalExecutionControl` instance created in
 * a guest context, and delegates all method calls to that instance. This means snippets are executed in the Espresso
 * context rather than the host.
 *
 * @see GuestExecutionProvider
 * @see GuestExecutionControl
 */
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

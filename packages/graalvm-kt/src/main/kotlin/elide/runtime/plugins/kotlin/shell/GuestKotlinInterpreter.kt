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

package elide.runtime.plugins.kotlin.shell

import org.jetbrains.kotlin.cli.common.repl.GenericReplCompilingEvaluatorBase
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult.*
import org.jetbrains.kotlin.cli.common.repl.ReplFullEvaluator
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.jvmhost.repl.JvmReplCompiler
import kotlin.script.experimental.jvmhost.repl.JvmReplEvaluator
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue

/**
 * An interactive Kotlin interpreter capable of compiling and evaluating snippets of code, for use in a REPL. Use the
 * [evaluate] method to execute a Kotlin snippet and receive the result as a [PolyglotValue].
 *
 * This class uses the experimental Kotlin Scripting engine internally, with a custom evaluator that wraps around a
 * guest context for execution.
 *
 * #### How it works
 *
 * The Kotlin Scripting engine allows defining custom evaluators to control execution of a script after compiling it.
 * In order to run the script in a guest context, a custom [GuestKotlinScriptEvaluator] is used to transfer the
 * compiled bytecode of the script class to the embedded context; then, a new instance of the class is created using
 * the polyglot bindings.
 *
 * Instances of the compiled script class execute the script in the constructor, which results
 * in the script being evaluated in the guest context. After instantiation, the result of the evaluation (if any) is
 * stored in a pre-defined field, which can then be read and returned as a [PolyglotValue].
 *
 * In particular, this class uses a [ReplFullEvaluator] with a [JvmReplCompiler] and a [JvmReplEvaluator], to correctly
 * process single statements rather than requiring full scripts.
 *
 * @see GuestKotlinScriptEvaluator
 */
@DelicateElideApi public class GuestKotlinInterpreter(private val context: PolyglotContext) {
  /** A REPL evaluator capable of compiling a snippet before evaluating it. */
  private val evaluator: ReplFullEvaluator by lazy { prepareEvaluator() }

  /** Container for the REPL state managed by the [evaluator]. */
  private val state by lazy { evaluator.createState() }

  /**
   * Prepares a [ReplFullEvaluator] instance with a custom JVM evaluator that uses the guest [context] to execute the
   * code snippets.
   */
  private fun prepareEvaluator(): ReplFullEvaluator {
    val compilationSettings = ScriptCompilationConfiguration()
    val evaluationSettings = ScriptEvaluationConfiguration()
    val guestEvaluator = GuestKotlinScriptEvaluator(context)

    return GenericReplCompilingEvaluatorBase(
      compiler = JvmReplCompiler(compilationSettings),
      evaluator = JvmReplEvaluator(evaluationSettings, guestEvaluator),
    )
  }

  /** Construct the next [ReplCodeLine] for the current REPL [state], using a text [snippet] as content. */
  private fun nextLine(snippet: String): ReplCodeLine {
    return ReplCodeLine(
      no = state.getNextLineNo(),
      generation = state.currentGeneration,
      code = snippet,
    )
  }

  /**
   * Compile and evaluate a [snippet] of Kotlin code, returning the result as a [PolyglotValue] if the code is an
   * expression or statement that returns a value, `null` if it does not return a value, or throwing an exception if
   * an error occurs during compilation or evaluation.
   *
   * Note that a Unit-returning expression (such as `val message = "Hello"`) will return `null`, but an expression
   * that returns null (such as `0.takeIf { it != 0 }`) will actually return a [PolyglotValue], encapsulating the
   * guest value of `null`.
   *
   * @param snippet A valid fragment of Kotlin code to be evaluated.
   * @return The result of the [snippet] evaluation, or `null` if it has no return value.
   */
  public fun evaluate(snippet: String): PolyglotValue? {
    // delegate to the evaluator and interpret the result
    return when (val result = evaluator.compileAndEval(state, nextLine(snippet))) {
      is UnitResult -> null
      is ValueResult -> {
        // simple sanity check; all values produced by the guest evaluator should have the correct value
        check(result.type == GuestKotlinScriptEvaluator.RETURN_TYPE_POLYGLOT_VALUE) {
          "Expected a the snippet result type to be 'PolyglotValue', received ${result.type}"
        }

        // if the value was packaged by the guest evaluator, it will be the correct type
        result.value as PolyglotValue
      }

      // map compilation/evaluation errors to exceptions
      is HistoryMismatch -> error("REPL history mismatch at line: ${result.lineNo}")
      is Incomplete -> error("Incomplete code snippet: ${result.message}")
      is Error.CompileTime -> error("Snippet compilation error at ${result.location}: ${result.message}")
      is Error.Runtime -> throw RuntimeException("Exception during snippet evaluation", result.cause)
    }
  }
}

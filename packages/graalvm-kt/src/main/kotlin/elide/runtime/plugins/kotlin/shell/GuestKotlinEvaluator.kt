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

import org.graalvm.polyglot.Source
import org.jetbrains.kotlin.cli.common.repl.GenericReplCompilingEvaluatorBase
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult.*
import org.jetbrains.kotlin.cli.common.repl.ReplFullEvaluator
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.valueOrNull
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.repl.JvmReplCompiler
import kotlin.script.experimental.jvmhost.repl.JvmReplEvaluator
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.GuestLanguageEvaluator
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
 * In order to run the script in a guest context, a custom [GuestScriptEvaluator] is used to transfer the
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
 * @see GuestScriptEvaluator
 */
@DelicateElideApi internal class GuestKotlinEvaluator(private val context: PolyglotContext) : GuestLanguageEvaluator {
  /** Shared evaluation configuration used by the scripting [host] and the [repl]. */
  private val evaluationConfig: ScriptEvaluationConfiguration by lazy { ScriptEvaluationConfiguration() }

  /** Shared compilation configuration used by the [repl]. */
  private val compilationConfig: ScriptCompilationConfiguration by lazy { ScriptCompilationConfiguration() }

  /** Guest evaluator used by the scripting [host] and the [repl]. */
  private val evaluator: GuestScriptEvaluator by lazy { GuestScriptEvaluator(context) }

  /** A scripting host for evaluating full Kotlin scripts. */
  private val host: BasicJvmScriptingHost by lazy {
    BasicJvmScriptingHost(evaluator = GuestScriptEvaluator(context))
  }

  /** A REPL evaluator capable of compiling a snippet before evaluating it. */
  private val repl: ReplFullEvaluator by lazy {
    GenericReplCompilingEvaluatorBase(
      compiler = JvmReplCompiler(compilationConfig),
      evaluator = JvmReplEvaluator(evaluationConfig, evaluator),
    )
  }

  /** Container for the REPL state managed by the [evaluator]. */
  private val replState by lazy { repl.createState() }

  /** Construct the next [ReplCodeLine] for the current REPL [replState], using a text [snippet] as content. */
  private fun nextLine(snippet: String): ReplCodeLine {
    return ReplCodeLine(
      no = replState.getNextLineNo(),
      generation = replState.currentGeneration,
      code = snippet,
    )
  }

  override fun accepts(source: Source): Boolean {
    return source.hasCharacters()
  }

  override fun evaluate(source: Source, context: PolyglotContext): PolyglotValue {
    require(accepts(source)) { "Only text-based sources are supported." }
    val sourceContents = source.characters.toString()

    return when (source.isInteractive) {
      // delegate to the REPL evaluator and interpret the result
      true -> when (val replResult = repl.compileAndEval(replState, nextLine(sourceContents))) {
        is UnitResult -> PolyglotValue.asValue(null)
        is ValueResult -> {
          // simple sanity check; all values produced by the guest evaluator should have the correct value
          check(replResult.type == GuestScriptEvaluator.RETURN_TYPE_POLYGLOT_VALUE) {
            "Expected a the snippet result type to be 'PolyglotValue', received ${replResult.type}"
          }

          // if the value was packaged by the guest evaluator, it will be the correct type
          replResult.value as PolyglotValue
        }

        // map compilation/evaluation errors to exceptions
        is HistoryMismatch -> error("REPL history mismatch at line: ${replResult.lineNo}")
        is Incomplete -> error("Incomplete code snippet: ${replResult.message}")
        is Error.CompileTime -> error("Snippet compilation error at ${replResult.location}: ${replResult.message}")
        is Error.Runtime -> throw RuntimeException("Exception during snippet evaluation", replResult.cause)
      }

      // evaluate the script using the jvm host
      false -> {
        val scriptResult = host.eval(
          script = sourceContents.toScriptSource(),
          compilationConfiguration = compilationConfig,
          evaluationConfiguration = evaluationConfig,
        )

        // return the script instance (similar to how evaluating esm sources returns a module)
        scriptResult.valueOrNull()?.returnValue?.scriptInstance as? PolyglotValue ?: PolyglotValue.asValue(null)
      }
    }
  }
}

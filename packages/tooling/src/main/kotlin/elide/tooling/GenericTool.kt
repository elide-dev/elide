/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

package elide.tooling

import java.io.PrintWriter
import java.io.StringWriter
import java.util.LinkedList
import java.util.SortedSet
import java.util.concurrent.atomic.AtomicLong
import elide.runtime.Logger

/**
 * ## Abstract Tool (Generic)
 */
public abstract class GenericTool<T, I, O>(info: Tool.CommandLineTool) : AbstractTool(info) {
  /**
   * Create the tool provider instance.
   *
   * @return Generic tool provider.
   */
  public abstract fun createTool(): T

  /**
   * Logger to use for this tool.
   */
  public abstract val toolLogger: Logger

  /**
   * Generic description for this task.
   */
  public abstract val taskDescription: String

  /**
   * Amend the arguments to be passed to the tool.
   *
   * @param args Arguments to amend.
   */
  public open fun amendArgs(args: MutableArguments): Unit = Unit

  /**
   * Implementation to run the generic tool.
   */
  public abstract fun toolRun(out: PrintWriter, err: PrintWriter, vararg args: String): Int

  /**
   * Wrapped execution of the tool itself, to customize before/after actions.
   *
   * @param block Block to execute
   */
  public open suspend fun <R> toolExec(block: suspend () -> R): R = block()

  /**
   * Inputs for this tool.
   */
  public abstract val inputs: I

  /**
   * Outputs for this tool.
   */
  public abstract val outputs: O

  /**
   * Resolve inputs for this tool.
   *
   * @return Resolved inputs.
   */
  public open fun resolveInputs(): I = inputs

  /**
   * Resolve outputs for this tool.
   *
   * @param out Standard output stream.
   * @param err Standard error stream.
   * @param ms Milliseconds since the tool started.
   * @return Resolved outputs.
   */
  public open suspend fun resolveOutputs(out: StringWriter, err: StringWriter, ms: Int): O = outputs

  // Supported by default.
  override fun supported(): Boolean = true

  override suspend fun invoke(state: EmbeddedToolState): Tool.Result {
    val args = MutableArguments.from(info.args).also { amendArgs(it) }
    toolLogger.debug { "Resolving inputs" }
    resolveInputs()
    val toolStart = System.currentTimeMillis()
    val toolEnd = AtomicLong(0L)
    val out = StringWriter()
    val outPrinter = PrintWriter(out, true)
    val err = StringWriter()
    val errPrinter = PrintWriter(err, true)
    val finalizdArgs = args.asArgumentList().toTypedArray()

    val exitCode = toolExec {
      @Suppress("TooGenericExceptionCaught", "SpreadOperator")
      try {
        toolLogger.debug { "Preparing generic tool execution task '$taskDescription'" }
        toolRun(outPrinter, errPrinter, *finalizdArgs)
      } catch (rxe: RuntimeException) {
        toolLogger.debug { "Exception while executing task: $rxe" }

        embeddedToolError(
          info,
          "$taskDescription failed: ${rxe.message}",
          cause = rxe,
        )
      } finally {
        toolLogger.debug { "$taskDescription job finished" }
        toolEnd.set(System.currentTimeMillis())
      }
    }

    out.flush()
    err.flush()
    val totalMs = toolEnd.get() - toolStart
    toolLogger.debug { "$taskDescription job completed in ${totalMs}ms (exit code: $exitCode)" }
    resolveOutputs(out, err, totalMs.toInt())
    return Tool.Result.Success
  }

  public companion object {
    // Gather options, inputs, and outputs for an invocation of the jar tool.
    @JvmStatic public fun gatherArgs(
      expectValues: SortedSet<String>,
      args: Arguments,
    ): Pair<LinkedList<Argument>, LinkedList<String>> {
      val effective = LinkedList<Argument>()
      val likelyInputs = LinkedList<String>()
      var nextIsValueForKey: String? = null

      for (arg in args.asArgumentList()) {
        if (nextIsValueForKey != null) {
          effective.add(Argument.of(nextIsValueForKey to arg))
          nextIsValueForKey = null
          continue
        }
        val argNormalized = arg.lowercase().trim()
        val argSplit = argNormalized.split('=')
        val argToken = argSplit.first()
        if (argToken in expectValues) {
          if (argSplit.size > 1) {
            // Split on `=`.
            val value = argSplit[1]
            effective.add(Argument.of(argToken to value))
          } else {
            // Next token is a value for this key.
            nextIsValueForKey = argToken
          }
        } else {
          // Just add the token as-is.
          effective.add(Argument.of(arg))
          if (!argToken.startsWith("-") && !argToken.startsWith("@")) {
            // this is a likely input
            likelyInputs.add(arg)
          }
        }
      }
      return (effective to likelyInputs)
    }
  }
}

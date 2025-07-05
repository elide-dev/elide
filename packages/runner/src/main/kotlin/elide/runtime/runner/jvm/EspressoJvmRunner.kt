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
package elide.runtime.runner.jvm

import org.graalvm.polyglot.Value
import elide.runtime.runner.AbstractRunner
import elide.runtime.runner.JvmRunner
import elide.runtime.runner.RunnerExecution
import elide.runtime.runner.RunnerOutcome
import elide.runtime.runner.TruffleRunner
import elide.tooling.Arguments

// Name of the Espresso runner.
private const val RUNNER_ESPRESSO = "espresso"

// Implements a JVM runner which uses Espresso on Truffle to run JVM bytecode.
internal class EspressoJvmRunner : AbstractRunner<JvmRunner.JvmRunnerJob>(RUNNER_ESPRESSO), JvmRunner, TruffleRunner {
  // Resolve the entrypoint main method to invoke.
  private fun resolveMain(guestCls: Value): Value? {
    return when (val mainEntry = guestCls.getMember("main/([Ljava/lang/String;)V")?.takeIf { it.canExecute() }) {
      null -> null
      else -> mainEntry
    }
  }

  // Invoke the main method and return a runner outcome.
  @Suppress("TooGenericExceptionCaught")
  private fun invokeMain(guestCls: Value, entry: Value, args: Arguments): RunnerOutcome {
    return try {
      entry.executeVoid(args.asArgumentList().toTypedArray())
      success()
    } catch (err: Throwable) {
      err(
        "failed to invoke main method in class '${guestCls.javaClass.name}': ${err.message ?: "unknown error"}",
        cause = err,
      )
    }
  }

  override suspend fun invoke(exec: RunnerExecution<JvmRunner.JvmRunnerJob>): RunnerOutcome {
    // start by resolving the main class
    val mainCls = exec.context
      .getBindings("java")
      .getMember(exec.job.mainClass)

    if (mainCls == null) {
      return err("main class not found: ${exec.job.mainClass}")
    }

    // next up, the entrypoint method
    val entry = resolveMain(mainCls)
    if (entry == null) {
      return err("main method not found in class: ${exec.job.mainClass}")
    }
    return invokeMain(mainCls, entry, exec.job.arguments)
  }
}

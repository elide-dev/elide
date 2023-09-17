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

@file:Suppress("MnInjectionPoints", "SuspendFunctionOnCoroutineScope")

package elide.tool.cli.cmd.selftest

import io.micronaut.context.BeanContext
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.stream.consumeAsFlow
import kotlin.streams.asSequence
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState
import elide.tool.cli.output.testRenderer
import elide.tool.testing.*
import elide.tool.testing.SelfTest.SelfTestContext
import elide.tool.testing.TestContext.TestStage
import elide.tool.testing.TestContext.TestStage.*
import elide.tool.testing.TestEvent.Type
import elide.tool.testing.TestResult.Result.*

/** Run tests embedded within the Elide runtime. */
@Command(
  name = "selftest",
  description = ["%nRun all self-tests for the runtime"],
  hidden = true,
  mixinStandardHelpOptions = true,
  showDefaultValues = true,
  abbreviateSynopsis = true,
  usageHelpAutoWidth = true,
)
@Singleton
internal class SelfTestCommand @Inject constructor (
  private val beanContext: BeanContext
) : AbstractSubcommand<ToolState, CommandContext>() {
  /** Test runner for self-tests. */
  private inner class SelfTestRunner : CommandTestRunner<SelfTest, SelfTestContext, CommandContext>() {
    override fun context(): SelfTestContext = buildSelfTestContext()
  }

  /** Implementation of context for self-tests. */
  internal class DefaultSelfTestContext private constructor() : SelfTestContext {
    companion object {
      @JvmStatic fun create(): SelfTestContext = DefaultSelfTestContext()
    }

    private val currentStage: AtomicReference<TestStage> = AtomicReference(PENDING)
    private val testResult: AtomicReference<TestResult> = AtomicReference(null)

    override val stage: TestStage get() = currentStage.get()
    override val result: AtomicReference<TestResult> get() = testResult.also {
      require(stage == DONE) {
        "Cannot use test result until it has completed running"
      }
    }

    override fun notify(stage: TestStage) {
      currentStage.set(stage)
    }

    override fun assignResult(testResult: TestResult) {
      currentStage.set(DONE)
      result.set(testResult)
    }

    override fun close() {
      // nothing
    }
  }

  /** Handles events from tests and turns them into output. */
  @Suppress("UNUSED") private inner class TestOutput (
    private val context: CommandContext,
  ) : TestEventListener<SelfTest, SelfTestContext>, AutoCloseable {
    private fun labelForTest(test: SelfTest): String {
      return test.name
    }

    override suspend fun onTestEvent(event: TestEvent<SelfTest, SelfTestContext>) {
      val prefix = when (event.type) {
        Type.EXECUTE -> "Running test".also {
          event.context.notify(EXECUTING)
        }

        Type.RESULT -> requireNotNull(event.result).let { result ->
          event.context.assignResult(result)
          when (result.effectiveResult.ok) {
            true -> "Test passed"
            false -> "Test failed"
          }
        }

        else -> return  // not interested in this event
      }

      if (plain) context.output {
        val label = labelForTest(event.test)
        appendLine("$prefix: $label")
      }
    }

    override fun close() {
      // nothing to do
    }
  }

  /** Whether to run multiple tests in parallel. */
  @Option(
    names = ["--parallel"],
    negatable = true,
    description = ["Run self-tests in parallel mode"],
    defaultValue = "true",
  )
  var parallel: Boolean = true

  /** Whether to run multiple tests in parallel. */
  @Option(
    names = ["--plain"],
    negatable = true,
    description = ["Run with plain output only"],
    defaultValue = "false",
  )
  var plain: Boolean = false

  /** Whether to summarize test results. */
  @Option(
    names = ["--summarize"],
    description = ["Print summary of test results"],
    defaultValue = "true",
  )
  var summarize: Boolean = true

  /** Filter to apply to eligible tests. */
  @Option(
    names = ["--tests"],
    description = ["Filter to apply to discovered self-tests"],
    arity = "0..N",
  )
  var testsFilter: String? = null

  // Test runner.
  private val runner: SelfTestRunner = SelfTestRunner()

  // Build context for a test.
  private fun buildSelfTestContext(): SelfTestContext {
    return DefaultSelfTestContext.create()
  }

  // Scan via the bean context for all known self tests.
  private fun scanForTestCases(): Collection<SelfTest> = beanContext.getBeansOfType(SelfTest::class.java)

  // Stream the set of found test cases.
  private fun streamAllTestCases(cases: Collection<SelfTest>): Flow<SelfTest> = cases
    .stream()
    .asSequence()
    .asFlow()

  // Run the provided `test` (a self-test).
  private suspend fun runSelfTest(agent: TestOutput, test: SelfTest): TestResult = runner.runTest(
    test,
    agent,
  )

  // Run all available self-tests, converting them as we go into `TestResult` instances.
  private suspend fun runAllSelfTests(
    cases: Collection<SelfTest>,
    agent: TestOutput,
  ): Flow<TestResult> = streamAllTestCases(cases).map {
    runSelfTest(agent, it)
  }

  // Render the result of a self-test run.
  private suspend fun CommandContext.printTestSummary(results: List<TestResult>) {
    output {
      results.forEach {
        val (symbols, message) = when (it.effectiveResult) {
          PASS -> ("✔" to "✅") to "PASS"
          FAIL -> ("⨯" to "❌") to "FAIL"
          SKIP -> ("⊝" to "⚠\uFE0F") to "SKIP"
          DROP -> ("‒" to "↘\uFE0F") to "DROP"
        }

        val (symbol, _) = symbols
        appendLine("$symbol $message ${it.info.name}")
      }
    }
  }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult = let { cmd ->
    output {
      appendLine("Running Elide self-tests...")
    }

    val selftests = scanForTestCases()
    if (selftests.isEmpty()) {
      output {
        appendLine("No tests to run.")
      }
      success()
    } else TestOutput(cmd).use { outputAgent ->
      if (plain) runAllSelfTests(selftests, outputAgent).toList(LinkedList()).let { results ->
        // print test summary, maybe
        if (summarize) printTestSummary(results)

        when (results.any { it.effectiveResult != PASS }) {
          true -> err("There were test failures")
          false -> success()
        }
      } else testRenderer(
        totalTests = selftests.size,
        workers = when (parallel) {
          true -> 4
          false -> 1
        },
        allTests = selftests.parallelStream().consumeAsFlow().map {
          it.testInfo() to suspend {
            async {
              runSelfTest(outputAgent, it)
            }
          }
        },
      ).let {
        success()
      }
    }
  }
}

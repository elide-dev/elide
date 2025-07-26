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
@file:OptIn(DelicateElideApi::class)

package elide.tooling.runner

import com.google.common.util.concurrent.MoreExecutors
import java.nio.file.Path
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlin.time.Duration
import kotlin.time.measureTimedValue
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.tooling.cli.Statics
import elide.tooling.config.TestConfigurator.TestEventController
import elide.tooling.testing.TestResult

// Implements a `TestRunner` which runs tests in parallel across threads.
public class ThreadedTestRunner internal constructor (
  config: TestRunner.Config,
  exec: Executor,
  events: TestEventController,
  contextProvider: () -> PolyglotContext,
) : AbstractTestRunner(config, exec, events, contextProvider) {
  public class Builder internal constructor (
    resourcesPath: Path = Statics.resourcesPath,
    override val contextProvider: () -> PolyglotContext,
  ) : TestRunner.Builder<ThreadedTestRunner> {
    override val config: TestRunner.MutableConfig = TestRunner.MutableConfig(resourcesPath)
    override var executor: Executor? = null

    override fun build(controller: TestEventController?): ThreadedTestRunner = ThreadedTestRunner(
      config = config.build(),
      events = controller ?: TestEventController.Inert,
      contextProvider = contextProvider,
      exec = executor ?: MoreExecutors.listeningDecorator(Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual().factory(),
      )),
    )
  }

  override fun runTest(scope: CoroutineScope, request: TestRunRequest): Deferred<TestExecutionResult> = scope.async {
    testSeen(request)
    val context = contextProvider()
    context.enter()
    try {
      runCatching {
        testExec(request)
        measureTimedValue { request.entry.invoke() }.let {
          it.duration to it.value
        }
      }.getOrElse {
        Duration.ZERO to fail(it)
      }.let { (duration, value) ->
        TestExecutionResult(
          test = request.test,
          scope = request.scope,
          result = value,
          timing = duration,
        ).also {
          when (val out = value) {
            is TestResult.Pass -> testSucceeded(request)
            is TestResult.Fail -> testFailed(request, out.cause)
            is TestResult.Skip -> testSkipped(request.test, out.reason)
          }
        }
      }
    } finally {
      context.leave()
    }
  }
}

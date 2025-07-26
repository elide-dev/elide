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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlin.time.measureTimedValue
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.tooling.cli.Statics
import elide.tooling.config.TestConfigurator.TestEventController
import elide.tooling.testing.TestResult

// Implements a `TestRunner` which runs tests in order, serially.
public class SerialTestRunner internal constructor (
  config: TestRunner.Config,
  exec: Executor,
  events: TestEventController,
  contextProvider: () -> PolyglotContext,
) : AbstractTestRunner(config, exec, events, contextProvider) {
  public class Builder internal constructor (
    resourcesPath: Path = Statics.resourcesPath,
    override val contextProvider: () -> PolyglotContext,
  ) : TestRunner.Builder<SerialTestRunner> {
    override val config: TestRunner.MutableConfig = TestRunner.MutableConfig(resourcesPath)
    override var executor: Executor? = MoreExecutors.directExecutor()

    override fun build(controller: TestEventController?): SerialTestRunner = SerialTestRunner(
      config = config.build(),
      exec = executor ?: MoreExecutors.directExecutor(),
      events = controller ?: TestEventController.Inert,
      contextProvider = contextProvider,
    )
  }

  override fun runTest(scope: CoroutineScope, request: TestRunRequest): Deferred<TestExecutionResult> {
    val context = contextProvider()
    context.enter()
    return try {
      measureTimedValue {
        runCatching {
          testExec(request)
          request.entry.invoke()
        }.getOrElse {
          fail(it)
        }
      }.let { timed ->
        scope.async {
          TestExecutionResult(
            test = request.test,
            scope = request.scope,
            result = timed.value,
            timing = timed.duration,
          ).also { result ->
            when (val out = result.result) {
              is TestResult.Pass -> testSucceeded(request)
              is TestResult.Skip -> testSkipped(request.test, out.reason)
              is TestResult.Fail -> testFailed(request, out.cause)
            }
          }
        }
      }
    } finally {
      context.leave()
      testSeen(request)
    }
  }
}

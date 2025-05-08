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

import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import java.nio.file.Path
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.guava.asDeferred
import kotlin.time.measureTimedValue
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.tool.cli.Statics
import elide.tooling.config.TestConfigurator.TestEventController

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

  override fun CoroutineScope.runTest(request: TestRunRequest): Deferred<TestExecutionResult> {
    testSeen(request)
    return measureTimedValue {
      runCatching {
        testExec(request)
        request.entry.invoke()
        pass()
      }.onSuccess {
        testSucceeded(request)
      }.onFailure {
        testFailed(request, it)
      }.getOrElse {
        fail(it)
      }
    }.let { timed ->
      // this is a serial runner, so we run it directly and then wrap in a pre-resolved `Deferred`.
      Futures.immediateFuture(TestExecutionResult(
        test = request.test,
        scope = request.scope,
        result = timed.value,
        timing = timed.duration,
      )).asDeferred()
    }
  }
}

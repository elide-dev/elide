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
package elide.runtime.runner

import org.graalvm.polyglot.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import elide.runtime.runner.jvm.StandardJvmRunner
import elide.tooling.Arguments
import elide.tooling.Classpath
import elide.tooling.asArgumentString

class JvmRunnerTest {
  val entrypointCls = "elide.runtime.runner.JvmRunnerEntrypointSample"
  private fun currentClasspath(): Classpath = Classpath.fromCurrent()

  private fun jvmRunnerJob(entry: String? = null): JvmRunner.JvmRunnerJob = JvmRunner.of(
    mainClass = entry ?: entrypointCls,
    classpath = currentClasspath(),
    args = Arguments.empty(),
  )

  private fun truffleContext(): Context {
    return Context.newBuilder()
      .allowAllAccess(true)
      .allowExperimentalOptions(true)
      .option("java.Classpath", currentClasspath().asArgumentString())
      .build()
  }

  private fun JvmRunner.prepare(): JvmRunner = apply {
    configure(
      truffleContext(),
      Dispatchers.Default,
    )
  }

  @Test fun testResolveAllRunners() {
    val runners = assertNotNull(Runners.all())
    assertTrue(runners.isNotEmpty())
  }

  @Test fun testResolveJvmRunner() {
    val runnerJob = jvmRunnerJob()
    val jvmRunners = assertNotNull(Runners.jvm(runnerJob))
    assertTrue(jvmRunners.isNotEmpty())
  }

  @Test fun testResolveJvmTruffleRunner() {
    val runnerJob = jvmRunnerJob()
    val jvmRunner = assertNotNull(Runners.jvm(runnerJob, truffle = true)).first()
    assertTrue(jvmRunner is TruffleRunner)
  }

  @Test fun testResolveJvmNonTruffleRunner() {
    val runnerJob = jvmRunnerJob()
    val jvmRunner = assertNotNull(Runners.jvm(runnerJob, truffle = false)).first()
    assertTrue(jvmRunner !is TruffleRunner)
  }

  @Test fun testCreateRunnerJob() {
    assertNotNull(jvmRunnerJob())
  }

  @Test fun testRunnerJobOnTruffle() = runTest {
    val job = jvmRunnerJob()
    val runner = Runners.jvm(job, truffle = true).first().prepare()
    assertIs<TruffleRunner>(runner)
    val outcome = assertNotNull(runner(job))
    assertIs<RunnerOutcome.Success>(outcome)
  }

  @Test fun testRunnerJobOnStandardJvm() = runTest {
    val job = jvmRunnerJob()
    val runner = Runners.jvm(job, truffle = false).first().prepare()
    assertIsNot<TruffleRunner>(runner)
    val outcome = assertNotNull(runner(job))
    assertIs<RunnerOutcome.Success>(outcome)
  }

  @Test fun testRunnerJobOnStandardJvmClsNotFound() = runTest {
    val job = jvmRunnerJob("some.unknown.Class")
    val runner = Runners.jvm(job, truffle = false).first().prepare()
    assertIsNot<TruffleRunner>(runner)
    val outcome = assertNotNull(runner(job))
    assertIsNot<RunnerOutcome.Success>(outcome)
  }

  @Test fun testRunnerJobOnTruffleJvmClsNotFound() = runTest {
    val job = jvmRunnerJob("some.unknown.Class")
    val runner = Runners.jvm(job, truffle = true).first().prepare()
    assertIs<TruffleRunner>(runner)
    val outcome = assertNotNull(runner(job))
    assertIsNot<RunnerOutcome.Success>(outcome)
  }

  @Test fun testRunnerJobOnTruffleJvmClsNotRunnable() = runTest {
    val job = jvmRunnerJob("elide.runtime.runner.JvmRunnerNoEntrypointSample")
    val runner = Runners.jvm(job, truffle = true).first().prepare()
    assertIs<TruffleRunner>(runner)
    val outcome = assertNotNull(runner(job))
    assertIsNot<RunnerOutcome.Success>(outcome)
  }

  @Test fun testRunnerJobOnStandardJvmClsNotRunnable() = runTest {
    val job = jvmRunnerJob("elide.runtime.runner.JvmRunnerNoEntrypointSample")
    val runner = Runners.jvm(job, truffle = false).first().prepare()
    assertIsNot<TruffleRunner>(runner)
    val outcome = assertNotNull(runner(job))
    assertIsNot<RunnerOutcome.Success>(outcome)
  }

  @Test fun testRunnerJobOnStandardJvmReflective() = runTest {
    val job = jvmRunnerJob()
    val runner = Runners.jvm(job, truffle = false).first().prepare()
    assertIsNot<TruffleRunner>(runner)
    assertIs<StandardJvmRunner>(runner)
    try {
      runner.configureStandardJvmRunner(StandardJvmRunner.JvmRunnerOptions(
        reflective = true,  // force reflective in-proc mode
      ))
      val outcome = assertNotNull(runner(job))
      assertIs<RunnerOutcome.Success>(outcome)
    } finally {
      runner.configureStandardJvmRunner(StandardJvmRunner.JvmRunnerOptions(
        reflective = false,
      ))
    }
  }
}

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
import elide.tooling.Arguments
import elide.tooling.Classpath
import elide.tooling.asArgumentString

class JvmRunnerTest {
  val entrypointCls = "elide.runtime.runner.JvmRunnerEntrypointSample"
  private fun currentClasspath(): Classpath = Classpath.fromCurrent()

  private fun jvmRunnerJob(): JvmRunner.JvmRunnerJob = JvmRunner.of(
    mainClass = entrypointCls,
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
    val jvmRunners = assertNotNull(Runners.jvm())
    assertTrue(jvmRunners.isNotEmpty())
  }

  @Test fun testResolveJvmTruffleRunner() {
    val jvmRunner = assertNotNull(Runners.jvm(truffle = true)).first()
    assertTrue(jvmRunner is TruffleRunner)
  }

  @Test fun testResolveJvmNonTruffleRunner() {
    val jvmRunner = assertNotNull(Runners.jvm(truffle = false)).first()
    assertTrue(jvmRunner !is TruffleRunner)
  }

  @Test fun testCreateRunnerJob() {
    assertNotNull(jvmRunnerJob())
  }

  @Test fun testRunnerJobOnTruffle() = runTest {
    val job = jvmRunnerJob()
    val runner = Runners.jvm(truffle = true).first().prepare()
    assertIs<TruffleRunner>(runner)
    val outcome = assertNotNull(runner(job))
    assertIs<RunnerOutcome.Success>(outcome)
  }

  @Test fun testRunnerJobOnStandardJvm() = runTest {
    val job = jvmRunnerJob()
    val runner = Runners.jvm(truffle = false).first().prepare()
    assertIsNot<TruffleRunner>(runner)
    val outcome = assertNotNull(runner(job))
    assertIs<RunnerOutcome.Success>(outcome)
  }
}

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
@file:Suppress("KotlinConstantConditions")
@file:OptIn(DelicateElideApi::class)

package elide.tooling.runner

import java.lang.AutoCloseable
import java.nio.file.Path
import java.util.LinkedList
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.Executor
import java.util.function.Predicate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import elide.exec.ActionScope
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.intrinsics.testing.TestingRegistrar.RegisteredTest
import elide.runtime.intrinsics.testing.TestingRegistrar.TestScope
import elide.tooling.config.TestConfigurator
import elide.tooling.config.TestConfigurator.*
import elide.tooling.project.ElideProject
import elide.tooling.runner.AbstractTestRunner.TestRunResult

// Default test parallelism to apply.
private val defaultParallelism by lazy {
  Runtime.getRuntime().availableProcessors() * 2
}

/**
 * # Test Runner
 *
 * Runner which accepts a configuration profile, and a suite of tests, and makes best attempts to run all tests to
 * exhaustion, in an efficient manner, while collecting results. Tests are gathered by test discovery mechanisms, and
 * then the runner and a stream is sent of all discovered tests.
 *
 * Runner implementations can elect to run tests in parallel or in a simple serial fashion. Events are propagated for
 * test execution, passes, failures, and so on, to any subscribed parties.
 *
 * ## Preparation
 *
 * Runner configuration governs concurrency, execution strategies, and other options which apply to the mechanics of
 * running the tests. Test selection is governed by a test predicate and sequence of tests. An executor may be provided;
 * if none is provided, a virtual-threaded executor is employed.
 *
 * ## Usage
 *
 * Once the runner is prepared, it can be used to run tests; the runner does not expect all tests to be available up-
 * front, and can accept tests as they are discovered. Provide discovered tests via `accept`. Enqueued tests are passed
 * to the executor, and results are eventually made available via the `results` flow.
 */
public interface TestRunner : AutoCloseable {
  /** Active configuration for this test runner. */
  public val config: Config

  /** Active executor in use by this runner. */
  public val executor: Executor

  /** Event controller in use by this runner. */
  public val events: TestEventController

  /**
   * Accept a [flow] of tests to run on this runner.
   *
   * @receiver Coroutine scope in which tests should be run.
   * @param flow Flow of discovered or specified tests to run.
   * @param final Indicates whether this is the final batch of detected tests; if `true`, the runner will begin
   *   concluding test execution once this batch completes.
   */
  public suspend fun tests(scope: CoroutineScope, flow: Flow<Pair<TestScope<*>, RegisteredTest>>, final: Boolean = true)

  /**
   * Await delivery and settlement of all tests; this method will block until a final delivery of tests is received by
   * [tests], and all tests conclude their execution (as applicable).
   *
   * In circumstances where an early exit arises (such as during `failFast` mode, when encountering a failed test), this
   * method will return before all tests have run; test stats must be inspected for more details.
   *
   * @return Test run result, including test stats and other information.
   */
  public suspend fun awaitSettled(): TestRunResult

  /**
   * ## Test Runner Configuration
   *
   * @property dry Whether to apply dry-run mode, in which case tests are planned, but not run.
   * @property actionScope Action scope where execution is taking place.
   */
  public sealed interface Config {
    /** Whether dry-run mode is active; defaults to `false`. */
    public val dry: Boolean

    /** Whether to suppress most output. */
    public val quiet: Boolean

    /** Whether to increase output level. */
    public val verbose: Boolean

    /** Whether to activate debugger features. */
    public val debug: Boolean

    /** Action scope where execution is taking place. */
    public val actionScope: ActionScope?

    /** Resources path that applies for this run. */
    public val resourcesPath: Path

    /** Root path to the project. */
    public val projectRoot: Path

    /** Elide project, if available. */
    public val project: ElideProject?

    /** Max number of workers. */
    public val parallelism: Int

    /** When `true`, stop on the first failed test. */
    public val failFast: Boolean

    /** Register a predicate to filter tests with. */
    public val testPredicate: Predicate<RegisteredTest>?
  }

  /** Mutable builder for a [TestRunner]; allows setting configuration and other parameters or controllers. */
  public interface Builder<T> where T: TestRunner {
    public val config: MutableConfig
    public var executor: Executor?
    public val contextProvider: () -> PolyglotContext

    /** @return Built and configured [TestRunner] instance of [T]. */
    public fun build(controller: TestEventController? = null): T

    /** @return Built and configured [TestRunner] instance of [T]. */
    public fun build(binder: TestEventController.Binder.() -> Unit): T {
      val eventBindings = HashMap<TestNotify, LinkedList<suspend TestNotify.(Any?) -> Unit>>()
      val controller = object: TestEventController {
        @Suppress("TooGenericExceptionCaught")
        override suspend fun <E : TestNotify, T : Any> emit(event: E, context: T) {
          val bindings = eventBindings[event]
          if (bindings?.isNotEmpty() == true) {
            bindings.forEach {
              try {
                @Suppress("UNCHECKED_CAST")
                (it as suspend E.(T) -> Unit).invoke(event, context)
              } catch (e: Exception) {
                Logging.root().debug("Exception during test callback", e)
              }
            }
          }
        }
      }
      val target = object: TestEventController.Binder {
        override fun <T : Any, X : Any, > bind(
          event: TestNotify,
          contextType: Class<T>,
          handler: suspend T.(X) -> Unit
        ): TestEventController.Binder = apply {
          @Suppress("UNCHECKED_CAST")
          eventBindings.computeIfAbsent(event) { LinkedList() }.add(handler as (suspend TestNotify.(Any?) -> Unit))
        }
      }
      target.binder()
      return build(controller)
    }
  }

  /** Default configuration for the test runner. */
  public data object ConfigDefaults {
    /** Default dry-run mode; defaults to `false`. */
    public const val DRY: Boolean = false

    /** Default quiet mode; defaults to `false`. */
    public const val QUIET: Boolean = false

    /** Default verbose mode; defaults to `false`. */
    public const val VERBOSE: Boolean = false

    /** Default debug mode; defaults to `false`. */
    public const val DEBUG: Boolean = false

    /** Default fail-fast mode; defaults to `false`. */
    public const val FAIL_FAST: Boolean = false
  }

  /**
   * ## Test Runner Configuration (Mutable)
   *
   * Holds test runner configuration in mutable form, so an invoking developer can customize behavior; call [build] to
   * construct into [ImmutableConfig].
   */
  public class MutableConfig internal constructor (override var resourcesPath: Path) : Config {
    override var dry: Boolean = ConfigDefaults.DRY
    override var quiet: Boolean = ConfigDefaults.QUIET
    override var verbose: Boolean = ConfigDefaults.VERBOSE
    override var debug: Boolean = ConfigDefaults.DEBUG
    override var failFast: Boolean = ConfigDefaults.FAIL_FAST
    override var testPredicate: Predicate<RegisteredTest>? = null
    override var actionScope: ActionScope? = null
    override var project: ElideProject? = null
    override var projectRoot: Path = Path.of(System.getProperty("user.dir"))
    override val parallelism: Int get() = defaultParallelism

    /** @return Immutable form of this configuration. */
    public fun build(): ImmutableConfig = ImmutableConfig(
      dry = dry,
      quiet = quiet,
      verbose = verbose,
      debug = debug,
      actionScope = actionScope,
      project = project,
      resourcesPath = resourcesPath,
      projectRoot = projectRoot,
      parallelism = parallelism,
    )
  }

  /**
   * ## Test Runner Configuration (Immutable)
   *
   * Holds test runner configuration in final immutable form.
   */
  @JvmRecord public data class ImmutableConfig internal constructor (
    override val dry: Boolean = ConfigDefaults.DRY,
    override val quiet: Boolean = ConfigDefaults.QUIET,
    override val verbose: Boolean = ConfigDefaults.VERBOSE,
    override val debug: Boolean = ConfigDefaults.DEBUG,
    override val failFast: Boolean = ConfigDefaults.FAIL_FAST,
    override val actionScope: ActionScope? = null,
    override val project: ElideProject? = null,
    override val testPredicate: Predicate<RegisteredTest>? = null,
    override val resourcesPath: Path = Path.of(System.getProperty("user.dir")),
    override val projectRoot: Path,
    override val parallelism: Int = defaultParallelism,
  ): Config

  /** Factories for preparing and constructing [TestRunner] instances. */
  public companion object {
    /** @return Serial test runner builder. */
    @JvmStatic public fun serialBuilder(ctxProvider: () -> PolyglotContext): SerialTestRunner.Builder =
      SerialTestRunner.Builder(contextProvider = ctxProvider)

    /** @return Threaded test runner builder. */
    @JvmStatic public fun threadedBuilder(ctxProvider: () -> PolyglotContext): ThreadedTestRunner.Builder =
      ThreadedTestRunner.Builder(contextProvider = ctxProvider)

    /** @return Serial test runner customized by [builder]. */
    @JvmStatic
    public fun serial(context: () -> PolyglotContext, builder: SerialTestRunner.Builder.() -> Unit = {}): TestRunner {
      return serialBuilder(context).apply { builder.invoke(this) }.build()
    }

    /** @return Threaded test runner customized by [builder]. */
    @JvmStatic public fun threaded(
      context: () -> PolyglotContext,
      builder: ThreadedTestRunner.Builder.() -> Unit = {},
    ): TestRunner {
      return threadedBuilder(context).apply { builder.invoke(this) }.build()
    }
  }
}

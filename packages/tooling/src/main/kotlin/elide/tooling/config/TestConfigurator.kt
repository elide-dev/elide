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
package elide.tooling.config

import io.micronaut.context.BeanContext
import java.nio.file.Path
import elide.exec.ActionScope
import elide.runtime.intrinsics.testing.TestingRegistrar
import elide.tooling.config.BuildConfigurator.ProjectDirectories
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.registry.ResolverRegistry

/**
 * # Test Configurator
 *
 * Test configurators implement some independently-calculable suite of test execution functionality for Elide project
 * test suites; for example, a configurator may be responsible for scanning the classpath for tests, or for configuring
 * aspects of testing (like coverage or reporting).
 */
public interface TestConfigurator : ProjectConfigurator {
  /**
   * ## Test Configuration
   *
   * API and structure provided to [TestConfigurator] instances.
   *
   * @property actionScope Project execution scope under which tests will be configured, discovered, and run.
   * @property resolvers Dependency resolvers assembled for this project's build.
   * @property projectRoot Root path to the project under configuration.
   * @property settings Mutable test settings, adjustable by configurators.
   */
  public interface TestConfiguration {
    public val actionScope: ActionScope
    public val resolvers: ResolverRegistry
    public val projectRoot: Path
    public val settings: MutableTestSettings
  }

  /**
   * ## Test State
   *
   * Describes resources, gathered state, and other contextual information intended for [TestConfigurator] instances at
   * runtime.
   *
   * @property beanContext Active Micronaut bean context.
   * @property project Configured Elide project under test.
   * @property events Event controller for test execution eventing.
   * @property layout Project directories for the configured project.
   * @property manifest Package manifest for the configured project.
   * @property resourcesPath Path to the resources directory to use (for Elide itself).
   * @property registrar Testing registrar to use during test discovery.
   */
  public interface ElideTestState {
    public val beanContext: BeanContext
    public val project: ElideConfiguredProject
    public val events: TestEventController
    public val layout: ProjectDirectories
    public val manifest: ElidePackageManifest
    public val resourcesPath: Path
    public val registrar: TestingRegistrar
  }

  /**
   * ## Test Notification
   *
   * Root of a hierarchy of types used to deliver events about project testing. See [TestEvent], [TestWork],
   * [TestWorker], and child types.
   */
  public sealed interface TestNotify

  /**
   * ## Test Notification: Event
   *
   * Describes a generic event which relates to test execution for an Elide project.
   */
  public sealed interface TestEvent : TestNotify

  /**
   * ## Test Notification: Status
   *
   * Describes an event which is emitted when test results are available for a test suite or test case.
   */
  public sealed interface TestStatus : TestNotify

  /** Delivered when a test passes, or all tests in a suite pass. */
  public data object TestPass : TestStatus

  /** Delivered when a test fails, or a suite is failed. */
  public data object TestFail : TestStatus

  /** Delivered when a test is skipped, or a suite is skipped. */
  public data object TestSkip : TestStatus

  /**
   * ## Test Notification: Work
   *
   * Describes an event which involves execution work (for instance, test execution itself) for an Elide project.
   */
  public sealed interface TestWork : TestEvent

  /**
   * ## Test Notification: Worker
   *
   * Describes an event where a test worker was launched, terminated, or otherwise interacted with.
   */
  public interface TestWorker : TestNotify

  /**
   * ## Test Event Controller
   *
   * Controller for formulating and enqueueing test events from callsites; internal test process objects (configurators
   * and runners) call into the controller to emit events. The controller is expected to keep track of interested
   * parties and deliver events to them as applicable.
   */
  public interface TestEventController {
    /**
     * ### Binder
     *
     * Context receiver which allows fluid binding to the test event controller's emitted events.
     */
    public interface Binder {
      /**
       * Bind an event handler to a method.
       *
       * @param T Type of event to bind to.
       * @param X Type of context to bind to; default-bound to [Any].
       * @param event Event type to bind to.
       * @param contextType Type for the context value that will be bound.
       * @param handler Handler method to dispatch.
       */
      public fun <T: Any, X: Any> bind(event: TestNotify, contextType: Class<T>, handler: suspend T.(X) -> Unit): Binder
    }

    /**
     * Emit event.
     *
     * Deliver a testing event to the event controller; this method is expected to be called often by various test
     * events, and may be called from non-main threads.
     *
     * In a thread-safe and non-blocking manner, the method must consider the provided [event] for delivery, ultimately
     * delivering it to any interested subscribers according to their expected semantics (including thread state).
     *
     * Event delivery happens for various types of events, all based on the [TestNotify] type hierarchy.
     *
     * @param event Event to deliver.
     * @param context Context related to this event; if no context applies, the test runner or configurator is provided.
     */
    public suspend fun <E: TestNotify, T: Any> emit(event: E, context: T)

    /** "Inert" event controller which does nothing. */
    public data object Inert : TestEventController {
      override suspend fun <E : TestNotify, T : Any> emit(event: E, context: T) {
        // no-op
      }
    }
  }

  /**
   * ## Test Settings
   *
   * API which provides settings access and control for test facilities within an Elide project.
   *
   * @property enableCoverage Whether coverage services are enabled.
   * @property enableDiscovery Whether discovery services are enabled.
   */
  public interface TestSettings {
    public val enableCoverage: Boolean
    public val enableDiscovery: Boolean
  }

  /**
   * ## Test Settings (Immutable)
   *
   * API which provides read-only settings access for test facilities within an Elide project.
   */
  @JvmRecord public data class ImmutableTestSettings (
    override val enableCoverage: Boolean = true,
    override val enableDiscovery: Boolean = true,
  ): TestSettings

  /**
   * ## Test Settings (Mutable)
   *
   * API which provides mutable settings access and control for test facilities within an Elide project.
   */
  public data class MutableTestSettings (
    override var enableCoverage: Boolean = true,
    override var enableDiscovery: Boolean = true,
  ): TestSettings

  /**
   * Contribute test configuration.
   *
   * This method is expected to be implemented by [TestConfigurator] subclasses; such "configurators" are loaded at
   * runtime in order to assemble test configuration and state for Elide projects.
   *
   * @param state State of the test environment, including the project and other resources.
   * @param config Configuration object to be populated by this configurator.
   */
  public suspend fun contribute(state: ElideTestState, config: TestConfiguration)
}

/**
 * Shorthand to bind a test event handler to the event controller.
 *
 * @param T Type of event to bind to.
 * @param X Type of context to bind to; default-bound to [Any].
 * @param event Event type to bind to.
 * @param handler Handler method to dispatch.
 */
public inline fun <X: Any, reified T: TestConfigurator.TestNotify> TestConfigurator.TestEventController.Binder.on(
  event: T,
  noinline handler: suspend T.(X) -> Unit,
): TestConfigurator.TestEventController.Binder = bind(
  event = event,
  contextType = T::class.java,
  handler = handler,
)

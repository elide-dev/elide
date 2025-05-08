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
   */
  public interface TestConfiguration {
    public val actionScope: ActionScope
    public val resolvers: ResolverRegistry
    public val projectRoot: Path
    public val settings: MutableTestSettings
    public val registrar: TestingRegistrar
  }

  /**
   * ## Test State
   *
   * Describes resources, gathered state, and other contextual information intended for [TestConfigurator] instances at
   * runtime.
   */
  public interface ElideTestState {
    public val beanContext: BeanContext
    public val project: ElideConfiguredProject
    public val events: TestEventController
    public val layout: ProjectDirectories
    public val manifest: ElidePackageManifest
    public val resourcesPath: Path
  }

  /**
   * ## Test Notification
   */
  public sealed interface TestNotify

  /**
   * ## Test Notification: Event
   */
  public sealed interface TestEvent : TestNotify

  /**
   * ## Test Notification: Worker
   */
  public sealed interface TestWork : TestEvent
  public interface TestWorker : TestNotify

  /**
   * ## Test Event Controller
   */
  public interface TestEventController {}

  /**
   * ## Test Settings
   */
  public interface TestSettings {}

  /**
   * ## Test Settings (Mutable)
   */
  public interface MutableTestSettings: TestSettings {}

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

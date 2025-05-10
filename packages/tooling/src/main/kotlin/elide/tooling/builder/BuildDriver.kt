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
package elide.tooling.builder

import io.micronaut.context.BeanContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import elide.exec.ExecutionBinder
import elide.exec.TaskGraph
import elide.exec.execute
import elide.tooling.config.BuildConfiguration
import elide.tooling.config.BuildConfigurator
import elide.tooling.config.BuildConfigurators
import elide.tooling.deps.DependencyResolver
import elide.tooling.project.ElideProject
import elide.exec.TaskGraphExecution.Listener as BuildListener
import elide.tooling.config.BuildConfigurator.BuildConfiguration as ConfiguredBuild

/**
 * # Build Driver
 *
 * Provides static utilities for configuring and executing components of Elide projects (or full projects). This also
 * exposes certain functionality for resolving dependencies and executing tasks in a project which is useful for test
 * and main code runners.
 *
 * ## Usage
 *
 * A full end-to-end build invocation looks something like this:
 * ```kotlin
 * val project: ElideProject = /* ... */
 *
 * // must already be in a coroutine scope, or establish one for the build
 * coroutineScope {
 *   BuildDriver.buildProject(project) {
 *     // bind build events here
 *   }
 * }
 * ```
 *
 * In this example, the `buildProject` method will configure the project, resolve dependencies, and execute the
 * materialized task graph to completion. The [ExecutionBinder] callback is optional but encouraged because builds do
 * not show any output or progress by default.
 *
 * ## Components of Builds
 *
 * To perform any subset of the above demonstrated behavior, you can use the component-wise methods:
 *
 * - [configure] establishes a [ConfiguredBuild] by executing all available [BuildConfigurators].
 * - [dependencies] configures [DependencyResolver] instances for a given project, and then seals them.
 * - [resolve] accepts an optional already-configured list of resolvers, and resolves all of them.
 * - [buildProject] performs all of the above steps in canonical order.
 *
 * A test or main code runner might use [configure], [dependencies], and [resolve], in order to prepare a project to be
 * run (assuming no compile step). Resolvers typically cache their output in deterministic ways, so this operation can
 * be pretty fast.
 */
public object BuildDriver {
  /**
   * ### Configure Build
   *
   * Given a parsed [ElideProject], this method will discover and execute all available [BuildConfigurator] instances;
   * then, finalization is performed, to produce a [ConfiguredBuild] instance. No actual build tasks are executed and no
   * dependencies are resolved by this method.
   *
   * @receiver The [CoroutineScope] to run the build in.
   * @param beanContext The [BeanContext] to use for dependency injection.
   * @param project The [ElideProject] to configure.
   * @param settings Optional [BuildConfigurator.BuildSettings] to use for configuration.
   * @param extraConfigurator An optional [BuildConfigurator] to use for additional configuration.
   * @return A [ConfiguredBuild] instance, which can be used to resolve dependencies and execute tasks.
   */
  @JvmStatic
  public suspend fun configure(
    beanContext: BeanContext,
    project: ElideProject,
    settings: BuildConfigurator.BuildSettings? = null,
    extraConfigurator: BuildConfigurator? = null,
  ): ConfiguredBuild {
    return BuildConfiguration.create(project.root, settings?.toMutable()).also {
      BuildConfigurators.contribute(beanContext, project.load(), it, extraConfigurator = extraConfigurator)
    }
  }

  /**
   * ### Materialize Dependencies (Build)
   *
   * Given a [ConfiguredBuild], materialize or otherwise resolve all dependencies for the project, across all ecosystems
   * and packages specified by the project. At this time, this includes all suites of dependencies (i.e. test- or dev-
   * scoped dependencies).
   *
   * @receiver The [CoroutineScope] to run the build in.
   * @param cfg The [ConfiguredBuild] to materialize dependencies for.
   * @return A [Deferred] list of [DependencyResolver] instances, which can be used to resolve dependencies.
   */
  @JvmStatic
  public fun CoroutineScope.dependencies(cfg: ConfiguredBuild): Deferred<List<DependencyResolver>> = async {
    cfg.resolvers.all().map { it.second }.toList().also {
      it.forEach {
        launch { it.seal() }
      }
    }
  }

  /**
   * ### Resolve Dependencies
   *
   * Given a [ConfiguredBuild] and a list of [DependencyResolver] instances, resolve all dependencies for the project;
   * it is assumed that the resolvers are already sealed for use (this means they are immutably configured and ready to
   * fire).
   *
   * Sealing of resolvers is performed by the [dependencies] step for either a configured build or project.
   *
   * After this method concludes, and the [Job] instances returned conclude, all dependencies should be resolved,
   * downloaded, written, linked, and otherwise prepared for use. Resolvers can be used to determine the final state of
   * this operation, and to calculate ephemera like classpaths.
   *
   * @receiver The [CoroutineScope] to run the build in.
   * @param buildConfig The [ConfiguredBuild] to resolve dependencies for.
   * @param dependencies The list of [DependencyResolver] instances to resolve.
   * @return A pair of the list of [DependencyResolver] instances and a list of [Job] instances, which represent the
   *   individual jobs needed for each resolver. All of these jobs should be awaited before proceeding.
   */
  @JvmStatic
  public suspend fun CoroutineScope.resolve(
    buildConfig: ConfiguredBuild,
    dependencies: List<DependencyResolver>? = null,
  ): Pair<List<DependencyResolver>, List<Job>> {
    return (dependencies ?: dependencies(buildConfig).await()).let { resolvers ->
      resolvers to resolvers.map { resolver ->
        async {
          resolver.resolve(this).toList().joinAll()
        }
      }
    }
  }

  /**
   * ### Build a Project
   *
   * Given a parsed [ElideProject], this method will perform an end-to-end build of all project targets; a full build
   * includes the following steps:
   *
   * - [configure] executes all [BuildConfigurator] instances visible to the project, yielding a [ConfiguredBuild].
   *
   * - [dependencies] materializes [DependencyResolver] instances, and [resolve] settles them into a usable state.
   *
   * - A [TaskGraph] is built from the [ElideProject] and [ConfiguredBuild]; after all dependencies are resolved, the
   *   graph is executed to completion. This method executes all mounted tasks by default, with dependency awareness
   *   across tasks within the graph.
   *
   * #### Coroutine Scoping
   *
   * It is expected that project builds occur within a [CoroutineScope] established by the caller. Context may change or
   * accrue for tasks, and a new [CoroutineScope] may be established (nested) for task graph execution. Once the outer
   * [CoroutineScope] concludes, the caller can assume that the build has finished, including all resolution, task graph
   * execution, and so on.
   *
   * No assumptions are made about execution context except that a scope is established by the caller; the build process
   * will switch threading contexts as needed, or from time to time, and provides no guarantees about which thread runs
   * event callbacks. Make sure when updating UI to properly handle context switches in callbacks where appropriate.
   *
   * #### Event Handling
   *
   * The [ExecutionBinder] callback is optional and allows the caller to subscribe to various task graph events. A few
   * of these events deal with failure, including task and full graph failure and/or cancellation. By default, the task
   * graph doesn't emit any output (unless debug logging is turned on).
   *
   * @receiver The [CoroutineScope] to run the build in.
   * @param project The [ElideProject] to build.
   * @param config The [ConfiguredBuild] to use; if not provided, a new one will be built.
   * @param binder An optional [ExecutionBinder] to bind build events with.
   * @return A [BuildListener] instance, which can be used for additional bindings, cancellation, and general build
   *   execution control.
   * @see elide.exec.TaskGraphExecution.Listener [BuildListener] for attaching to events
   * @see ElideProject Loading and using Elide projects
   */
  @JvmStatic
  public suspend fun CoroutineScope.buildProject(
    beanContext: BeanContext,
    project: ElideProject,
    config: ConfiguredBuild? = null,
    binder: ExecutionBinder? = null,
  ): BuildListener = configureBuildProject(beanContext, project, config, binder).also { listener ->
    coroutineScope {
      launch { listener.await() }
    }
  }

  // Internally build a project with given configurations or resolvers.
  @JvmStatic
  internal suspend fun CoroutineScope.configureBuildProject(
    beanContext: BeanContext,
    project: ElideProject,
    config: ConfiguredBuild? = null,
    binder: ExecutionBinder? = null,
  ): BuildListener {
    val buildConfig = config ?: configure(beanContext, project)
    val graph = async { TaskGraph.build(buildConfig.taskGraph) }
    val deps = dependencies(buildConfig).await()
    val (_, jobs) = resolve(buildConfig, deps)
    jobs.joinAll()

    return graph.await().execute(buildConfig.actionScope) {
      binder?.invoke(this)
    }
  }
}

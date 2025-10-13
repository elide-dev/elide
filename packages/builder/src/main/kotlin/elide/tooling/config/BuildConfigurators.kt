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
import java.util.LinkedList
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import elide.tooling.config.BuildConfigurator.BuildConfiguration
import elide.tooling.config.BuildConfigurator.*
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.manifest.ElidePackageManifest

/**
 * ## Build Configurators
 *
 * Deals with the discovery, instantiation, and "contribution" dispatch of [BuildConfigurator]-compliant instances from
 * the JVM service-loader protocol.
 *
 * Configurators are expected to be instantiable without parameters. Once created, configurators have a chance to
 * "contribute" to build configuration through their [BuildConfigurator.contribute] method.
 *
 * Build configurators impose no ordering semantics, and are expected to be executed in parallel during the preparatory
 * ("configuration") stage of a build. Thus, configurators should be idempotent, thread-safe, and independent of each
 * other.
 *
 * @see BuildConfigurator protocol for build configuration
 * @see TestConfigurator similar protocol for test configurators
 * @see TestConfigurators service driver for test configuration
 */
public object BuildConfigurators {
  // Keeps track of executed build configurators.
  private val executedConfigurators = ConcurrentSkipListMap<String, Boolean>()

  // Accounts for dependencies between configurators.
  private suspend fun executeDepsThenConfigurator(
    state: ElideBuildState,
    to: BuildConfiguration,
    configurator: BuildConfigurator,
    all: List<BuildConfigurator>,
  ) {
    val alreadyDone = executedConfigurators.containsKey(configurator::class.java.simpleName)
    if (alreadyDone) {
      // configurator has already been executed, skip it
      return
    }
    val deps = configurator.dependsOn()
    val depsSatisfiedOrEmpty = deps.isEmpty() || deps.all { dep ->
      executedConfigurators.containsKey(dep.java.simpleName)
    }

    when {
      depsSatisfiedOrEmpty -> configurator.contribute(state, to).also {
        // mark this configurator as executed
        executedConfigurators[configurator::class.java.simpleName] = true
      }

      else -> deps.forEach { dep ->
        val matchedConfigurator = all.find {
          it::class.java == dep
        } ?: error(
          "No matching build configurator for dependency: $dep"
        )
        executeDepsThenConfigurator(
          state,
          to,
          matchedConfigurator,
          all,
        )
      }
    }
  }

  @JvmStatic public fun collect(): Sequence<BuildConfigurator> {
    return ServiceLoader.load<BuildConfigurator>(BuildConfigurator::class.java).asSequence()
  }

  @JvmStatic
  public suspend fun contribute(
    beanContext: BeanContext,
    project: ElideConfiguredProject,
    from: Sequence<BuildConfigurator>,
    to: BuildConfiguration,
    extraConfigurator: BuildConfigurator? = null,
    binder: BuildEventController.() -> Unit = {},
  ) {
    val eventBindings = HashMap<BuildEvent, MutableList<suspend (BuildEvent, ctx: Any?) -> Unit>>()
    val dispatcherFactory = Thread.ofVirtual().factory()
    val dispatcher = Executors.newThreadPerTaskExecutor(dispatcherFactory)
    val dispatcherContext = dispatcher.asCoroutineDispatcher()

    val eventController = object : BuildEventController {
      override fun emit(event: BuildEvent) {
        dispatcher.execute {
          runBlocking(dispatcherContext) {
            eventBindings[event]?.forEach { cbk ->
              cbk(event, null)
            }
          }
        }
      }

      override fun <T> emit(event: BuildEvent, ctx: T?) {
        dispatcher.execute {
          runBlocking(dispatcherContext) {
            eventBindings[event]?.forEach { cbk ->
              cbk(event, ctx)
            }
          }
        }
      }

      @Suppress("UNCHECKED_CAST")
      override fun <E : BuildEvent, X> bind(event: E, cbk: suspend E.(X) -> Unit) {
        eventBindings.computeIfAbsent(event) { LinkedList() }.also {
          it.add(cbk as suspend (BuildEvent, Any?) -> Unit)
        }
      }
    }
    // bind events
    binder.invoke(eventController)

    val layout = object : ProjectDirectories {
      override val projectRoot: Path get() = to.projectRoot
    }
    val state = object : ElideBuildState {
      override val debug: Boolean get() = to.settings.debug
      override val release: Boolean get() = to.settings.release
      override val beanContext: BeanContext get() = beanContext
      override val project: ElideConfiguredProject get() = project
      override val console: BuildConsoleController get() = TODO("Not yet implemented")
      override val events: BuildEventController get() = eventController
      override val manifest: ElidePackageManifest get() = project.manifest
      override val layout: ProjectDirectories get() = layout
      override val resourcesPath: Path get() = project.resourcesPath
      override val config: BuildConfiguration get() = to
    }
    from.let {
      when (extraConfigurator) {
        null -> it
        else -> it + sequenceOf(extraConfigurator)
      }
    }.toList().let { configurators ->
      configurators.forEach { configurator ->
        executeDepsThenConfigurator(state, to, configurator, configurators)
      }
    }
  }

  @JvmStatic public suspend fun contribute(
    beanContext: BeanContext,
    project: ElideConfiguredProject,
    to: BuildConfiguration,
    extraConfigurator: BuildConfigurator? = null,
    binder: BuildEventController.() -> Unit = {},
  ) {
    contribute(beanContext, project, collect(), to, extraConfigurator, binder)
  }
}

/**
 * Subscribe to a build event [T] using context shape [X].
 *
 * @param T Type of event to subscribe to
 * @param X Type of context to pass to the callback
 * @param ev Event to subscribe to
 * @param cbk Callback to invoke when the event is emitted
 */
public inline fun <X: Any, reified T: BuildEvent> BuildEventController.on(ev: T, noinline cbk: suspend T.(X) -> Unit) {
  bind(ev, cbk)
}

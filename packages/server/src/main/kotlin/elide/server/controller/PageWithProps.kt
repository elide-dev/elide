/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.server.controller

import com.google.common.annotations.VisibleForTesting
import com.google.common.util.concurrent.Futures
import org.graalvm.polyglot.HostAccess
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.guava.asDeferred
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import elide.ssr.type.RequestState

/**
 * Extends [PageController] with support for page-level [State], produced via the [props] method; computed state is
 * shared with VM render executions, and can additionally be injected into the page for use by frontend code (typically
 * to hydrate a server-rendered UI).
 *
 * ### Defining custom page state
 *
 * When extending `PageWithProps`, a [State] class must be provided which follows a set of requirements. All [State]
 * classes must:
 *
 * - Be annotated with [kotlinx.serialization.Serializable] to facilitate DOM injection of prop structure. Annotating
 *   a class with `Serializable` has its own set of requirements; see the Kotlin Serialization Guide for more info.
 * - Annotated with [HostAccess.Export] for each SSR-available property -- this can occur at the top level of a tree
 *   of properties, for instance
 *
 * An example of compliant custom page [State]:
 * ```kotlin
 * @Serializable
 * data class HelloProps(
 *   @get:HostAccess.Export val message: String = "Hello World!",
 * )
 * ```
 *
 * And providing that state via the [PageWithProps] controller:
 * ```
 * @Page class HelloPage : PageWithProps<HelloProps>(HelloProps.serializer()) {
 *   override suspend fun props(): HelloProps {
 *     // ...
 *   }
 * }
 * ```
 *
 * ### Using state from SSR executions
 *
 * When running guest language code for SSR, for instance JavaScript, [State] is made available and can be loaded using
 * frontend tools -- for instance, [elide.js.ssr.boot]:
 *
 * ```kotlin
 * boot<HelloProps> { props ->
 *   // ...
 * }
 * ```
 *
 * Optionally, the developer can load the inlined server-side props on their own, via a known DOM ID:
 *
 * ```js
 * JSON.parse(document.getElementById("ssr-data").textContent || '{}')
 * ```
 *
 * In SSR mode, [elide.js.ssr.boot] will read the props (if any), and provide them to the entrypoint for the application
 * so initial render or hydration may be performed, based on the active serving mode.
 *
 * @param State Represents the serializable property state associated with this controller. [propsAsync] is executed to
 *   produce the rendered set of page props. If no state is needed, use [Any].
 * @param serializer Kotlin serializer to use for the state attached to this controller.
 * @param defaultState Default state value to inject, if any.
 */
@Suppress("MemberVisibilityCanBePrivate")
public abstract class PageWithProps<State> protected constructor (
  @VisibleForTesting internal val serializer: KSerializer<State>,
  @VisibleForTesting internal val defaultState: State? = null,
) : PageController() {
  /** @return Finalized context from this controller, which can then be passed to render processes. */
  @Suppress("SwallowedException")
  private suspend fun computePropsAsync(state: RequestState): Deferred<State?> {
    return try {
      propsAsync(state)
    } catch (uoe: UnsupportedOperationException) {
      Futures.immediateFuture(null).asDeferred()
    }
  }

  /**
   * Asynchronously compute the server-side [State] (also referred to as "props") which should be active for the
   * lifetime of the current request; developer-provided props must follow guidelines to be usable safely (see below).
   *
   * When performing blocking work to compute page properties, implementations should suspend. Both the async and
   * synchronous versions of this method are available for the developer to override (prefer [props]). If no state is
   * provided by the developer, `null` is returned.
   *
   * If the developer overrides the method but opts to throw instead, [UnsupportedOperationException] should be thrown,
   * which is caught and translated into `null` state.
   *
   * To use a given class as server-side [State], it must:
   * - Be annotated with [kotlinx.serialization.Serializable] to facilitate DOM injection of prop structure. Annotating
   *   a class with `Serializable` has its own set of requirements; see the Kotlin Serialization Guide for more info.
   * - Annotated with [HostAccess.Export] for each SSR-available property -- this can occur at the top level of a tree
   *   of properties, for instance
   *
   * @see props for the synchronous version of this same method (the preferred extension point).
   * @param state Computed request state for this request/response cycle.
   * @return Deferred task which resolves to the state provided for this cycle, or `null`.
   */
  public open suspend fun propsAsync(state: RequestState): Deferred<State?> = coroutineScope {
    return@coroutineScope async {
      props(state)
    }
  }

  /**
   * Compute the server-side [State] (also referred to as "props") which should be active for the lifetime of the
   * current request; developer-provided props must follow guidelines to be usable safely (see below).
   *
   * When performing blocking work to compute page properties, implementations should suspend. Both the async and
   * synchronous versions of this method are available for the developer to override (prefer [props]). If no state is
   * provided by the developer, `null` is returned.
   *
   * If the developer overrides the method but opts to throw instead, [UnsupportedOperationException] should be thrown,
   * which is caught and translated into `null` state.
   *
   * To use a given class as server-side [State], it must:
   * - Be annotated with [kotlinx.serialization.Serializable] to facilitate DOM injection of prop structure. Annotating
   *   a class with `Serializable` has its own set of requirements; see the Kotlin Serialization Guide for more info.
   * - Annotated with [HostAccess.Export] for each SSR-available property -- this can occur at the top level of a tree
   *   of properties, for instance
   *
   * @see propsAsync for the asynchronous version of this same method (available, but not recommended).
   * @param state Computed request state for this request/response cycle.
   * @return State that should be active for this cycle, or `null` if no state is provided or available.
   */
  public open suspend fun props(state: RequestState): State? = defaultState

  /**
   * "Finalize" the state for this controller, by (1) computing the state, if necessary, and (2) serializing it for
   * embedding into the page; frontend tools can then read this state to hydrate the UI without causing additional calls
   * to the server.
   *
   * @param state Materialized HTTP request state for this cycle.
   * @return Deferred task which resolves to a pair, where the first item is the [State] procured for this cycle via the
   *   [props] and [propsAsync] methods, and the second item is the same state, serialized as a JSON [String]. If no
   *   state is available, both pair members are `null`.
   */
  public open suspend fun finalizeAsync(state: RequestState): Deferred<Pair<State?, String?>> = coroutineScope {
    return@coroutineScope async {
      val computed = computePropsAsync(state).await()
      if (computed != null) {
        computed to Json.encodeToString(
          serializer,
          computed,
        )
      } else {
        null to null
      }
    }
  }
}

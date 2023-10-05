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

package elide.runtime.core

/**
 * The lifecycle allows [plugins][EnginePlugin] to subscribe to [events][EngineLifecycleEvent] such as
 * [engine][EngineLifecycleEvent.EngineCreated] and [context][EngineLifecycleEvent.ContextCreated] configuration.
 *
 * [Lifecycle events][EngineLifecycleEvent] are part of a sealed hierarchy of singletons that act as type-safe event
 * keys, and can be used as follows:
 *
 * ```kotlin
 * lifecycle.on(EngineLifecycleEvent.ContextCreated) { it: ContextBuilder ->
 *  // update the context builder before it is handed back to the engine
 *  it.option(...)
 * }
 * ```
 */
@DelicateElideApi public interface EngineLifecycle {
  public fun <T> on(event: EngineLifecycleEvent<T>, consume: (T) -> Unit)
}

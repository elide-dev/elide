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
package elide.runtime.core

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine

/**
 * The Polyglot Engine is responsible for creating new [PolyglotContext] instances, as well as triggering
 * [events][EngineLifecycleEvent] that allow [plugins][EnginePlugin] to extend the runtime.
 *
 * Engine instances can be created using the [PolyglotEngine][elide.runtime.core.PolyglotEngine] DSL, which provides
 * methods to configure and extend the engine by installing plugins.
 *
 * The [acquire] function can be used to obtain a new [PolyglotContext] configured by the engine.
 */
@DelicateElideApi
public interface PolyglotEngine {
  /** @return The underlying GraalVM engine. */
  public fun unwrap(): Engine

  /** Acquire a new [PolyglotContext]. The returned context has all plugins applied on creation. */
  public fun acquire(cfg: Context.Builder.(Engine) -> Unit = {}): PolyglotContext {
    return acquire(shared = true, detached = false, cfg = cfg)
  }

  /** Acquire a new [PolyglotContext]. The returned context has all plugins applied on creation. */
  public fun acquire(shared: Boolean, detached: Boolean, cfg: Context.Builder.(Engine) -> Unit = {}): PolyglotContext
}

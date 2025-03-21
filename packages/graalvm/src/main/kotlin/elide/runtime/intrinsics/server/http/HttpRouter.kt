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
package elide.runtime.intrinsics.server.http

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intrinsics.server.http.internal.HandlerRegistry

/**
 * Base class providing route registration APIs to guest code, compiling routing keys that can be used to resolve
 * handler references from a [HandlerRegistry].
 */
@DelicateElideApi public interface HttpRouter {
  /** Guest-accessible method used to register a [handler] for the provided [method] and [path]. */
  @Export public fun handle(method: String?, path: String?, handler: PolyglotValue)
}

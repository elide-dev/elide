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

package elide.tooling.js.resolver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import elide.tooling.deps.DependencyResolver

/**
 * ## JSR Resolver
 */
public class JsrResolver : DependencyResolver.JsrResolver {
  override fun close() {
    TODO("Not yet implemented")
  }

  override suspend fun resolve(scope: CoroutineScope): Sequence<Job> {
    TODO("Not yet implemented")
  }

  override suspend fun seal() {
    TODO("Not yet implemented")
  }
}

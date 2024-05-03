/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.embedded

import kotlinx.coroutines.Deferred
import elide.embedded.http.EmbeddedResponse

/**
 * A dispatcher for incoming [EmbeddedCall]s, which schedules execution in the scope of a guest application using the
 * active runtime configuration to manage the guest evaluation context.
 */
public fun interface EmbeddedCallDispatcher {
  /**
   * Dispatch an incoming [call] through a guest [app], returning a deferred value which tracks the execution
   * progress.
   */
  public fun dispatch(call: EmbeddedCall, app: EmbeddedApp): Deferred<EmbeddedResponse>
}

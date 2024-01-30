/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package elide.embedded.api

import elide.core.api.Symbolic
import elide.embedded.NativeApi
import elide.embedded.NativeApi.InvocationStatus as NativeInvocationStatus
import elide.embedded.NativeApi.InvocationStatus.*

/**
 *
 */
public enum class InvocationStatus (override val symbol: NativeInvocationStatus): Symbolic<NativeInvocationStatus> {
  /**
   *
   */
  PENDING(ELIDE_INFLIGHT_PENDING),

  /**
   *
   */
  EXECUTING(ELIDE_INFLIGHT_EXECUTING),

  /**
   *
   */
  ERR(ELIDE_INFLIGHT_ERR),

  /**
   *
   */
  COMPLETED(ELIDE_INFLIGHT_COMPLETED);

  /** */
  public companion object : Symbolic.SealedResolver<NativeInvocationStatus, InvocationStatus> {
    override fun resolve(symbol: NativeApi.InvocationStatus): InvocationStatus = when (symbol) {
      ELIDE_INFLIGHT_PENDING -> PENDING
      ELIDE_INFLIGHT_EXECUTING -> EXECUTING
      ELIDE_INFLIGHT_ERR -> ERR
      ELIDE_INFLIGHT_COMPLETED -> COMPLETED
    }
  }
}

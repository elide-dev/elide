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
import elide.embedded.NativeApi.ProtocolMode as NativeProtocolMode
import elide.embedded.NativeApi.ProtocolMode.*

/**
 * # Protocol Mode
 */
public enum class ProtocolMode (override val symbol: NativeProtocolMode) : Symbolic<NativeProtocolMode> {
  /**
   *
   */
  PROTOBUF(ELIDE_PROTOBUF),

  /**
   *
   */
  CAPNPROTO(ELIDE_CAPNPROTO);

  /**
   *
   */
  public companion object : Symbolic.SealedResolver<NativeProtocolMode, ProtocolMode> {
    @JvmStatic override fun resolve(symbol: NativeProtocolMode): ProtocolMode = when (symbol) {
      ELIDE_PROTOBUF -> PROTOBUF
      ELIDE_CAPNPROTO -> CAPNPROTO
    }
  }
}

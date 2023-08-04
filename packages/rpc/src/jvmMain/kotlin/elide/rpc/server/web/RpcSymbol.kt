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

package elide.rpc.server.web

/**
 * Enumerates byte symbols which are meaningful in the gRPC-Web Protocol; there are only two, [DATA] and [TRAILER].
 *
 * The [TRAILER] value is used to encode gRPC responses over regular HTTP/1.1-style responses, if needed. The [DATA]
 * symbol is used to demarcate a data frame inside a gRPC Web request or response.
 */
public enum class RpcSymbol (public val value: Byte) {
  /** Symbol indicating a data frame. */
  DATA((0x00).toByte()),

  /** Symbol used to demarcate trailers. */
  TRAILER((0x80).toByte()),
}

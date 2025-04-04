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

@file:Suppress("ClassName")

package elide.http

import elide.core.api.Symbolic

// HTTP version constants.
private const val HTTP_1_0_SYMBOL = "HTTP/1.0"
private const val HTTP_1_1_SYMBOL = "HTTP/1.1"
private const val HTTP_2_SYMBOL = "HTTP/2"

/**
 * ## HTTP Protocol Version
 *
 * Describes the version of the HTTP protocol which is in use; substantively, this consists of a [major] and [minor]
 * protocol version.
 */
public sealed interface ProtocolVersion : HttpToken, Symbolic<String> {
  /** Major protocol version number. */
  public val major: UShort

  /** Minor protocol version number. */
  public val minor: UShort get() = 0u

  /** String symbol representing this protocol version. */
  override val symbol: String

  override fun asString(): String = symbol

  /** Known protocol version for HTTP 1.0. */
  public data object HTTP_1_0 : ProtocolVersion {
    override val major: UShort get() = 1u
    override val symbol: String get() = HTTP_1_0_SYMBOL
  }

  /** Known protocol version for HTTP 1.1. */
  public data object HTTP_1_1 : ProtocolVersion {
    override val major: UShort get() = 1u
    override val minor: UShort get() = 1u
    override val symbol: String get() = HTTP_1_1_SYMBOL
  }

  /** Known protocol version for HTTP 2.0. */
  public data object HTTP_2 : ProtocolVersion {
    override val major: UShort get() = 2u
    override val symbol: String get() = HTTP_2_SYMBOL
  }

  /** Static utilities relating to [ProtocolVersion] instances. */
  public companion object: Symbolic.SealedResolver<String, ProtocolVersion> {
    override fun resolve(symbol: String): ProtocolVersion = when (symbol) {
      HTTP_1_0_SYMBOL -> HTTP_1_0
      HTTP_1_1_SYMBOL -> HTTP_1_1
      HTTP_2_SYMBOL -> HTTP_2
      else -> throw unresolved("Unknown protocol version: $symbol")
    }
  }
}

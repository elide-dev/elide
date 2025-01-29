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
package elide.runtime.intrinsics.sqlite

import io.micronaut.core.annotation.ReflectiveAccess
import elide.annotations.API
import elide.core.api.Symbolic

// Token identifying a deferred transaction.
private const val DEFERRED_TOKEN: String = "DEFERRED"

// Token identifying an immediate transaction.
private const val IMMEDIATE_TOKEN: String = "IMMEDIATE"

// Token identifying an exclusive transaction.
private const val EXCLUSIVE_TOKEN: String = "EXCLUSIVE"

/**
 * # SQLite: Transaction Types
 *
 * Enumerates transaction modes supported by SQLite; the SQLite driver adopts a global default transaction mode (which
 * may be adjusted in a stateful manner by the user), and may also run transactions in an explicit mode.
 *
 * Transaction types govern how transactions are executed amidst other transactions; for more information about each
 * type, please see the SQLite documentation.
 *
 * @see SQLiteTransaction Transaction API
 * @see SQLiteTransactor Transactor Interface
 */
@API @ReflectiveAccess public enum class SQLiteTransactionType (override val symbol: String): Symbolic<String> {
  /** Automatic (or undeclared) transaction type. */
  AUTO("AUTO"),

  /** Deferred transaction type. */
  DEFERRED(DEFERRED_TOKEN),

  /** Immediate transaction type. */
  IMMEDIATE(IMMEDIATE_TOKEN),

  /** Exclusive transaction type. */
  EXCLUSIVE(EXCLUSIVE_TOKEN);

  /**
   * ## SQLite Transaction Symbols
   *
   * Provides a mapping of transaction type symbols to transaction types.
   */
  public companion object: Symbolic.SealedResolver<String, SQLiteTransactionType> {
    override fun resolve(symbol: String): SQLiteTransactionType {
      return when (symbol) {
        DEFERRED_TOKEN -> DEFERRED
        IMMEDIATE_TOKEN -> IMMEDIATE
        EXCLUSIVE_TOKEN -> EXCLUSIVE
        else -> AUTO
      }
    }
  }
}

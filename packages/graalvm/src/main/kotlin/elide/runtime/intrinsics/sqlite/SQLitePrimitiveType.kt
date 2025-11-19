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
import kotlin.reflect.KClass
import elide.annotations.API
import elide.core.api.Symbolic

// Token for a primitive string type.
private const val SQLITE_PRIMITIVE_TEXT = "TEXT"

// Token for a primitive integer type.
private const val SQLITE_PRIMITIVE_INTEGER = "INTEGER"

// Token for a primitive real type.
private const val SQLITE_PRIMITIVE_REAL = "REAL"

// Token for a primitive blob type.
private const val SQLITE_PRIMITIVE_BLOB = "BLOB"

// Integer identifying text fields.
private const val SQLITE_INT_TEXT: Int = 0

// Integer identifying integer fields.
private const val SQLITE_INT_INTEGER: Int = 1

// Integer identifying real fields.
private const val SQLITE_INT_REAL: Int = 2

// Integer identifying blob fields.
private const val SQLITE_INT_BLOB: Int = 3

/**
 * # SQLite: Primitive Type
 *
 * Enumerates each primitive type supported by SQLite; SQLite has a particularly restricted set of data primitives, to
 * include:
 *
 * - `TEXT`: A string type; encoding is set at the database level.
 * - `INTEGER`: A 64-bit signed integer.
 * - `REAL`: A floating-point value.
 * - `BLOB`: A binary large object.
 *
 * All high-level data types are expressed in one of the four types specified above.
 *
 * &nbsp;
 *
 * ## Resolving by Symbol
 *
 * The SQLite protocol supports both string names and integer identifiers for primitive types; the `SQLitePrimitiveType`
 * maps both.
 *
 * Use the [resolve] method to map a symbol to a `SQLitePrimitiveType` instance, and the [symbol] or [number] properties
 * to obtain the string or numeric representation.
 */
@API @ReflectiveAccess public enum class SQLitePrimitiveType (
  override val symbol: String,
  public val number: Int,
  public val mappedType: KClass<*>,
): SQLiteType, Symbolic<String> {
  /**
   * ## Data Type: Text
   *
   * The `TEXT` data type is a string type; encoding is set at the database level.
   */
  TEXT(SQLITE_PRIMITIVE_TEXT, SQLITE_INT_TEXT, String::class),

  /**
   * ## Data Type: Integer
   *
   * The `INTEGER` data type is stored as a 64-bit signed integer-capable value.
   */
  INTEGER(SQLITE_PRIMITIVE_INTEGER, SQLITE_INT_INTEGER, Long::class),

  /**
   * ## Data Type: Real
   *
   * The `REAL` data type is a floating-point value.
   */
  REAL(SQLITE_PRIMITIVE_REAL, SQLITE_INT_REAL, Double::class),

  /**
   * ## Data Type: Blob
   *
   * The `BLOB` data type is a binary large object.
   */
  BLOB(SQLITE_PRIMITIVE_BLOB, SQLITE_INT_BLOB, ByteArray::class);

  /**
   * ## Resolve by Symbol
   *
   * Companion object which offers methods (via [resolve]) to obtain a [SQLitePrimitiveType] by its string or numeric
   * identity.
   */
  public companion object: Symbolic.SealedResolver<String, SQLitePrimitiveType> {
    /**
     * Resolve an SQLite primitive type by its integer symbol.
     *
     * @param symbol The integer symbol to resolve.
     * @return The resolved SQLite primitive type.
     * @throws Symbolic.Unresolved if the symbol is unrecognized.
     */
    public fun resolve(symbol: Int): SQLitePrimitiveType = when (symbol) {
      SQLITE_INT_TEXT -> TEXT
      SQLITE_INT_INTEGER -> INTEGER
      SQLITE_INT_REAL -> REAL
      SQLITE_INT_BLOB -> BLOB
      else -> throw unresolved("Primitive symbol: $symbol")
    }

    /**
     * Resolve an SQLite primitive type by its string symbol or type affinity.
     *
     * @param symbol The string symbol or type name to resolve.
     * @return The resolved SQLite primitive type.
     * @see <a href="https://www.sqlite.org/datatype3.html#determination_of_column_affinity">SQLite Type Affinity</a>
     */
    override fun resolve(symbol: String): SQLitePrimitiveType {
      when (symbol) {
        SQLITE_PRIMITIVE_TEXT -> return TEXT
        SQLITE_PRIMITIVE_INTEGER -> return INTEGER
        SQLITE_PRIMITIVE_REAL -> return REAL
        SQLITE_PRIMITIVE_BLOB -> return BLOB
      }

      val upperSymbol = symbol.uppercase()

      if (upperSymbol.contains("INT")) return INTEGER

      if (upperSymbol.contains("CHAR") ||
          upperSymbol.contains("CLOB") ||
          upperSymbol.contains("TEXT")) return TEXT

      if (upperSymbol.contains("BLOB") || upperSymbol.isEmpty()) return BLOB

      if (upperSymbol.contains("REAL") ||
          upperSymbol.contains("FLOA") ||
          upperSymbol.contains("DOUB")) return REAL

      return REAL
    }
  }
}

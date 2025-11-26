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
package elide.runtime.intrinsics.sqlite

import io.micronaut.core.annotation.ReflectiveAccess
import elide.annotations.API
import elide.vm.annotations.Polyglot

/**
 * # SQLite Changes
 *
 * Represents the result of a write operation (INSERT, UPDATE, DELETE) on a SQLite database.
 * This matches the Bun SQLite API's `Changes` interface.
 *
 * @see SQLiteStatement.run
 * @see SQLiteDatabase.exec
 */
@API @ReflectiveAccess public interface SQLiteChanges {
  /**
   * The number of rows changed by the last `run` or `exec` call.
   */
  @get:Polyglot public val changes: Long

  /**
   * The rowid of the last inserted row, or 0 if no row was inserted.
   * If `safeIntegers` is enabled, this should be treated as a bigint.
   */
  @get:Polyglot public val lastInsertRowid: Long
}

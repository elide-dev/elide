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
import java.io.Closeable
import java.sql.PreparedStatement
import java.sql.Statement
import elide.annotations.API
import elide.vm.annotations.Polyglot

/**
 * # SQLite Statement
 *
 * Describes a SQLite query statement, potentially in prepared form, which can be executed against an active SQLite
 * database connection.
 *
 * This type is typically created by the [SQLiteDatabase] interface, as driven by user code, and then either executed in
 * one-shot form, or in repeated (prepared) form, as needed.
 *
 * SQLite statements hold their own resources, so they are [Closeable] and [AutoCloseable]; all statement except simple
 * one-shot statements are managed by their owning [SQLiteDatabase] instance.
 *
 * As a result, calling [SQLiteDatabase.close] will call [close] on any owned statements.
 *
 * @see SQLiteDatabase SQLite Database API
 */
@API @ReflectiveAccess public interface SQLiteStatement: Closeable, AutoCloseable {
  /**
   * Unwrap this statement into a JDBC [Statement]; this method is provided for host-side use only.
   *
   * @return JDBC statement corresponding to this one.
   */
  public fun unwrap(): Statement

  /**
   * Prepare this statement with the provided [args] (as applicable), rendering if needed; successive calls to this
   * method may skip superfluous work.
   *
   * This method prepares the statement into a JDBC [PreparedStatement], and is meant for host-side use only.
   *
   * @param args Arguments to render into the query; if a map is provided as the first and only argument, it is used as
   *   a suite of named arguments.
   * @return Parsed, validated, and statement for execution.
   */
  public fun prepare(args: Array<out Any?>? = null): PreparedStatement

  /**
   * ## All Results
   *
   * Execute the underlying statement against the owning [SQLiteDatabase]; then, build a [List] of all results, with
   * each structured as a map-like [SQLiteObject].
   *
   * If no results are found, an empty list is returned; if the underlying database has closed, an error is thrown.
   *
   * Repeated calls to this method with unchanging [args] will cache the underlying rendered query, but will not cache
   * the result-set.
   *
   * @param args Arguments to render into the query; if a map is provided as the first and only argument, it is used as
   *   a suite of named arguments.
   * @return List of matching [SQLiteObject] instances.
   */
  @Polyglot public fun all(vararg args: Any?): List<SQLiteObject>

  /**
   * ## All Values
   *
   * Execute the underlying statement against the owning [SQLiteDatabase]; then, build a [List] of all results, with
   * each structured as an array of values, with order preserved for the requested table columns.
   *
   * If no results are found, an empty list is returned; if the underlying database has closed, an error is thrown.
   *
   * Repeated calls to this method with unchanging [args] will cache the underlying rendered query, but will not cache
   * the result-set.
   *
   * @param args Arguments to render into the query; if a map is provided as the first and only argument, it is used as
   *   a suite of named arguments.
   * @return List of matching rows, with values structured as list entries.
   */
  @Polyglot public fun values(vararg args: Any?): List<List<Any?>>

  /**
   * ## Single Value
   *
   * Execute the underlying statement against the owning [SQLiteDatabase]; then, build a single result, structured as a
   * map-like [SQLiteObject].
   *
   * This method is optimized to never decode more than one result.
   * The user's query is not modified, and may additionally be optimized by appending `LIMIT 1`.
   *
   * If no results are found, `null` is returned; if the underlying database has closed, an error is thrown.
   *
   * Repeated calls to this method with unchanging [args] will cache the underlying rendered query, but will not cache
   * the result-set.
   *
   * @param args Arguments to render into the query; if a map is provided as the first and only argument, it is used as
   *  a suite of named arguments.
   * @return Single matching [SQLiteObject] instance, or `null`.
   */
  @Polyglot public fun get(vararg args: Any?): SQLiteObject?

  /**
   * ## Run Statement
   *
   * Execute the underlying statement against the owning [SQLiteDatabase]; this method is optimized for one-shot
   * execution, or execution of queries without results (such as inserts, updates, deletes, and schema changes).
   *
   * If the underlying database has closed, an error is thrown.
   *
   * Repeated calls to this method with unchanging [args] will cache the underlying rendered query, but will not cache
   * execution of the query (in other words, the query is executed each time [run] is called).
   *
   * @param args Arguments to render into the query.
   * @return Changes object containing the number of affected rows and last insert rowid.
   */
  @Polyglot public fun run(vararg args: Any?): SQLiteChanges

  /**
   * ## Finalize Statement
   *
   * Close this statement, releasing any resources held by it; this method is called automatically by the owning
   * database when it is closed, but may also be called earlier by the user on-demand.
   *
   * After a statement has closed, it may not be used again; calls to it result in an error.
   */
  @Polyglot @JvmSynthetic public fun finalize()
}

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

/**
 * # SQLite Transactor
 *
 * Describes a function which is provided as a transactional closure to the SQLite driver; the transactor executes the
 * steps of the transaction itself, and may be subject to rollback or retries as determined by the engine.
 *
 * This type is typically not accessible directly by user code, but is instead consumed by the [SQLiteTransaction]
 * created by the driver to facilitate execution.
 *
 * &nbsp;
 *
 * ## Idempotency
 *
 * Because transactions are subject to rollback and retry, the transactor should be idempotent; that is, it should be
 * safe to execute the transaction multiple times without causing side effects.
 *
 * For more information about transaction behavior, see the [SQLiteTransaction] documentation.
 *
 * @param R Return value type.
 * @see SQLiteTransaction Transaction API
 */
@FunctionalInterface
@API @ReflectiveAccess public fun interface SQLiteTransactor<R> {
  /**
   * Execute the transaction; this method is called by the SQLite driver to execute the transaction, and may be called
   * multiple times in the event of a rollback or retry.
   *
   * The [args] provided, if any, are rendered into the prepared statement which is executed as part of the transaction.
   * The return value [R] of the transactor is provided to the caller.
   *
   * @receiver Database instance.
   * @param args Transaction arguments.
   * @return Return value of the transaction.
   */
  public fun SQLiteDatabase.transact(vararg args: Any?): R

  /**
   * Transaction dispatch; called by the SQLite engine to invoke the transaction function.
   *
   * The transaction function is defined at [transact], and is called within the receivership of an [SQLiteDatabase]
   * instance.
   *
   * @param database Database instance.
   * @param args Transaction arguments.
   * @return Return value of the transaction.
   */
  public fun dispatch(database: SQLiteDatabase, args: Array<out Any?>): R = database.transact(*args)
}

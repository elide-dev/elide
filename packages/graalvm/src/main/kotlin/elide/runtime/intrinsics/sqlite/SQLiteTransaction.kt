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
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.util.concurrent.Future
import elide.annotations.API
import elide.vm.annotations.Polyglot

// Properties and methods surfaced to guest code for an SQLite transaction via the object interface.
private val SQLITE_TRANSACTION_PROPS_AND_METHODS = arrayOf(
  "deferred",
  "immediate",
  "exclusive",
)

/**
 * # SQLite Transaction
 *
 * Describes a transaction which is bound to an [SQLiteTransactor] function, and which is under execution via the SQLite
 * engine against an active database; the transaction may be invoked one or more times, and may be adjusted via the
 * mode methods provided ([deferred], [immediate], and [exclusive]).
 *
 * If no transaction mode is declared explicitly via the methods provided, the connection's default transaction mode is
 * used.
 *
 * &nbsp;
 *
 * ## Transaction Behavior
 *
 * Transaction functions may be executed more than once in order to facilitate retry or rollback logic; the transaction
 * function is expected to be idempotent.
 *
 * The return value (type [R]) provided by the [SQLiteTransactor], if any, is returned by this transaction object, via
 * [invoke].
 *
 * &nbsp;
 *
 * ## Object Interop & Cancellation
 *
 * Transactions are directly executable, either host-side (via [invoke]), or guest-side (via [ProxyExecutable]).
 * Transactions can also be used as [Future] objects which yield their result when the transaction completes.
 *
 * Cancelling a transaction will make a best-effort attempt to interrupt it (if allowed), by raising an exception within
 * the thread executing the transaction.
 *
 * Transaction cancellation then propagates to the caller instead of providing a value.
 *
 * [SQLite Transaction Docs](https://www.sqlite.org/lang_transaction.html)
 * @param R Return value type.
 * @see SQLiteDatabase Database API
 * @see SQLiteStatement Statement API
 * @see SQLiteTransactor Transactor Interface
 */
@API @ReflectiveAccess public interface SQLiteTransaction<R>: ProxyExecutable, ProxyObject, Future<R> {
  /** Indicate the type of this transaction; see [SQLiteTransactionType] for options. */
  public val type: SQLiteTransactionType

  /**
   * ## Transaction Mode: Deferred
   *
   * Applies `DEFERRED` mode to this transaction; another transaction object is returned (a copy), applying this mode,
   * but with the same properties otherwise.
   *
   * [SQLite documentation](https://www.sqlite.org/lang_transaction.html) describes `DEFERRED`-mode transactions as:
   *
   * "`DEFERRED` means that the transaction does not actually start until the database is first accessed. Internally,
   * the `BEGIN DEFERRED` statement merely sets a flag on the database connection that turns off the automatic commit
   * that would normally occur when the last statement finishes. This causes the transaction that is automatically
   * started to persist until an explicit `COMMIT` or `ROLLBACK` or until a rollback is provoked by an error or an
   * `ON CONFLICT ROLLBACK` clause. If the first statement after `BEGIN DEFERRED` is a `SELECT`, then a read transaction
   * is started. Subsequent write statements will upgrade the transaction to a write transaction if possible, or return
   * `SQLITE_BUSY`. If the first statement after `BEGIN DEFERRED` is a write statement, then a write transaction is
   * started."
   */
  @Polyglot public fun deferred(): SQLiteTransaction<R>

  /**
   * ## Transaction Mode: Immediate
   *
   * Applies `IMMEDIATE` mode to this transaction; another transaction object is returned (a copy), applying this mode,
   * but with the same properties otherwise.
   *
   * [SQLite documentation](https://www.sqlite.org/lang_transaction.html) describes `IMMEDIATE`-mode transactions as:
   *
   * "`IMMEDIATE` causes the database connection to start a new write immediately, without waiting for a write
   * statement. The `BEGIN IMMEDIATE` might fail with `SQLITE_BUSY` if another write transaction is already active on
   * another database connection."
   */
  @Polyglot public fun immediate(): SQLiteTransaction<R>

  /**
   * ## Transaction Mode: Exclusive
   *
   * Applies `EXCLUSIVE` mode to this transaction; another transaction object is returned (a copy), applying this mode,
   * but with the same properties otherwise.
   *
   * [SQLite documentation](https://www.sqlite.org/lang_transaction.html) describes `EXCLUSIVE`-mode transactions as:
   *
   * "`EXCLUSIVE` is similar to `IMMEDIATE` in that a write transaction is started immediately. `EXCLUSIVE` and
   * `IMMEDIATE` are the same in WAL mode, but in other journaling modes, `EXCLUSIVE` prevents other database
   * connections from reading the database while the transaction is underway."
   */
  @Polyglot public fun exclusive(): SQLiteTransaction<R>

  // Polyglot execution delegates to `invoke`.
  override fun execute(vararg arguments: Value?): Any = invoke(*arguments) as Any

  /**
   * Execute this transaction, invoking the associated [SQLiteTransactor] function.
   *
   * @param args Arguments to pass to the transactor function.
   * @return the result of the transactor function.
   */
  @Polyglot public operator fun invoke(vararg args: Any?): R

  override fun getMemberKeys(): Array<String> = SQLITE_TRANSACTION_PROPS_AND_METHODS
  override fun hasMember(key: String?): Boolean = key != null && key in SQLITE_TRANSACTION_PROPS_AND_METHODS
  override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false

  override fun putMember(key: String?, value: Value?) {
    // no-op
  }
}

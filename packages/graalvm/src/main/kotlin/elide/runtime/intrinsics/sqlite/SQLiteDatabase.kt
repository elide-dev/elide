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

import com.oracle.truffle.js.runtime.objects.JSDynamicObject
import io.micronaut.core.annotation.ReflectiveAccess
import org.intellij.lang.annotations.Language
import org.sqlite.SQLiteConnection
import org.sqlite.core.DB
import java.io.Closeable
import elide.annotations.API
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.js.Disposable
import elide.vm.annotations.Polyglot

// Properties and methods exposed to guest code.
private val SQLITE_DATABASE_PROPS_AND_METHODS = arrayOf(
  "loadExtension",
  "prepare",
  "query",
  "exec",
  "transaction",
  "serialize",
  "close"
)

/**
 * # SQLite Database
 *
 * Describes the concept of a database as driven by the SQLite engine; SQLite databases can be held in-memory, or backed
 * by a file which is persisted to disk.
 *
 * The [SQLiteDatabase] is the primary interface through which all database operations are performed, including queries,
 * transactions, and schema operations.
 *
 * &nbsp;
 *
 * ## Database Lifecycle
 *
 * There are essentially three phases to a SQLite database as understood by Elide: **initialized**, **opened**, and
 * **closed**.
 *
 * When the database is in an **initialized** state, it has not yet loaded data from disk or memory (via serialization).
 *
 * Initialization takes place immediately when the database object is created, and is guided by the configuration
 * parameters which can be provided to the constructor.
 *
 * The database moves almost immediately to an **opened** state, where it is ready to accept queries, transactions, or
 * deserialization from in-memory data.
 *
 * ```mermaid
 * graph TD
 *  A[Initialized] --> B[Opened]
 *  B --> C[Active]
 *  C --> C
 *  C --> D[Closed]
 * ```
 *
 * When the database is closed (via the [close] method), all associated resources are freed, and the database may no
 * longer be used for queries or transactions of any kind; such transactions (placed after close) throw unconditional
 * exceptions.
 *
 * &nbsp;
 *
 * ## Managed Resources
 *
 * All resources associated with a given SQLite database are managed by that database; this includes query ephemeral,
 * like prepared statements, transactions, and associated objects.
 *
 * When a database is closed, this tree of objects is closed (where applicable) in a reasonable order.
 *
 * After closing, resources are freed, with the only exception being result objects de-serialized for use in the user's
 * application.
 *
 * These objects remain alive so long as the user's application maintains references to them; database references held
 * by ephemeral objects are always weakly referenced.
 *
 * &nbsp;
 *
 * ## Guest Usage
 *
 * SQLite leverages a constrained set of primitive data types (see [SQLitePrimitiveType]) to represent data within user
 * tables; as a result, SQLite data types work well with guest code out of the box.
 *
 * Wrapping and boxing types, where required, use proxy interfaces so that they interoperate as expected with guest
 * code; by and large, Elide's SQLite API for guests follows Bun's API in JavaScript.
 *
 * @see SQLiteAPI Module-level SQLite API
 * @see SQLiteStatement Statement API
 * @see SQLiteTransaction Transaction API
 * @see SQLiteTransactor Transactor Interface
 * @see SQLiteType SQLite Types
 */
@API @ReflectiveAccess public interface SQLiteDatabase: Closeable, AutoCloseable, ReadOnlyProxyObject, Disposable {
  /** Hard-coded defaults for SQLite. */
  public companion object Defaults {
    public const val DEFAULT_CREATE: Boolean = false
    public const val DEFAULT_READONLY: Boolean = false
    public const val DEFAULT_THROW_ON_ERROR: Boolean = true
    public const val MAIN_SCHEMA: String = "main"
  }

  /** Indicate whether the database is open and active; if `false`, queries will throw. */
  public val active: Boolean

  /**
   * Obtain the underlying SQLite JDBC connection; this method is provided for host-side use only.
   *
   * @return SQLite/JDBC connection for this database.
   */
  public fun connection(): SQLiteConnection

  /**
   * Unwrap the underlying SQLite database; this method is provided for host-side use only.
   *
   * @return SQLite native database object.
   */
  public fun unwrap(): DB

  /**
   * ## Load Extension
   *
   * Load a native extension into the SQLite engine context managed by this database proxy; the extension is loaded via
   * the native interface for SQLite.
   *
   * If the extension cannot be located or cannot be loaded, an exception is thrown.
   *
   * @param extension Extension path or name to load.
   */
  @Polyglot public fun loadExtension(extension: String): JSDynamicObject

  /**
   * ## Prepare Statement
   *
   * Parse and prepare a query [statement] for execution; the resulting statement can be used multiple times, with
   * intelligent caching for re-use.
   *
   * Arguments provided at creation time are cached with the statement and merged with additional arguments provided
   * at execution time.
   *
   * See _Query Arguments_ for more information on how to bind arguments to a prepared statement.
   *
   * @param statement SQL query to prepare.
   * @param args Arguments to bind to the statement.
   * @return Prepared statement object.
   */
  @Polyglot public fun prepare(@Language("sql") statement: String, vararg args: Any?): SQLiteStatement

  /**
   * ## Query (String)
   *
   * Parse a query [statement] for execution; the resulting statement can be used multiple times, with intelligent
   * caching for re-use.
   *
   * Arguments provided at creation time are cached with the statement and merged with additional arguments provided
   * at execution time.
   *
   * See _Query Arguments_ for more information on how to bind arguments to a prepared statement.
   *
   * @param statement SQL query to prepare.
   * @param args Arguments to bind to the statement.
   * @return Prepared statement object.
   */
  @Polyglot public fun query(@Language("sql") statement: String, vararg args: Any?): SQLiteStatement

  /**
   * ## Query (Statement)
   *
   * Parse a query [statement] for execution; the resulting statement can be used multiple times, with intelligent
   * caching for re-use.
   *
   * Arguments provided at creation time are cached with the statement and merged with additional arguments provided
   * at execution time.
   *
   * See _Query Arguments_ for more information on how to bind arguments to a prepared statement.
   *
   * @param statement SQL query to prepare.
   * @param args Arguments to bind to the statement.
   * @return Prepared statement object.
   */
  @Polyglot public fun query(statement: SQLiteStatement, vararg args: Any?): SQLiteStatement

  /**
   * ## Execute (String)
   *
   * Parse, prepare, and then execute an SQL query [statement] with the provided [args] (if any), against the current
   * SQLite database.
   *
   * @param statement SQL query to execute.
   * @param args Arguments to bind to the statement.
   * @return Changes object containing the number of affected rows and last insert rowid.
   */
  @Polyglot public fun exec(@Language("sql") statement: String, vararg args: Any?): SQLiteChanges

  /**
   * ## Execute (Statement)
   *
   * Execute the provided [statement], preparing it if necessary, with the provided [args] (if any), against the current
   * SQLite database.
   *
   * @param statement Prepared statement to execute.
   * @param args Arguments to bind to the statement.
   * @return Changes object containing the number of affected rows and last insert rowid.
   */
  @Polyglot public fun exec(statement: SQLiteStatement, vararg args: Any?): SQLiteChanges

  /**
   * ## Execute Transaction
   *
   * Execute the provided [runnable] transaction function against the current SQLite database, applying recovery and
   * exclusion logic where applicable; the return value [R] yielded by the transaction function is returned to the
   * caller, via the resolution of the provided [SQLiteTransaction].
   *
   * Transaction functions may be executed multiple times to facilitate retry or rollback logic.
   *
   * For more information about transaction behavior, see _Transactions_ in the main database object docs, and in the
   * SQLite documentation.
   *
   * @param runnable Transaction function to execute.
   * @return Transaction object.
   */
  @Polyglot public fun <R> transaction(runnable: SQLiteTransactor<R>): SQLiteTransaction<R>

  /**
   * ## Serialize Database
   *
   * Serialize the current database into a raw array of bytes; the resulting bytes can be persisted to disk, or sent
   * across a network connection, and de-serialized into an equivalently behaving database object at a later time or on
   * another peer.
   *
   * The [schema] parameter is used to specify the schema to serialize; by default, the `main` schema is serialized.
   *
   * @param schema Schema to serialize.
   * @return Serialized database.
   */
  @Polyglot public fun serialize(schema: String = MAIN_SCHEMA): ByteArray

  /**
   * ## De-serialize Database
   *
   * De-serialize an array of raw bytes previously emitted via [serialize], loading the resulting data into the current
   * SQLite database; the [schema] parameter is used to specify the schema to de-serialize (if none is provided, the
   * `main` schema is used).
   *
   * @param data Serialized database.
   * @param schema Schema to de-serialize.
   * @return De-serialized database.
   */
  @Polyglot public fun deserialize(data: ByteArray, schema: String = MAIN_SCHEMA)

  /**
   * ## Close Database
   *
   * Close the underlying connection backing this database, and persist the current database data if so configured via
   * the path provided to the database constructor.
   *
   * After persisting the database, close all associated resources, including prepared statements, transactions, and
   * other ephemeral objects.
   *
   * For more information about close and free behavior, see _Managed Resources_ in the main database object docs.
   *
   * @param throwOnError Whether to throw an exception if an error occurs during close.
   */
  @Polyglot public fun close(throwOnError: Boolean)

  @Polyglot override fun close() {
    close(DEFAULT_THROW_ON_ERROR)
  }

  // -- Proxy Object Interface

  override fun getMemberKeys(): Array<String> = SQLITE_DATABASE_PROPS_AND_METHODS
}

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
package elide.runtime.gvm.builtins.sqlite

import elide.runtime.intrinsics.sqlite.SQLiteDatabase
import java.nio.file.Path

/**
 * # SQLite
 *
 * Static utilities for creating or accessing SQLite databases from within an Elide-enabled application, or host-side
 * from the Elide runtime; uses native bindings into the SQLite C API.
 *
 * &nbsp;
 *
 * ## Usage
 *
 * Obtain a database via the [inMemory] or [atPath] methods:
 * ```kotlin
 * val db = SQLite.inMemory()
 * ```
 * ```kotlin
 * val db = SQLite.atPath(Path.of("my-database.db"))
 * ```
 *
 * Optionally, provide [SQLiteCreateDatabaseOptions] to configure the database:
 * ```kotlin
 * val db = SQLite.atPath(Path.of("my-database.db"), SQLiteCreateDatabaseOptions.defaults().copy(
 *   readonly = true,
 * ))
 * ```
 *
 * &nbsp;
 *
 * ## Guest Bindings
 *
 * SQLite databases are enabled for guest use by default, and can be passed without modification between guest and host
 * code; this is also true for SQLite transactions, statements, objects, and so on.
 */
public data object SQLite {
  /**
   * ## SQLite: Deserialize
   *
   * Create an in-memory database, and then de-serialize the provided [data] into that database instance; the returned
   * database contains the provided data.
   *
   * @param data The serialized database data.
   * @return The database instance.
   */
  @JvmStatic public fun deserialize(data: ByteArray): SQLiteDatabase = SqliteDatabaseProxy.fromSerialized(data)

  /**
   * ## SQLite: In-memory
   *
   * Create an in-memory database, using any of the provided options as constructor options; this method is syntactic
   * sugar on top of [inMemory] with an explicit suite of options.
   *
   * @param readonly Whether the database should be read-only.
   * @return The database instance.
   */
  @JvmStatic public fun inMemory(readonly: Boolean = SQLiteDatabase.Defaults.DEFAULT_READONLY): SQLiteDatabase =
    SqliteDatabaseProxy.inMemory(SQLiteCreateDatabaseOptions.defaults().let {
      if (!readonly) it else it.copy(readonly = true)
    })

  /**
   * ## SQLite: Disk-backed
   *
   * Create a disk-backed SQLite database instance, at the provided [path], and with the provided [options], assuming
   * sandbox access to I/O; if no permission is granted for I/O, an error is thrown.
   *
   * @param path The path to the database file.
   * @param options The database creation options.
   * @return The database instance.
   */
  @JvmStatic public fun atPath(
      path: Path,
      options: SQLiteCreateDatabaseOptions = SQLiteCreateDatabaseOptions.defaults(),
  ): SQLiteDatabase = SqliteDatabaseProxy.atFile(
    path,
    options,
  )

  /**
   * ## SQLite: Disk-backed
   *
   * Create a disk-backed SQLite database instance, at the provided [path], and with the provided options, assuming
   * sandbox access to I/O; if no permission is granted for I/O, an error is thrown.
   *
   * This method is a convenience wrapper around [atPath], with the ability to specify options directly.
   *
   * @param path The path to the database file.
   * @param create Whether to create the database if it does not exist.
   * @param readonly Whether the database should be read-only.
   * @return The database instance.
   */
  @JvmStatic public fun atPath(
      path: Path,
      create: Boolean = SQLiteDatabase.Defaults.DEFAULT_CREATE,
      readonly: Boolean = SQLiteDatabase.Defaults.DEFAULT_READONLY,
  ): SQLiteDatabase = atPath(
    path,
    SQLiteCreateDatabaseOptions.defaults().copy(
      create = create,
      readonly = readonly,
    ),
  )
}

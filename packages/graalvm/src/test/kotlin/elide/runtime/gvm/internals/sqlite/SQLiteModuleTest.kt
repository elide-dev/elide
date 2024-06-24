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
@file:Suppress("NpmUsedModulesInstalled", "LargeClass")

package elide.runtime.gvm.internals.sqlite

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.sqlite.SQLiteException
import java.io.FileNotFoundException
import java.nio.file.Files
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.streams.asStream
import kotlin.test.*
import elide.annotations.Inject
import elide.core.api.Symbolic.Unresolved
import elide.jvm.LifecycleBoundResources
import elide.runtime.gvm.js.node.ElideJsModuleTest
import elide.runtime.gvm.sqlite.SQLite
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.sqlite.SQLiteAPI
import elide.runtime.intrinsics.sqlite.SQLitePrimitiveType
import elide.runtime.intrinsics.sqlite.SQLiteTransactionType
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of SQLite. */
@TestCase internal class SQLiteModuleTest : ElideJsModuleTest<ElideSqliteModule>() {
  override val pureModuleName: String get() = "sqlite"
  override fun provide(): ElideSqliteModule = ElideSqliteModule()
  @Inject lateinit var sqlite: SQLiteAPI

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("Database")
  }

  @Test override fun testInjectable() {
    assertNotNull(sqlite)
  }

  @Test fun testDatabaseAsProxyObject() {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      assertNotNull(it.connection())
      assertNotNull(it.unwrap())
      assertNotNull(it.memberKeys)
      assertNull(it.getMember("nonexistent"))
      assertFalse(it.removeMember("sample"))
      assertFalse(it.hasMember(null))
      assertTrue(it.memberKeys.isNotEmpty())
      assertFalse(it.hasMember(null))
      it.memberKeys.forEach { key ->
        assertTrue(it.hasMember(key))
      }
      assertDoesNotThrow {
        it.putMember("something", Value.asValue("bar"))
      }
      assertDoesNotThrow {
        assertFalse(it.removeMember("something"))
      }
    }
  }

  @Test fun testSqlitePrimitiveType() {
    SQLitePrimitiveType.entries.forEach {
      assertNotNull(it)
      assertNotNull(it.name)
      assertNotNull(it.ordinal)
      assertNotNull(it.symbol)
      assertNotNull(it.number)
      assertEquals(it, SQLitePrimitiveType.valueOf(it.name))
      assertEquals(it, SQLitePrimitiveType.resolve(it.symbol))
      assertEquals(it, SQLitePrimitiveType.resolve(it.number))
    }
    assertThrows<Unresolved> {
      SQLitePrimitiveType.resolve("foo")
    }
    assertThrows<Unresolved> {
      SQLitePrimitiveType.resolve(99)
    }
  }

  @Test fun testRejectInvalidExtension() {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      assertThrows<SQLiteException> {
        it.loadExtension("sample")
      }
      assertThrows<SQLiteException> {
        val loader = assertNotNull(it.getMember("loadExtension"))
        assertIs<ProxyExecutable>(loader)
        loader.execute(Value.asValue("sample"))
      }
      assertThrows<IllegalArgumentException> {
        it.loadExtension("sample; SELECT * FROM sensitive;")
      }
    }
  }

  @Test fun testSqliteTransactionType() {
    SQLiteTransactionType.entries.forEach {
      assertNotNull(it)
      assertNotNull(it.name)
      assertNotNull(it.ordinal)
      assertNotNull(it.symbol)
      assertEquals(it, SQLiteTransactionType.valueOf(it.name))
      assertEquals(it, SQLiteTransactionType.resolve(it.symbol))
    }
    assertDoesNotThrow {
      SQLiteTransactionType.resolve("foo")
      assertEquals(SQLiteTransactionType.AUTO, SQLiteTransactionType.resolve("foo"))
    }
  }

  @Test fun `create an in-memory sqlite database`() = dual {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
    }
  }.guest {
    // language=JavaScript
    """
      const { ok } = require("node:assert");
      const { Database } = require("elide:sqlite");
      ok(Database);
      ok(new Database());
    """
  }

  @Test fun testDatabaseAsDisposable() {
    val db = assertNotNull(SQLite.inMemory())
    assertTrue(db.active)
    db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
    db.exec("INSERT INTO test (name) VALUES ('foo');")
    assertTrue(db.active)
    db.dispose()
    assertFalse(db.active)
    assertThrows<IllegalArgumentException> {
      db.exec("SELECT * FROM test;")
    }
  }

  @Test fun testDatabaseAsResource() {
    val db = assertNotNull(SQLite.inMemory())
    db.use {
      assertTrue(db.active)
      db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      db.exec("INSERT INTO test (name) VALUES ('foo');")
      assertTrue(db.active)
    }
    assertFalse(db.active)
    assertThrows<IllegalArgumentException> {
      db.exec("SELECT * FROM test;")
    }
  }

  @Test fun testDatabaseClose() {
    val db = assertNotNull(SQLite.inMemory())
    assertTrue(db.active)
    db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
    db.exec("INSERT INTO test (name) VALUES ('foo');")
    assertTrue(db.active)
    db.close()
    assertFalse(db.active)
    assertThrows<IllegalArgumentException> {
      db.exec("SELECT * FROM test;")
    }
    assertDoesNotThrow {
      db.close()
    }
    assertDoesNotThrow {
      db.close()
    }
  }

  @Test fun testDatabaseCloseFailure() {
    assertNotNull(SQLite.inMemory()).let { db ->
      assertTrue(db.active)
      db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      db.exec("INSERT INTO test (name) VALUES ('foo');")
      assertTrue(db.active)
      val didClose = AtomicBoolean(false)
      // register an asset which fails to close
      (db as LifecycleBoundResources).register(AutoCloseable {
        didClose.set(true)
        throw FileNotFoundException("some exotic catchable exception")
      })
      assertThrows<FileNotFoundException> {
        db.close(true)  // throw on error
      }
      assertTrue(didClose.get())
      assertFalse(db.active) // db should close anyway
      assertThrows<IllegalArgumentException> {
        db.exec("SELECT * FROM test;")
      }
      assertDoesNotThrow {
        db.close()
      }
      assertDoesNotThrow {
        db.close()
      }
    }
    assertNotNull(SQLite.inMemory()).let { db ->
      assertTrue(db.active)
      db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      db.exec("INSERT INTO test (name) VALUES ('foo');")
      assertTrue(db.active)
      val didClose = AtomicBoolean(false)
      // register an asset which fails to close
      (db as LifecycleBoundResources).register(AutoCloseable {
        didClose.set(true)
        throw FileNotFoundException("some exotic catchable exception")
      })
      assertDoesNotThrow {
        db.close(false)
      }
      assertTrue(didClose.get())
      assertFalse(db.active) // db should close anyway
      assertDoesNotThrow {
        db.close()
      }
      assertDoesNotThrow {
        db.close()
      }
    }
  }

  @Test fun `create and populate in-memory sqlite database`() = dual {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('foo');")
    }
  }.guest {
    // language=JavaScript
    """
      const { ok } = require("node:assert");
      const { Database } = require("elide:sqlite");
      ok(Database);
      const db = new Database();
      ok(db);
      db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);");
      db.exec("INSERT INTO test (name) VALUES ('foo');");
      db.close();
    """
  }

  @Test fun `create and query in-memory sqlite database via get()`() = dual {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      val query = it.query("SELECT * FROM test LIMIT 1;")
      val result = assertNotNull(query.get())
      assertTrue(result.isNotEmpty())
      assertEquals("hi", result["name"])
    }
  }.guest {
    // language=JavaScript
    """
      const { ok } = require("node:assert");
      const { Database } = require("elide:sqlite");
      ok(Database);
      const db = new Database();
      ok(db);
      db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);");
      db.exec("INSERT INTO test (name) VALUES ('hi');");
      const query = db.query("SELECT * FROM test LIMIT 1;");
      const result = query.get();
      ok(result);
      ok(result.name);
      ok(result.name === "hi");
      db.close();
    """
  }

  @Test fun `create and query in-memory sqlite database via all()`() = dual {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      val query = it.query("SELECT * FROM test LIMIT 1;")
      val results = assertNotNull(query.all())
      assertTrue(results.isNotEmpty())
      assertEquals(1, results.size)
      val result = results.first()
      assertTrue(result.isNotEmpty())
      assertEquals("hi", result["name"])
    }

    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      it.exec("INSERT INTO test (name) VALUES ('yo');")
      it.exec("INSERT INTO test (name) VALUES ('hello');")
      it.exec("INSERT INTO test (name) VALUES ('elide');")
      it.exec("INSERT INTO test (name) VALUES ('testing');")
      val query = it.query("SELECT * FROM test;")
      val results = assertNotNull(query.all())
      assertTrue(results.isNotEmpty())
      assertEquals(5, results.size)
      val result = results.first()
      assertTrue(result.isNotEmpty())
      val found = assertNotNull(results.find { entry -> entry["name"] == "hello" })
      assertEquals("hello", found["name"])
    }
  }.guest {
    // language=JavaScript
    """
      const { ok } = require("node:assert");
      const { Database } = require("elide:sqlite");
      ok(Database);
      const db = new Database();
      ok(db);
      db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);");
      db.exec("INSERT INTO test (name) VALUES ('hi');");
      const query = db.query("SELECT * FROM test LIMIT 1;");
      const results = query.all();
      ok(results);
      ok(results.length === 1);
      ok(results[0].name === "hi");

      db.exec("INSERT INTO test (name) VALUES ('yo');");
      db.exec("INSERT INTO test (name) VALUES ('hello');");
      db.exec("INSERT INTO test (name) VALUES ('elide');");
      db.exec("INSERT INTO test (name) VALUES ('testing');");
      const query2 = db.query("SELECT * FROM test;");
      const results2 = query2.all();
      ok(results2);
      ok(results2.length === 5);
      ok(results2[0].name === "hi");
      ok(results2.find(entry => entry.name === "hello"));
      db.close();
    """
  }

  @Test fun `create and query in-memory sqlite database via values()`() = dual {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      val query = it.query("SELECT * FROM test LIMIT 1;")
      val results = assertNotNull(query.values())
      assertTrue(results.isNotEmpty())
      assertEquals(1, results.size)
      val result = results.first()
      assertTrue(result.isNotEmpty())
      assertEquals(2, result.size)
      assertEquals("hi", result[1])
    }
  }.guest {
    // language=JavaScript
    """
      const { ok } = require("node:assert");
      const { Database } = require("elide:sqlite");
      ok(Database);
      const db = new Database();
      ok(db);
      db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);");
      db.exec("INSERT INTO test (name) VALUES ('hi');");
      const query = db.query("SELECT * FROM test LIMIT 1;");
      const results = query.values();
      ok(results);
      ok(results.length === 1);
      ok(results[0].length === 2);
      ok(results[0][1] === "hi");
      db.close();
    """
  }

  @Test fun `host - create and decode a row with all primitive types`() {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE persons (id INTEGER PRIMARY KEY, name TEXT, age INTEGER, height REAL, data BLOB);")
      it.exec("INSERT INTO persons (name, age, height, data) VALUES ('hi', 42, 6.0, X'0102030405060708090A');")
      val query = it.query("SELECT * FROM persons LIMIT 1;")
      val result = assertNotNull(query.get())
      assertTrue(result.isNotEmpty())
      assertEquals("hi", result["name"])
      assertEquals(42, result["age"])
      assertEquals(6.0, result["height"])
      assertEquals(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).toList(), (result["data"] as ByteArray).toList())
    }
  }

  @Test fun `host - unwrap to a jdbc connection`() {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      assertNotNull(it.connection()).let { connection ->
        assertNotNull(connection.database)
        assertNotNull(connection.connectionConfig)
        assertNotNull(connection.url)
        assertFalse(connection.isReadOnly)
      }
    }
  }

  @Test fun `host - unwrap to a sqlite database`() {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      assertNotNull(it.unwrap()).let { db ->
        assertNotNull(db.url)
        assertNotNull(db.config)
      }
    }
  }

  @Test fun `host - create a read-only sqlite database`() {
    assertNotNull(SQLite.inMemory(readonly = true)).use {
      assertTrue(it.active)
      assertNotNull(it.connection()).let { connection ->
        assertNotNull(connection.database)
        assertNotNull(connection.connectionConfig)
        assertNotNull(connection.url)
        assertTrue(connection.isReadOnly)
      }
    }
  }

  @Test fun `create and serialize a database`() = dual {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      val query = it.query("SELECT * FROM test LIMIT 1;")
      val result = assertNotNull(query.get())
      assertTrue(result.isNotEmpty())
      assertEquals("hi", result["name"])
      val serialized = it.serialize()
      assertNotNull(serialized)
      assertTrue(serialized.isNotEmpty())
      val serialized2 = it.serialize("main")
      assertNotNull(serialized2)
      assertTrue(serialized2.isNotEmpty())
      assertThrows<TypeError> {
        (it.getMember("serialize") as ProxyExecutable).execute(Value.asValue(5))
      }
      assertThrows<TypeError> {
        (it.getMember("serialize") as ProxyExecutable).execute(Value.asValue(5.5))
      }
      assertThrows<TypeError> {
        (it.getMember("serialize") as ProxyExecutable).execute(Value.asValue(true))
      }
    }
  }.guest {
    // language=JavaScript
    """
      const { ok } = require("node:assert");
      const { Database } = require("elide:sqlite");
      ok(Database);
      const db = new Database();
      ok(db);
      db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);");
      db.exec("INSERT INTO test (name) VALUES ('hi');");
      const serialized = db.serialize();
      ok(serialized);
      ok(serialized.length > 0);
      const serialized2 = db.serialize("main");
      ok(serialized2);
      ok(serialized2.length > 0);
      db.close();
    """
  }

  @Test fun `serialize rejects invalid arguments`() = dual {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      val query = it.query("SELECT * FROM test LIMIT 1;")
      val result = assertNotNull(query.get())
      assertTrue(result.isNotEmpty())
      assertEquals("hi", result["name"])
      assertNotNull(it.serialize())
    }
  }.guest {
    // language=JavaScript
    """
      const { ok, throws } = require("node:assert");
      const { Database } = require("elide:sqlite");
      ok(Database);
      const db = new Database();
      ok(db);
      db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);");
      db.exec("INSERT INTO test (name) VALUES ('hi');");
      const serialized = db.serialize();
      ok(serialized);
      ok(serialized.length > 0);
      throws(() => db.serialize(null));
      throws(() => db.serialize(5));
      db.close();
    """
  }

  @Test fun `host - create and serialize and deserialize a database`() {
    val serialized = assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      val query = it.query("SELECT * FROM test LIMIT 1;")
      val result = assertNotNull(query.get())
      assertTrue(result.isNotEmpty())
      assertEquals("hi", result["name"])
      val serialized = it.serialize()
      assertNotNull(serialized)
      assertTrue(serialized.isNotEmpty())
      serialized
    }
    assertNotNull(SQLite.inMemory()).use {
      it.deserialize(serialized)
      val query = it.query("SELECT * FROM test LIMIT 1;")
      val result = assertNotNull(query.get())
      assertNotNull(result.columns)
      assertTrue(result.columns.isNotEmpty())
      assertNotNull(result.columnTypes)
      assertTrue(result.columnTypes.isNotEmpty())
      assertTrue(result.isNotEmpty())
      assertEquals("hi", result["name"])
    }
    assertNotNull(SQLite.deserialize(serialized)).use {
      val query = it.query("SELECT * FROM test LIMIT 1;")
      val result = assertNotNull(query.get())
      assertTrue(result.isNotEmpty())
      assertEquals("hi", result["name"])
    }
  }

  @Test fun `host - can unwrap statement to jdbc type`() {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      val query = it.query("SELECT * FROM test WHERE name = 'hi';")
      assertNotNull(query.unwrap())
    }
  }

  @Test fun `can run statement plain`() = dual {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      assertDoesNotThrow {
        it.query("SELECT * FROM test WHERE name = 'hi';").run()
      }
    }
  }.guest {
    // language=JavaScript
    """
      const { ok } = require("node:assert");
      const { Database } = require("elide:sqlite");
      ok(Database);
      const db = new Database();
      ok(db);
      db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);");
      db.exec("INSERT INTO test (name) VALUES ('hi');");
      const query = db.query("SELECT * FROM test WHERE name = 'hi';");
      ok(query);
      query.run();
      db.close();
    """
  }

  @Test fun `can manually finalize a statement`() = dual {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      val statement = it.query("SELECT * FROM test WHERE name = 'hi';")
      statement.use {
        assertDoesNotThrow { it.all() }
      }
      assertThrows<Throwable> { statement.all() }
    }
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      val statement = it.query("SELECT * FROM test WHERE name = 'hi';")
      assertDoesNotThrow { statement.all() }
      assertDoesNotThrow { statement.finalize() }
      assertThrows<Throwable> { statement.all() }
    }
  }.guest {
    // language=JavaScript
    """
      const { ok } = require("node:assert");
      const { Database } = require("elide:sqlite");
      ok(Database);
      const db = new Database();
      ok(db);
      db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);");
      db.exec("INSERT INTO test (name) VALUES ('hi');");
      const query = db.query("SELECT * FROM test WHERE name = 'hi';");
      query.all();
      query.finalize();
      db.close();
    """
  }

  @Test fun `host - create and use a prepared statement`() {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      assertThrows<Throwable> { it.prepare("SELECT * FROM test WHERE name = ?;").prepare() }
      assertThrows<Throwable> { it.prepare("SELECT * FROM test WHERE name = ?;").prepare(null) }
      assertThrows<Throwable> { it.prepare("SELECT * FROM test WHERE name = ?;").prepare(emptyArray()) }
      it.prepare("SELECT * FROM test WHERE name = ?;").use { query ->
        val result = assertNotNull(query.get("hi"))
        assertTrue(result.isNotEmpty())
        assertEquals("hi", result["name"])

        // the result set is cached, so we don't need to re-bind arguments
        val result2 = assertNotNull(query.get())
        assertTrue(result2.isNotEmpty())
        assertEquals("hi", result2["name"])

        // bind a new set of arguments and run it again
        assertNull(query.get("hey"))
        val result3 = assertNotNull(query.all("hey"))
        assertTrue(result3.isEmpty())
      }
    }
  }

  @Test fun `host - try to load a database from a non-existent file`() {
    assertThrows<FileNotFoundException> {
      SQLite.atPath(Path("does-not-exist.db"))
    }
  }

  @Test fun `host - file-backed database with create-on-demand`() {
    val tmpDir = Files.createTempDirectory("elide-sqlite-test-")
    val dbPath = tmpDir.resolve("test.db")
    assertFalse(Files.exists(dbPath))
    assertTrue(Files.isWritable(dbPath.parent))
    val absolutePath = dbPath.absolute()

    SQLite.atPath(absolutePath, create = true).use {
      assertTrue(it.active)
      assertNotNull(it.connection())
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      val query = it.query("SELECT * FROM test LIMIT 1;")
      val result = assertNotNull(query.get())
      assertTrue(result.isNotEmpty())
      assertEquals("hi", result["name"])
    }

    assertTrue(Files.exists(dbPath))
    assertTrue(Files.isReadable(dbPath))

    SQLite.atPath(absolutePath).use {
      assertTrue(it.active)
      assertNotNull(it.connection())
      val query = it.query("SELECT * FROM test LIMIT 1;")
      val again = it.query(query)
      val result = assertNotNull(again.get())
      assertTrue(result.isNotEmpty())
      assertEquals("hi", result["name"])
    }
  }

  @Test fun `guest - constructor`() {
    val one = assertNotNull(SQLiteDatabaseConstructor.newInstance())
    val two = assertNotNull(SQLiteDatabaseConstructor.newInstance(Value.asValue("")))
    val three = assertNotNull(SQLiteDatabaseConstructor.newInstance(Value.asValue(":memory:")))
    assertNotSame(one, two)
    assertNotSame(two, three)
    assertNotSame(one, three)
    assertThrows<TypeError> { SQLiteDatabaseConstructor.newInstance(Value.asValue(5)) }
    assertThrows<TypeError> { SQLiteDatabaseConstructor.newInstance(Value.asValue(1.1)) }
    assertThrows<TypeError> { SQLiteDatabaseConstructor.newInstance(Value.asValue(false)) }
  }

  @Test fun `host - transaction api`() {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      val txn = it.transaction {
        exec("INSERT INTO test (name) VALUES ('yo');")
        exec("INSERT INTO test (name) VALUES ('hello');")
        exec("INSERT INTO test (name) VALUES ('elide');")
        exec("INSERT INTO test (name) VALUES ('testing');")
      }
      assertFalse(txn.isDone)
      assertFalse(txn.isCancelled)
      txn()
      val query = it.query("SELECT * FROM test;")
      val results = assertNotNull(query.all())
      assertTrue(results.isNotEmpty())
      assertEquals(5, results.size)
      val result = results.first()
      assertTrue(result.isNotEmpty())
      val found = assertNotNull(results.find { entry -> entry["name"] == "hello" })
      assertEquals("hello", found["name"])
    }
  }

  @Test fun `host - transaction should fail if database is closed`() {
    val txn = assertNotNull(SQLite.inMemory()).use {
      it.transaction {
        exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
        exec("INSERT INTO test (name) VALUES ('hi');")
      }
    }
    assertThrows<IllegalStateException> {
      txn()
    }
  }

  @Test fun `host - transaction rollback`() {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      val txn = it.transaction {
        exec("INSERT INTO test (name) VALUES ('yo');")
        exec("INSERT INTO test (name) VALUES ('hello');")
        exec("INSERT INTO test (name) VALUES ('elide');")
        exec("INSERT INTO test (name) VALUES ('testing');")
        error("oopsie")
      }
      assertFalse(txn.isDone)
      assertFalse(txn.isCancelled)
      assertTrue(runCatching { txn() }.isFailure)
      val query = it.query("SELECT * FROM test;")
      val results = assertNotNull(query.all())
      assertTrue(results.isNotEmpty())
      assertEquals(1, results.size)  // should not have been committed
      val result = results.first()
      assertTrue(result.isNotEmpty())
      val found = assertNotNull(results.find { entry -> entry["name"] == "hi" })
      assertEquals("hi", found["name"])
    }
  }

  @Test fun `host - transaction api via polyglot execution`() {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      val txn = it.transaction {
        exec("INSERT INTO test (name) VALUES ('yo');")
        exec("INSERT INTO test (name) VALUES ('hello');")
        exec("INSERT INTO test (name) VALUES ('elide');")
        exec("INSERT INTO test (name) VALUES ('testing');")
      }
      assertFalse(txn.isDone)
      assertFalse(txn.isCancelled)
      txn.execute()
      val query = it.query("SELECT * FROM test;")
      val results = assertNotNull(query.all())
      assertTrue(results.isNotEmpty())
      assertEquals(5, results.size)
      val result = results.first()
      assertTrue(result.isNotEmpty())
      val found = assertNotNull(results.find { entry -> entry["name"] == "hello" })
      assertEquals("hello", found["name"])
    }
  }

  @Test fun `host - transaction api via future`() {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      val txn = it.transaction {
        exec("INSERT INTO test (name) VALUES ('yo');")
        exec("INSERT INTO test (name) VALUES ('hello');")
        exec("INSERT INTO test (name) VALUES ('elide');")
        exec("INSERT INTO test (name) VALUES ('testing');")
      }
      assertFalse(txn.isDone)
      assertFalse(txn.isCancelled)
      txn.get()
      val query = it.query("SELECT * FROM test;")
      val results = assertNotNull(query.all())
      assertTrue(results.isNotEmpty())
      assertEquals(5, results.size)
      val result = results.first()
      assertTrue(result.isNotEmpty())
      val found = assertNotNull(results.find { entry -> entry["name"] == "hello" })
      assertEquals("hello", found["name"])
    }
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      it.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
      it.exec("INSERT INTO test (name) VALUES ('hi');")
      val txn = it.transaction {
        exec("INSERT INTO test (name) VALUES ('yo');")
        exec("INSERT INTO test (name) VALUES ('hello');")
        exec("INSERT INTO test (name) VALUES ('elide');")
        exec("INSERT INTO test (name) VALUES ('testing');")
      }
      assertFalse(txn.isDone)
      assertFalse(txn.isCancelled)
      txn.get(10, SECONDS)
      val query = it.query("SELECT * FROM test;")
      val results = assertNotNull(query.all())
      assertTrue(results.isNotEmpty())
      assertEquals(5, results.size)
      val result = results.first()
      assertTrue(result.isNotEmpty())
      val found = assertNotNull(results.find { entry -> entry["name"] == "hello" })
      assertEquals("hello", found["name"])
    }
  }

  @Test fun `guest - create in-memory database`() = dual {
    // nothing
  }.guest {
    // language=JavaScript
    """
      const { ok } = require("node:assert");
      const { Database } = require("elide:sqlite");
      ok(Database);
      ok(new Database());
      ok(new Database(""));
      ok(new Database(":memory:"));
    """
  }

  @TestFactory fun `database api`(): Stream<DynamicTest> = sequence {
    val db = SQLite.inMemory()
    for (member in db.memberKeys) {
      yield(
        dynamicTest("host member - $member") { assertNotNull(db.getMember(member)) }
      )
      dynamicGuestTest("guest member - $member") {
        // language=JavaScript
        """
          const { ok } = require("node:assert");
          const { Database } = require("elide:sqlite");
          const db = new Database();
          ok(db);
          ok(db.$member);
          db.close();
        """
      }
    }
  }.asStream()

  @TestFactory fun `transaction api`(): Stream<DynamicTest> = sequence {
    val db = SQLite.inMemory()
    db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
    val txn = db.transaction {
      exec("INSERT INTO test (name) VALUES ('yo');")
      exec("INSERT INTO test (name) VALUES ('hello');")
      exec("INSERT INTO test (name) VALUES ('elide');")
      exec("INSERT INTO test (name) VALUES ('testing');")
    }
    assertEquals(SQLiteTransactionType.AUTO, txn.type)
    assertEquals(SQLiteTransactionType.EXCLUSIVE, txn.exclusive().type)
    assertEquals(SQLiteTransactionType.DEFERRED, txn.deferred().type)
    assertEquals(SQLiteTransactionType.IMMEDIATE, txn.immediate().type)
    assertNull(txn.getMember("foo"))

    for (member in txn.memberKeys) {
      yield(
        dynamicTest("host member - $member") { assertNotNull(txn.getMember(member)) }
      )
      dynamicGuestTest("guest member - $member") {
        // language=JavaScript
        """
          const { ok } = require("node:assert");
          const { Database } = require("elide:sqlite");
          const db = new Database();
          db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);");
          const txn = db.transaction(() => {
            db.exec("INSERT INTO test (name) VALUES ('yo');");
          });
          ok(txn);
          ok(txn.$member);
        """
      }
    }
  }.asStream()

  @TestFactory fun `host invalid arg tests`(): Stream<DynamicTest> = sequence {
    assertNotNull(SQLite.inMemory()).use {
      assertTrue(it.active)
      yield(dynamicTest("`prepare` should require an argument") {
        assertThrows<TypeError> {
          (assertNotNull(it.getMember("prepare")) as ProxyExecutable).execute()
        }
      })
      yield(dynamicTest("`prepare` should reject invalid queries") {
        assertThrows<TypeError> {
          (assertNotNull(it.getMember("prepare")) as ProxyExecutable).execute(null)
        }
        assertThrows<TypeError> {
          (assertNotNull(it.getMember("prepare")) as ProxyExecutable).execute(Value.asValue(null))
        }
        assertThrows<TypeError> {
          (assertNotNull(it.getMember("prepare")) as ProxyExecutable).execute(Value.asValue(5))
        }
        assertThrows<TypeError> {
          (assertNotNull(it.getMember("prepare")) as ProxyExecutable).execute(Value.asValue(5.5))
        }
        assertThrows<TypeError> {
          (assertNotNull(it.getMember("prepare")) as ProxyExecutable).execute(Value.asValue(false))
        }
      })

      yield(dynamicTest("`query` should require an argument") {
        assertThrows<TypeError> {
          (assertNotNull(it.getMember("query")) as ProxyExecutable).execute()
        }
      })
      yield(dynamicTest("`query` should reject invalid queries") {
        assertThrows<TypeError> {
          (assertNotNull(it.getMember("query")) as ProxyExecutable).execute(null)
        }
        assertThrows<TypeError> {
          (assertNotNull(it.getMember("query")) as ProxyExecutable).execute(Value.asValue(null))
        }
        assertThrows<TypeError> {
          (assertNotNull(it.getMember("query")) as ProxyExecutable).execute(Value.asValue(5))
        }
        assertThrows<TypeError> {
          (assertNotNull(it.getMember("query")) as ProxyExecutable).execute(Value.asValue(5.5))
        }
        assertThrows<TypeError> {
          (assertNotNull(it.getMember("query")) as ProxyExecutable).execute(Value.asValue(false))
        }
      })
    }
  }.asStream()

  @TestFactory fun `guest invalid arg tests`(): Stream<DynamicTest> = sequence {
    dynamicGuestTest("`prepare` should reject invalid args") {
      // language=JavaScript
      """
          const { ok, throws } = require("node:assert");
          const { Database } = require("elide:sqlite");
          const db = new Database();
          ok(db);
          throws(() => db.prepare(null));
          throws(() => db.prepare(false));
          throws(() => db.prepare());
        """
    }
    dynamicGuestTest("`query` should reject invalid args") {
      // language=JavaScript
      """
          const { ok, throws } = require("node:assert");
          const { Database } = require("elide:sqlite");
          const db = new Database();
          ok(db);
          throws(() => db.query(null));
          throws(() => db.query(false));
          throws(() => db.query());
        """
    }
  }.asStream()

  @Test fun `should be able to require() builtin module at elide prefix`() {
    require("elide:sqlite")
  }
}

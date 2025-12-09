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
@file:Suppress("DataClassPrivateConstructor")

package elide.runtime.gvm.internals.sqlite

import com.oracle.truffle.js.runtime.objects.JSDynamicObject
import com.oracle.truffle.js.runtime.objects.Undefined
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyObject
import org.sqlite.SQLiteConnection
import org.sqlite.SQLiteOpenMode
import org.sqlite.SQLiteOpenMode.CREATE
import org.sqlite.SQLiteOpenMode.OPEN_MEMORY
import org.sqlite.SQLiteOpenMode.READONLY
import org.sqlite.SQLiteOpenMode.READWRITE
import org.sqlite.core.DB
import org.sqlite.jdbc4.JDBC4ResultSet
import java.lang.ref.WeakReference
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.files.FileNotFoundException
import kotlin.io.path.absolutePathString
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.jvm.LifecycleBoundResources
import elide.jvm.ResourceManager
import elide.runtime.core.lib.NativeLibraries
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.struct.map.JsMap
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.MapLike
import elide.runtime.intrinsics.sqlite.*
import elide.runtime.intrinsics.sqlite.SQLiteTransactionType.*
import elide.runtime.lang.javascript.SyntheticJSModule
import elide.util.UUID
import elide.vm.annotations.Polyglot
import elide.runtime.intrinsics.sqlite.SQLiteStatement as Statement
import org.sqlite.SQLiteConfig as RawConfig
import java.sql.Statement as SqlStatement

// Symbol where the internal module implementation is installed.
private const val SQLITE_MODULE_SYMBOL: String = "sqlite"

// Native library to load for SQLite support.
private const val SQLITE3_LIBRARY: String = "sqlitejdbc"

// Symbol where the database class is installed.
private const val SQLITE_DATABASE_SYMBOL: String = "sqlite_Database"

// Token for creating in-memory SQLite databases.
private const val SQLITE_IN_MEMORY_TOKEN: String = ":memory:"

// Whether to create the database file (constructor config parameter).
private const val CONFIG_ATTR_CREATE: String = "create"

// Whether to open the database as read-only (constructor config parameter).
private const val CONFIG_ATTR_READONLY: String = "readonly"

// Installs the Elide SQLite bindings.
@Intrinsic
@Factory internal class ElideSqliteModule : AbstractNodeBuiltinModule() {
  @Singleton fun provide(): SQLiteAPI = SqliteModule.obtain()

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[SQLITE_MODULE_SYMBOL.asJsSymbol()] = provide()
    bindings[SQLITE_DATABASE_SYMBOL.asJsSymbol()] = SQLiteDatabaseConstructor
  }

  init {
    ModuleRegistry.deferred(ModuleInfo.of(SQLITE_MODULE_SYMBOL)) {
      NativeLibraries.resolve(SQLITE3_LIBRARY) {
        org.sqlite.SQLiteJDBCLoader.initialize()
      }
      provide()
    }
  }
}

public class SqliteModule : ProxyObject, SyntheticJSModule<SQLiteAPI>, SQLiteAPI {
  public companion object {
    private val SINGLETON = SqliteModule()
    @JvmStatic public fun obtain(): SQLiteAPI = SINGLETON
  }

  override fun provide(): SQLiteAPI = SINGLETON

  override fun getMemberKeys(): Array<String> = arrayOf("Database")
  override fun putMember(key: String?, value: Value?): Unit = Unit
  override fun removeMember(key: String?): Boolean = false
  override fun hasMember(key: String): Boolean = key in memberKeys

  override fun getMember(key: String): Any? = when (key) {
    "Database" -> SQLiteDatabaseConstructor
    else -> null
  }
}

@Suppress("unused") private suspend fun SequenceScope<String>.addIfSet(value: String?) =
  value?.ifBlank { null }?.let { yield(it) }

private suspend fun SequenceScope<String>.addIfSet(key: String, value: String?) =
  value?.ifBlank { null }?.let { yield("$key=$it") }

// Driver-level options which are available host-side.
@JvmRecord public data class SQLiteDriverOptions(
  val cache: String? = null,
) {
  // Render the driver options to a list of strings.
  internal fun render(): Sequence<String> = sequence {
    addIfSet("cache", cache)
  }
}

// Creation options for a SQLite database instance.
@JvmRecord public data class SQLiteCreateDatabaseOptions (
  @get:Polyglot public val create: Boolean = SQLiteDatabase.DEFAULT_CREATE,
  @get:Polyglot public val readonly: Boolean = SQLiteDatabase.DEFAULT_READONLY,
) {
  public companion object {
    @JvmStatic public fun defaults(): SQLiteCreateDatabaseOptions = SQLiteCreateDatabaseOptions()

    @Suppress("KotlinConstantConditions")
    @JvmStatic public fun from(value: Any?): SQLiteCreateDatabaseOptions {
      return when (value) {
        null -> defaults()
        is Value -> SQLiteCreateDatabaseOptions(
          create = if (value.hasMember(CONFIG_ATTR_CREATE))
            value.getMember(CONFIG_ATTR_CREATE).asBoolean()
          else
            SQLiteDatabase.DEFAULT_CREATE,
          readonly = if (value.hasMember(CONFIG_ATTR_READONLY))
            value.getMember(CONFIG_ATTR_READONLY).asBoolean()
          else
            SQLiteDatabase.DEFAULT_READONLY,
        )

        else -> throw IllegalArgumentException("Cannot convert value to SQLite database options: $value")
      }
    }
  }

  // Render a suite of `Properties`, and connection string options (as a `Sequence<String>`) for a new database
  // connection which is being opened.
  internal fun renderFor(
    path: Path?,
    driver: SQLiteDriverOptions?,
  ): Pair<Properties, Sequence<String>> = openProperties(path, driver) to sequence {
    // force in-memory mode if the database does not specify a path
    if (path == null) yield("mode=memory")
    driver?.let { yieldAll(it.render()) }
  }

  // Build a suite of properties when opening a new connection via the JDBC driver.
  private fun openProperties(path: Path?, driver: SQLiteDriverOptions?): Properties = RawConfig().also {
    it.setOpenMode(effectiveOpenMode(path == null))
    it.setEncoding(RawConfig.Encoding.UTF8)
    it.setReadOnly(readonly)
    it.enableLoadExtension(true)
    driver?.let { opts -> it.setSharedCache(opts.cache == "shared") }
  }.toProperties().also {
    if (readonly) it.setProperty("jdbc.explicit_readonly", "true")
  }

  // Resolve the effective open mode to use for the database.
  private fun effectiveOpenMode(inMemory: Boolean): SQLiteOpenMode {
    return when {
      inMemory -> OPEN_MEMORY
      readonly -> READONLY
      create -> CREATE
      else -> READWRITE
    }
  }
}

// Proxy constructor for SQLite database instances.
internal object SQLiteDatabaseConstructor : ProxyInstantiable {
  override fun newInstance(vararg arguments: Value?): Any {
    val firstIfAny = arguments.getOrNull(0)

    return when {
      // `new Database()`
      firstIfAny == null -> SqliteDatabaseProxy.inMemory()

      // `new Database('some-string')`
      firstIfAny.isString -> firstIfAny.asString().let { subject ->
        if (subject == "" || subject == SQLITE_IN_MEMORY_TOKEN) {
          SqliteDatabaseProxy.inMemory()
        } else SQLiteCreateDatabaseOptions.from(arguments.getOrNull(1)).let { options ->
          SqliteDatabaseProxy.atFile(Path.of(firstIfAny.asString()), options)
        }
      }

      // any other constructor type is invalid
      else -> throw JsError.typeError("Invalid arguments to Database constructor: $arguments")
    }
  }
}

// Properties and methods made available to guests on SQLite objects.
private val sqliteObjectPropsAndMethods = arrayOf<String>()

// Effective configuration for a SQLite database instance.
@JvmRecord private data class SQLiteConfig(
  val path: Path?,
  val params: SQLiteCreateDatabaseOptions,
)
// Implementation of a SQLite database object.
internal class SqliteDatabaseProxy private constructor (
  private val config: SQLiteConfig,
  private val resources: ResourceManager = ResourceManager(),
  driverOptions: SQLiteDriverOptions? = null,
) : LifecycleBoundResources by resources, SQLiteDatabase {
  @JvmRecord private data class SQLiteColumn private constructor (
    val name: String,
    val type: SQLiteType,
  ) {
    companion object {
      fun of(name: String, type: SQLiteType): SQLiteColumn = SQLiteColumn(name, type)
    }
  }

  // Reusable information decoded from result set metadata.
  private class SQLiteObjectSchema private constructor (
    val columnCount: Int,
    val columnNames: Array<String>,
    val columns: Array<SQLiteColumn>,
  ) {
    companion object {
      fun buildFrom(metadata: ResultSetMetaData): SQLiteObjectSchema {
        val columnCount = metadata.columnCount
        val columnNames = Array(columnCount) { metadata.getColumnName(it + 1) }
        val columns = Array(columnCount) { i ->
          SQLiteColumn.of(columnNames[i], SQLitePrimitiveType.resolve(metadata.getColumnTypeName(i + 1)))
        }
        return SQLiteObjectSchema(columnCount, columnNames, columns)
      }
    }
  }

  // Internal SQLiteChanges implementation
  private data class SQLiteChangesImpl(
    override val changes: Long,
    override val lastInsertRowid: Long,
  ) : SQLiteChanges, ProxyObject {
    override fun getMemberKeys(): Array<String> = arrayOf("changes", "lastInsertRowid")
    override fun hasMember(key: String): Boolean = key == "changes" || key == "lastInsertRowid"
    override fun getMember(key: String): Any? = when (key) {
      "changes" -> changes
      "lastInsertRowid" -> lastInsertRowid
      else -> null
    }
    override fun putMember(key: String?, value: Value?) {
      throw UnsupportedOperationException("SQLiteChanges is immutable")
    }

    companion object {
      val EMPTY = SQLiteChangesImpl(0, 0)
    }
  }

  // Internal SQLite object implementation, backed by a de-serialized map.
  private data class SQLiteObjectImpl private constructor (
    private val schema: SQLiteObjectSchema,
    private val dataMap: MapLike<String, Any?>,
  ): SQLiteObject, MapLike<String, Any?> by dataMap {
    override val columns: Array<String> get() = schema.columnNames
    override val columnTypes: Map<String, SQLiteType> get() = schema.columns.associate { it.name to it.type }
    override fun asList(): List<Any?> = schema.columns.map { dataMap[it.name] }.toImmutableList()

    override fun getMemberKeys(): Array<String> = sqliteObjectPropsAndMethods.plus(
      dataMap.keys
    )

    override fun hasMember(key: String): Boolean = key in sqliteObjectPropsAndMethods || key in dataMap

    override fun getMember(key: String): Any? = when (key) {
      else -> dataMap[key] ?: Undefined.instance
    }

    companion object {
      fun from(schema: SQLiteObjectSchema, data: MapLike<String, Any?>): SQLiteObject = SQLiteObjectImpl(
        schema,
        data,
      )

      fun single(schema: SQLiteObjectSchema, resultSet: ResultSet): List<SQLiteObject> =
        Collections.singletonList(of(schema, resultSet))

      fun of(schema: SQLiteObjectSchema, resultSet: ResultSet): SQLiteObject {
        val dataMap = LinkedHashMap<String, Any?>()
        for (i in 1..schema.columnCount) {
          val name = schema.columnNames[i - 1]
          val value = resultSet.getObject(i)
          dataMap[name] = value
        }
        return from(schema, JsMap.of(dataMap))
      }
    }
  }

  // Internal compiled statement implementation.
  private class SQLiteStatementImpl private constructor (
    private val db: WeakReference<SQLiteDatabase>,
    private val sql: SqlStatement,
    private val query: String,
  ): Statement {
    companion object {
      fun boundTo(
        database: SQLiteDatabase,
        statement: SqlStatement,
        query: String,
      ): SQLiteStatementImpl = SQLiteStatementImpl(
        WeakReference(database),
        statement,
        query,
      )
    }

    // Whether we have prepared at least one query.
    private val initialized: AtomicBoolean = AtomicBoolean(false)

    // Prepared query to execute; filled lazily or eagerly depending on calls to `prepare`.
    private val prepared: AtomicReference<PreparedStatement> = AtomicReference(null)

    // Last-seen result set from this query; emptied at close.
    private val lastSeenResultSet: AtomicReference<List<SQLiteObject>> = AtomicReference(null)

    override fun close() {
      if (initialized.get()) {
        db.clear()
        lastSeenResultSet.set(null)
        prepared.get()?.close()
        prepared.set(null)
        sql.close()
      }
    }

    private fun invalidate() {
      prepared.get()?.close()
      prepared.set(null)
      lastSeenResultSet.set(null)
    }

    private fun buildResultSet(resultSet: JDBC4ResultSet, limit: Int): List<SQLiteObject> {
      // bail if the result-set is empty
      if (resultSet.emptyResultSet) return emptyList()

      // build reusable schema first
      val metadata = resultSet.metaData
      val schema = SQLiteObjectSchema.buildFrom(metadata)

      // short-circuit if we know there is a maximum of one result (for instance, via `SQLiteStatement.get`)
      if (limit == 1) return SQLiteObjectImpl.single(schema, resultSet)

      val results: MutableList<SQLiteObject> = if (limit > 0) ArrayList(limit) else LinkedList()
      while (resultSet.next()) {
        results.add(SQLiteObjectImpl.of(schema, resultSet))
        if (limit > 0 && results.size >= limit) break
      }
      return results.toImmutableList()
    }

    private inline fun <R> withResultSet(
      args: Array<out Any?>,
      limit: Int = 0,
      crossinline block: (Sequence<SQLiteObject>) -> R,
    ): R {
      assert(db.get() != null) { "Database is closed" }
      val resultSet = lastSeenResultSet.get()
      val argsAreEmpty = args.isEmpty()

      return when {
        // if we have a valid and cached result set, use it
        resultSet != null && argsAreEmpty -> block.invoke(resultSet.asSequence())

        // otherwise, if we are empowered to execute the query, execute it
        else -> {
          // prepare on-demand @TODO caching here
          val prepared = prepare(args)

          // execute and assign result set
          prepared.executeQuery().use {
            require(it is JDBC4ResultSet)
            buildResultSet(it, limit).let { results ->
              lastSeenResultSet.set(results)
              block.invoke(results.asSequence())
            }
          }
        }
      }
    }

    @Suppress("SqlSourceToSinkFlow")
    override fun prepare(args: Array<out Any?>?): PreparedStatement {
      val alreadyInitialized = initialized.get()
      if (alreadyInitialized) invalidate()
      return requireNotNull(db.get()).connection().prepareStatement(
        if (args != null && args.isNotEmpty()) {
          SqliteQueryRenderer.render(query, args)
        } else {
          SqliteQueryRenderer.render(query)
        },
      ).also {
        prepared.set(it)
        if (!alreadyInitialized) initialized.set(true)
      }
    }

    override fun unwrap(): java.sql.Statement = sql

    @Polyglot override fun all(vararg args: Any?): List<SQLiteObject> = withResultSet(args) {
      it.toImmutableList()
    }

    @Polyglot override fun values(vararg args: Any?): List<List<Any?>> = withResultSet(args) { results ->
      results.map { it.asList() }.toImmutableList()
    }

    @Polyglot override fun get(vararg args: Any?): SQLiteObject? = withResultSet(args, limit = 1) { resultSet ->
      // we use the result set and throw it away immediately, rather than registering it, because we are only
      // interested in retrieving and decoding one object via this method.
      resultSet.firstOrNull()
    }

    @Polyglot override fun run(vararg args: Any?): SQLiteChanges {
      return requireNotNull(db.get()) { "Database is closed" }.exec(this, *args)
    }

    @Polyglot @JvmSynthetic override fun finalize() {
      close()
    }
  }

  // Implementation of a transaction wrapper type.
  private class SQLiteTransactionImpl<R> private constructor (
    private val db: WeakReference<SQLiteDatabase>,
    private val transactor: SQLiteTransactor<R>,
    private val mode: SQLiteTransactionType,
  ): SQLiteTransaction<R> {
    companion object {
      fun <R> boundTo(database: SQLiteDatabase, transactor: SQLiteTransactor<R>): SQLiteTransactionImpl<R> =
        SQLiteTransactionImpl(WeakReference(database), transactor, AUTO)
    }

    // Whether the transaction has completed.
    private val done: AtomicBoolean = AtomicBoolean(false)

    // Whether the transaction has cancelled.
    private val cancelled: AtomicBoolean = AtomicBoolean(false)

    // Return the same transaction but with a different mode.
    private fun withMode(mode: SQLiteTransactionType): SQLiteTransaction<R> =
      SQLiteTransactionImpl(db, transactor, mode)

    // Make sure the database hasn't been closed before we begin a transactional operation.
    private inline fun withOpen(crossinline block: SQLiteDatabase.() -> R): R =
      block.invoke(db.get().also {
        if (it == null || !it.active) error("Database is closed")
      }!!)

    override val type: SQLiteTransactionType get() = mode
    @Polyglot override fun deferred(): SQLiteTransaction<R> = withMode(DEFERRED)
    @Polyglot override fun immediate(): SQLiteTransaction<R> = withMode(IMMEDIATE)
    @Polyglot override fun exclusive(): SQLiteTransaction<R> = withMode(EXCLUSIVE)
    @Polyglot override fun isCancelled(): Boolean = cancelled.get()
    @Polyglot override fun isDone(): Boolean = done.get()
    @Polyglot override fun get(): R = invoke()

    @Polyglot override fun get(timeout: Long, unit: TimeUnit): R = withOpen {
      runBlocking {
        withTimeout(unit.toMillis(timeout)) {
          transactor.dispatch(this@withOpen, emptyArray())
        }
      }
    }

    override fun getMember(key: String?): Any? = when (key) {
      "deferred" -> ProxyExecutable { deferred() }
      "immediate" -> ProxyExecutable { immediate() }
      "exclusive" -> ProxyExecutable { exclusive() }
      else -> null
    }

    // Entrypoint for transaction execution.
    @Polyglot override fun invoke(vararg args: Any?): R = withOpen {
      exec("BEGIN;")
      try {
        transactor.dispatch(this, args).also {
          exec("COMMIT;")
        }
      } catch (err: Throwable) {
        exec("ROLLBACK;")
        throw err
      }
    }
  }

  // Connection to the SQLite database.
  private val connection: AtomicReference<SQLiteConnection> = AtomicReference(null)

  // Whether this database instance is open.
  private val open: AtomicBoolean = AtomicBoolean(false)

  // Resolved absolute path to the database, if available/applicable.
  private val path: AtomicReference<Path> = AtomicReference(null)

  init {
    initialize(driverOptions)
  }

  // Perform early database initialization; read the database if provided with a path.
  private fun initialize(driverOptions: SQLiteDriverOptions?) {
    when (val target = config.path) {
      // nothing to initialize; it's an in-memory database. open an in-memory connection.
      null -> openDatabase(driverOptions = driverOptions)

      else -> resolvePath(path, target).let { path ->
        val exists = Files.exists(path)
        val create = config.params.create

        when {
          // if we have a path, and it exists, we should open it as a database
          exists && Files.isReadable(path) -> openDatabase(path, driverOptions)

          // if we have a path, and it doesn't exist, and we are told to create it, touch the file preemptively
          !exists && create && Files.isWritable(path.parent) -> openDatabase(path, driverOptions)

          // otherwise, throw
          else -> throw FileNotFoundException("Database file does not exist or is not readable")
        }
      }
    }
  }

  // Open an existing database file for access.
  private fun openDatabase(path: Path? = null, driverOptions: SQLiteDriverOptions? = null) {
    config.params.renderFor(path, driverOptions ?: SQLiteDriverOptions()).let { (props, params) ->
      (DriverManager.getConnection(
        renderConnectionString(path, params),
        props,
      ) as SQLiteConnection).also {
        connection.compareAndSet(null, it)
        open.compareAndSet(false, true)
        resources.register(it)
      }
    }
  }

  private inline fun <R> withOpen(crossinline block: SQLiteConnection.() -> R): R {
    require(open.get()) { "Database is closed" }
    val connection = connection.get()
    assert(connection != null)
    return block.invoke(connection)
  }

  // Utility to create (and potentially render) a statement within the context of the active connection.
  private fun SQLiteConnection.statement(register: Boolean, query: String): Statement {
    return SQLiteStatementImpl.boundTo(
      this@SqliteDatabaseProxy,
      this.createStatement().also { if (register) resources.register(it) },
      query,
    )
  }

  // Utility to create a one-shot statement which is not registered for resource control.
  private fun SQLiteConnection.oneShotStatement(query: String, args: Array<out Any?>?): Statement =
    statement(false, query).also { if (args?.isNotEmpty() == true) it.prepare(args) }

  override val active: Boolean get() = open.get()

  override fun connection(): SQLiteConnection = withOpen { this }
  override fun unwrap(): DB = withOpen { database }

  @Polyglot override fun loadExtension(extension: String): JSDynamicObject = withOpen {
    unwrap().enable_load_extension(true)
    require(';' !in extension && ' ' !in extension) { "Invalid extension name" } // sanity check
    exec("SELECT load_extension('$extension')")
    Undefined.instance
  }

  @Polyglot override fun prepare(statement: String, vararg args: Any?): Statement = withOpen {
    statement(true, statement).also {
      if (args.isNotEmpty()) it.prepare(args)
    }
  }

  @Polyglot override fun query(statement: String, vararg args: Any?): Statement = withOpen {
    statement(true, statement).also {
      if (args.isNotEmpty()) it.prepare(args)
    }
  }

  @Polyglot override fun query(statement: Statement, vararg args: Any?): Statement = withOpen {
    statement.also {
      if (args.isNotEmpty()) it.prepare(args)
    }
  }

  @Polyglot override fun exec(statement: String, vararg args: Any?): SQLiteChanges = withOpen {
    exec(oneShotStatement(statement, args))
  }

  @Polyglot override fun exec(statement: Statement, vararg args: Any?): SQLiteChanges = withOpen {
    val prepared = statement.prepare(args)
    prepared.use { stmt ->
      stmt.execute()
      val updateCount = stmt.updateCount.toLong()
      // Get last insert rowid via SQLite function
      val lastRowId = connection().prepareStatement("SELECT last_insert_rowid()").use { rowIdStmt ->
        rowIdStmt.executeQuery().use { rs ->
          if (rs.next()) rs.getLong(1) else 0L
        }
      }
      SQLiteChangesImpl(updateCount, lastRowId)
    }
  }

  @Polyglot override fun <R> transaction(runnable: SQLiteTransactor<R>): SQLiteTransaction<R> = withOpen {
    SQLiteTransactionImpl.boundTo(this@SqliteDatabaseProxy, runnable)
  }

  @Polyglot override fun serialize(schema: String): ByteArray = withOpen {
    unwrap().serialize(schema)
  }

  override fun deserialize(data: ByteArray, schema: String) = withOpen {
    unwrap().deserialize(schema, data)
  }

  @Polyglot override fun dispose() {
    close(false)
  }

  @Polyglot override fun close(throwOnError: Boolean) {
    if (!open.get()) return  // already closed

    // persist the database first (before closing resources), otherwise the database is not available for us to persist.
    val captured: LinkedList<Throwable> = LinkedList()
    var capturedErr = false

    fun closeCapture(resource: AutoCloseable) {
      try {
        resource.close()
      } catch (err: Throwable) {
        capturedErr = true
        captured.add(err)
      }
    }

    // next, if no errors have occurred, close all shared resources associated with this database. this includes things
    // like database statements, transactions, and ultimately the connection itself.
    val connection = requireNotNull(connection.get())
    for (resource in resources.allResources) {
      // we need to wait until everything is closed to close the connection
      if (resource != connection) closeCapture(resource)
    }

    // finally, close the connection.
    if (!capturedErr) closeCapture(connection)

    // always closes even if there are errors
    open.compareAndSet(true, false)

    // finally, throw error if requested
    if (capturedErr && throwOnError)
      captured.first().let { throw it }
  }

  override fun getMember(key: String?): Any? = when (key) {
    "loadExtension" -> ProxyExecutable {
      loadExtension(it[0].asString())
    }

    "prepare" -> ProxyExecutable {
      val query = it.getOrNull(0)
        ?: throw JsError.typeError("Must provide query to `prepare`")
      if (!query.isString) throw JsError.typeError("Invalid query type")
      val rest = it.drop(1).toTypedArray()
      prepare(query.asString(), *rest)
    }

    "query" -> ProxyExecutable {
      val query = it.getOrNull(0) ?: throw JsError.typeError("Must provide query")
      when {
        query.isString -> query(query.asString(), *it.drop(1).toTypedArray())
        query.isHostObject -> query(query.asHostObject<Statement>(), *it.drop(1).toTypedArray())
        else -> throw JsError.typeError("Invalid query type")
      }
    }

    "exec" -> ProxyExecutable {
      val query = it.getOrNull(0) ?: throw JsError.typeError("Must provide query")
      when {
        query.isString -> exec(query.asString(), *it.drop(1).toTypedArray())
        query.isHostObject -> exec(query.asHostObject<Statement>(), *it.drop(1).toTypedArray())
        else -> throw JsError.typeError("Invalid query type")
      }
    }

    "serialize" -> ProxyExecutable {
      when (val firstIfAny = it.getOrNull(0)) {
        null -> serialize()
        else -> when {
          firstIfAny.isString -> serialize(firstIfAny.asString())
          else -> throw JsError.typeError("Invalid argument to `serialize`")
        }
      }
    }

    "close" -> ProxyExecutable {
      close(it.getOrNull(0)?.asBoolean() ?: false)
    }

    "transaction" -> ProxyExecutable {
      val first = it.getOrNull(0) ?: throw JsError.typeError(
        "Must provide transaction function"
      )
      when {
        first.canExecute() -> transaction { args -> first.execute(*args) }
        else -> throw JsError.typeError("Invalid argument to `transaction`")
      }
    }

    else -> null
  }

  companion object {
    // Token to prefix database connections with.
    private const val JDBC_SQLITE_TOKEN: String = "jdbc:sqlite"

    // Normalize and resolve a path.
    private fun resolvePath(atomic: AtomicReference<Path>, path: Path): Path = path.normalize().toAbsolutePath().also {
      atomic.compareAndSet(null, path)
    }

    private fun Sequence<String>.renderToOptions(): String = joinToString(separator = "&", prefix = "?") { it }.let {
      if (it == "?") "" else it
    }

    // Render a JDBC/SQLite connection string, optionally for a provided path.
    private fun renderConnectionString(path: Path? = null, options: Sequence<String>): String = when (path) {
      // with no path, we should use an in-memory connection string
      null -> "$JDBC_SQLITE_TOKEN:${UUID.random()}.db${options.renderToOptions()}"

      // with a path, we should render it into the connection string (safely)
      else -> "$JDBC_SQLITE_TOKEN:${path.absolutePathString()}${options.renderToOptions()}"
    }

    // Create an implementation of an in-memory SQLite database.
    fun inMemory(options: SQLiteCreateDatabaseOptions? = null): SQLiteDatabase =
      SqliteDatabaseProxy(SQLiteConfig(null, options ?: SQLiteCreateDatabaseOptions.defaults()))

    // Create an implementation of a file-bound SQLite database.
    fun atFile(file: Path, options: SQLiteCreateDatabaseOptions): SQLiteDatabase =
      SqliteDatabaseProxy(SQLiteConfig(file, options))

    // De-serialize raw bytes into a SQLite database.
    fun fromSerialized(data: ByteArray, schema: String = SQLiteDatabase.MAIN_SCHEMA): SQLiteDatabase =
      inMemory().also { it.deserialize(data, schema) }
  }
}


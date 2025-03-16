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

@file:OptIn(DelicateElideApi::class)
@file:Suppress("unused")

package benchmarks.gvm

import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.Setup
import kotlinx.benchmark.TearDown
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotEngine
import elide.runtime.gvm.GraalVMGuest
import elide.runtime.gvm.internals.sqlite.SqliteModule
import elide.runtime.plugins.js.JavaScript

/** Tests for guest SQLite performance. */
@Suppress("DuplicatedCode")
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
class SQLiteBenchmark {
  private companion object {
    // language=JavaScript
    private const val setupSqliteCode = """
      function run() {
        const db = new Database()
        db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
        db.exec("INSERT INTO test (name) VALUES ('foo');")
        db.exec("INSERT INTO test (name) VALUES ('bar');")
        db.exec("INSERT INTO test (name) VALUES ('baz');")
      }
      run;
    """

    // language=JavaScript
    private const val execSqliteCode = """
      function exec() {
        const query = db.query("SELECT COUNT(*) FROM test;");
        const result = query.get();
        return result.name;
      }
      exec;
    """

    private val setupSqlite = Source.newBuilder("js", setupSqliteCode, "setup-sqlite.js")
      .cached(true)
      .build()

    private val execSqlite = Source.newBuilder("js", execSqliteCode, "exec-sqlite.js")
      .cached(true)
      .build()
  }

  private lateinit var engine: PolyglotEngine
  private lateinit var context: PolyglotContext
  private lateinit var warmedContext: PolyglotContext
  private lateinit var handle1: Value
  private lateinit var handle2: Value

  @Setup fun warmupContext() {
    engine = PolyglotEngine {
      enableLanguage(GraalVMGuest.JAVASCRIPT)
      install(JavaScript) {
        npm { enabled = false }
      }
    }
    context = engine.acquire()
    context.bindings(GraalVMGuest.JAVASCRIPT).apply {
      putMember("Database", SqliteModule.obtain().getMember("Database"))
    }
    context.let {
      try {
        it.enter()
        handle1 = it.evaluate(setupSqlite)
      } finally {
        it.leave()
      }
    }

    warmedContext = engine.acquire().apply {
      bindings(GraalVMGuest.JAVASCRIPT).apply {
        val ctor = SqliteModule.obtain().getMember("Database")
        putMember("Database", ctor)
        val instance = (ctor as ProxyInstantiable).newInstance()
        putMember("db", instance)
      }
    }.let {
      try {
        it.enter()
        it.evaluate(Source.newBuilder("js", """
          db.exec("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT);")
          db.exec("INSERT INTO test (name) VALUES ('foo');")
          db.exec("INSERT INTO test (name) VALUES ('bar');")
          db.exec("INSERT INTO test (name) VALUES ('baz');")
        """.trimIndent(), "initDb.js").build())
        handle2 = it.evaluate(execSqlite)
      } finally {
        it.leave()
      }
      it
    }
    context.enter()
    warmedContext.enter()
  }

  @TearDown fun cleanup() {
    context.leave()
    warmedContext.leave()
    engine.unwrap().close()
  }

  private fun PolyglotContext.exec(source: Source): Value {
    return context.let {
      try {
        it.enter()
        return it.evaluate(source)
      } finally {
        it.leave()
      }
    }
  }

  @Benchmark @Threads(1) fun createInMemoryDb(): Value {
    return handle1.execute()
  }

  @Benchmark @Threads(1) fun execInMemoryDbQuery(): Value {
    return handle2.execute()
  }
}

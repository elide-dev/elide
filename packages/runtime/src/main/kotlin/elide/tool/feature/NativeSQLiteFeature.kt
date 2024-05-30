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
package elide.tool.feature

import org.graalvm.nativeimage.Platform
import org.graalvm.nativeimage.hosted.Feature.*
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization
import org.graalvm.nativeimage.hosted.RuntimeJNIAccess
import org.sqlite.*
import org.sqlite.Function
import org.sqlite.Function.Aggregate
import org.sqlite.Function.Window
import org.sqlite.SQLiteJDBCLoader.VersionHolder
import org.sqlite.core.DB
import org.sqlite.core.DB.ProgressObserver
import org.sqlite.core.NativeDB
import org.sqlite.jdbc3.JDBC3DatabaseMetaData
import org.sqlite.util.LibraryLoaderUtil
import org.sqlite.util.OSInfo
import elide.annotations.internal.VMFeature
import elide.runtime.feature.engine.AbstractStaticNativeLibraryFeature

@VMFeature class NativeSQLiteFeature : AbstractStaticNativeLibraryFeature() {
  override fun getDescription(): String = "Registers native SQLite access"

  override fun nativeLibs(access: BeforeAnalysisAccess) = listOfNotNull(
    nativeLibrary(linux = libraryNamed(
      "sqlitejdbc",
      "org.sqlite.core.NativeDB",
    )),
  )

  override fun unpackNatives(access: BeforeAnalysisAccess) {
    when {
      Platform.includedIn(Platform.LINUX::class.java) -> when (System.getProperty("os.arch")) {
        "x86_64" -> access.unpackLibrary(
          "sqlite-jdbc",
          "sqlitejdbc",
          "x86-64",
          "org/sqlite/native/Linux/x86_64/libsqlitejdbc.so",
        )

        "aarch64" -> access.unpackLibrary(
          "sqlite-jdbc",
          "sqlitejdbc",
          "x86-64",
          "org/sqlite/native/Linux/aarch64/libsqlitejdbc.so",
        )
      }

      Platform.includedIn(Platform.DARWIN::class.java) -> when (System.getProperty("os.arch")) {
        "x86_64" -> access.unpackLibrary(
          "sqlite-jdbc",
          "sqlitejdbc",
          "x86-64",
          "org/sqlite/native/Mac/x86_64/libsqlitejdbc.dylib",
        )

        "aarch64" -> access.unpackLibrary(
          "sqlite-jdbc",
          "sqlitejdbc",
          "x86-64",
          "org/sqlite/native/Mac/aarch64/libsqlitejdbc.dylib",
        )
      }
    }
  }

  private fun onDbReachable() {
    RuntimeJNIAccess.register(NativeDB::class.java)
    RuntimeJNIAccess.register(*fields(
      NativeDB::class.java,
      "pointer",
      "busyHandler",
      "commitListener",
      "updateListener",
      "progressHandler",
    ))
    RuntimeJNIAccess.register(method(
      DB::class.java,
      "onUpdate",
      Integer.TYPE,
      String::class.java,
      String::class.java,
      java.lang.Long.TYPE,
    ))
    RuntimeJNIAccess.register(method(DB::class.java, "onCommit", java.lang.Boolean.TYPE))
    RuntimeJNIAccess.register(method(NativeDB::class.java, "stringToUtf8ByteArray", String::class.java))
    RuntimeJNIAccess.register(method(DB::class.java, "throwex"))
    RuntimeJNIAccess.register(method(DB::class.java, "throwex", Integer.TYPE))
    RuntimeJNIAccess.register(method(NativeDB::class.java, "throwex", String::class.java))
    RuntimeJNIAccess.register(Function::class.java)
    RuntimeJNIAccess.register(*this.fields(Function::class.java, "context", "value", "args"))
    RuntimeJNIAccess.register(method(Function::class.java, "xFunc"))
    RuntimeJNIAccess.register(Collation::class.java)
    RuntimeJNIAccess.register(method(
      Collation::class.java,
      "xCompare",
      String::class.java,
      String::class.java,
    ))
    RuntimeJNIAccess.register(Aggregate::class.java)
    RuntimeJNIAccess.register(method(Aggregate::class.java, "xStep"))
    RuntimeJNIAccess.register(method(Aggregate::class.java, "xFinal"))
    RuntimeJNIAccess.register(method(Aggregate::class.java, "clone"))
    RuntimeJNIAccess.register(Window::class.java)
    RuntimeJNIAccess.register(method(Window::class.java, "xInverse"))
    RuntimeJNIAccess.register(method(Window::class.java, "xValue"))
    RuntimeJNIAccess.register(ProgressObserver::class.java)
    RuntimeJNIAccess.register(method(ProgressObserver::class.java, "progress", Integer.TYPE, Integer.TYPE))
    RuntimeJNIAccess.register(ProgressHandler::class.java)
    RuntimeJNIAccess.register(method(ProgressHandler::class.java, "progress"))
    RuntimeJNIAccess.register(BusyHandler::class.java)
    RuntimeJNIAccess.register(method(BusyHandler::class.java, "callback", Integer.TYPE))
    RuntimeJNIAccess.register(Throwable::class.java)
    RuntimeJNIAccess.register(method(Throwable::class.java, "toString"))
    RuntimeJNIAccess.register(BooleanArray::class.java)
  }

  private fun initializeSqlite() {
    assert(SQLiteJDBCLoader.initialize()) { "Failed to initialize SQLite" }
    assert(SQLiteJDBCLoader.isNativeMode()) { "Native mode for SQLite should be enabled" }
  }

  override fun beforeAnalysis(access: BeforeAnalysisAccess) {
    super.beforeAnalysis(access)
    RuntimeClassInitialization.initializeAtBuildTime(VersionHolder::class.java)
    RuntimeClassInitialization.initializeAtBuildTime(JDBC3DatabaseMetaData::class.java)
    RuntimeClassInitialization.initializeAtBuildTime(OSInfo::class.java)
    RuntimeClassInitialization.initializeAtBuildTime(LibraryLoaderUtil::class.java)
    access.registerReachabilityHandler(
      { onDbReachable().also { initializeSqlite() } },
      method(SQLiteJDBCLoader::class.java, "initialize"),
    )
  }
}

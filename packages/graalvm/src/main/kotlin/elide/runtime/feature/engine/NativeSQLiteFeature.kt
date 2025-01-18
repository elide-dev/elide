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
@file:Suppress("SpreadOperator")

package elide.runtime.feature.engine

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
import java.sql.DriverManager
import elide.annotations.internal.VMFeature
import elide.runtime.feature.NativeLibraryFeature.NativeLibInfo
import elide.runtime.feature.NativeLibraryFeature.NativeLibType.STATIC

/** Mounts the SQLite native library via static JNI. */
@VMFeature public class NativeSQLiteFeature : AbstractStaticNativeLibraryFeature() {
  private companion object {
    private const val STATIC_JNI = true
    private const val LIBMATH = "m"
    private const val SQLITEJDBC_LIB = "sqlitejdbc"
    private const val NATIVE_DB = "org.sqlite.core.NativeDB"
  }

  override fun getDescription(): String = "Registers native SQLite access"

  override fun nativeLibs(access: BeforeAnalysisAccess): List<NativeLibInfo> = if (STATIC_JNI) listOfNotNull(
    nativeLibrary(singular = libraryNamed(SQLITEJDBC_LIB, NATIVE_DB, type = STATIC, deps = (
        // on linux, we need to additionally link against the `math` library, which is at `libm`
        if (Platform.includedIn(Platform.LINUX::class.java) && STATIC_JNI) listOf(LIBMATH) else emptyList()
    ))),
  ) else emptyList()

  private fun onDbReachable() {
    val driver = DriverManager.getDriver("jdbc:sqlite:memory:")
    requireNotNull(driver) { "Failed to resolve SQLite driver at build-time" }
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
    RuntimeJNIAccess.register(*fields(Function::class.java, "context", "value", "args"))
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

  override fun beforeAnalysis(access: BeforeAnalysisAccess) {
    super.beforeAnalysis(access)
    onDbReachable()
    RuntimeClassInitialization.initializeAtBuildTime(VersionHolder::class.java)
    RuntimeClassInitialization.initializeAtBuildTime(JDBC3DatabaseMetaData::class.java)
    RuntimeClassInitialization.initializeAtBuildTime(OSInfo::class.java)
    RuntimeClassInitialization.initializeAtBuildTime(LibraryLoaderUtil::class.java)
  }
}

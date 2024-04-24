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
package elide.runtime.gvm.vfs

import org.graalvm.polyglot.io.FileSystem
import java.nio.file.Path
import elide.runtime.gvm.internals.GuestVFS
import elide.runtime.gvm.internals.LanguageVFS

/**
 * TBD.
 */
public object LanguageVFS {
  /**
   *
   */
  public interface LanguageVFSInfo {
    /**
     *
     */
    public val router: (Path) -> Boolean

    /**
     *
     */
    public val fsProvider: () -> FileSystem
  }

  /**
   * TBD.
   */
  public fun delegate(language: String, fs: () -> LanguageVFSInfo): LanguageVFS {
    var instance: FileSystem? = null
    var router: (Path) -> Boolean = { false }
    val provide = {
      if (instance == null) {
        fs().let {
          router = it.router
          instance = it.fsProvider()
        }
      }
      requireNotNull(instance)
    }
    return object : LanguageVFS, FileSystem by provide() {
      override val languageId: String get() = language
      override val writable: Boolean get() = false
      override val deletable: Boolean get() = false
      override val virtual: Boolean get() = true
      override val host: Boolean get() = false
      override val compound: Boolean get() = false
      override val supportsSymlinks: Boolean get() = true
      override fun allowsHostFileAccess(): Boolean = false
      override fun allowsHostSocketAccess(): Boolean = false
      override fun close() = Unit
      override fun accepts(path: Path): Boolean = router.invoke(path)
    }
  }
}

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
import elide.runtime.gvm.internals.LanguageVFS

/**
 * # Virtual File Systems: Language I/O
 *
 * Public access to create "language filesystem" instances, which overlay on top of embedded or host I/O file systems to
 * provide access to language built-ins. Language VFS is used by the Python, Ruby, and TypeScript layers to load core
 * standard libraries regardless of application I/O settings.
 *
 * &nbsp;
 *
 * ## Usage
 *
 * Language VFS accepts a [FileSystem] instance directly, and a [LanguageVFSInfo.router] function, which indicates
 * whether the implemented I/O should be used for a given path. The [LanguageVFSInfo.fsProvider] function is used to
 * spawn or otherwise acquire an instance of the I/O provider which should receive the routed calls.
 *
 * Adapter factories are supplied for regular Java NIO file system implementations. These interfaces wrap the proper
 * GraalVM interface and translate calls appropriately.
 *
 * &nbsp;
 *
 * ## Language VFS in Elide
 *
 * Elide uses the language VFS layer to mount sources for languages like Python and Ruby, which package their own
 * standard library code. When loading a module from the Python or Ruby standard libraries, calls are routed to language
 * VFS instances, which return the appropriate data.
 */
public object LanguageVFS {
  /**
   * ## Language VFS Info
   *
   * Describes a configured language-level virtual file system; this includes a [fsProvider] which can be used to obtain
   * the VFS implementation, and a [router] function which decides if a path is eligible to be handled by the VFS.
   */
  public interface LanguageVFSInfo {
    /**
     * ### Language VFS: Router
     *
     * Given a [Path], determine if this language VFS instance should handle I/O.
     */
    public val router: (Path) -> Boolean

    /**
     * ### Language VFS: Provider
     *
     * Obtain an instance of the language VFS which should handle an eligible path.
     */
    public val fsProvider: () -> FileSystem
  }

  /**
   * Delegate VFS
   *
   * Create a language VFS instance which delegates to a [LanguageVFSInfo] provider; the provider is expected to supply
   * an instance of [FileSystem].
   *
   * @param language Language ID which relates to this VFS provider
   * @param fs Provider function which returns a [LanguageVFSInfo] instance
   * @return A new language VFS instance
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

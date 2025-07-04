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
package elide.runtime.node.fs

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyInstantiable
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import elide.runtime.gvm.js.JsError
import elide.runtime.intrinsics.js.node.fs.Dirent

// Properties and methods present on `Dirent` instances.
private const val DIRENT_PROP_NAME = "name"
private const val DIRENT_PROP_ISBLOCKDEVICE = "isBlockDevice"
private const val DIRENT_PROP_ISCHARACTERDEVICE = "isCharacterDevice"
private const val DIRENT_PROP_ISDIRECTORY = "isDirectory"
private const val DIRENT_PROP_ISFIFO = "isFIFO"
private const val DIRENT_PROP_ISFILE = "isFile"
private const val DIRENT_PROP_ISSOCKET = "isSocket"
private const val DIRENT_PROP_ISSYMBOLICLINK = "isSymbolicLink"
private const val DIRENT_PROP_PARENTPATH = "parentPath"

// All methods and properties.
private val direntPropsAndMethods = arrayOf(
  DIRENT_PROP_NAME,
  DIRENT_PROP_ISBLOCKDEVICE,
  DIRENT_PROP_ISCHARACTERDEVICE,
  DIRENT_PROP_ISDIRECTORY,
  DIRENT_PROP_ISFIFO,
  DIRENT_PROP_ISFILE,
  DIRENT_PROP_ISSOCKET,
  DIRENT_PROP_ISSYMBOLICLINK,
  DIRENT_PROP_PARENTPATH,
)

/**
 * ## Node Filesystem: Directory Entry Implementations
 *
 * Implements a sealed hierarchy of types, each of which ultimately behaves as a [Dirent] (directory entry) instance.
 *
 * Such instances hold information about a given entry in a directory's listing; the instance holds things like the name
 * of the entry, and various booleans indicating what kind of entry it is.
 *
 * Directory entry instances are simple and behave like simple objects. They are not writable and do not expose any
 * methods. For a richer interaction with a directory, specifically, see [Directory].
 *
 * @see Directory directory listings
 */
public sealed interface DirectoryEntry : Dirent {
  /** Factory methods for obtaining [Dirent] instances. */
  public companion object Factory: ProxyInstantiable {
    /**
     * Create a directory entry backed by a [File] instance.
     *
     * @param file File to create an entry from.
     * @return [FileEntry] instance.
     */
    @JvmStatic public fun forFile(file: File): FileEntry = FileEntry.of(file)

    /**
     * Create a directory entry backed by a [Path] instance.
     *
     * @param path Path to create an entry from.
     * @return [PathEntry] instance.
     */
    @JvmStatic public fun forPath(path: Path): PathEntry = PathEntry.of(path)

    override fun newInstance(vararg arguments: Value?): Any? {
      if (arguments.isEmpty() || arguments.first()?.isNull != false) {
        throw JsError.typeError("Must provide argument to `Dirent` constructor (a file handle)")
      }
      TODO("Not yet implemented: Dirent constructor from guest")
    }
  }

  override fun getMemberKeys(): Array<String> = direntPropsAndMethods

  override fun getMember(key: String): Any? = when (key) {
    DIRENT_PROP_NAME -> name
    DIRENT_PROP_ISBLOCKDEVICE -> isBlockDevice
    DIRENT_PROP_ISCHARACTERDEVICE -> isCharacterDevice
    DIRENT_PROP_ISDIRECTORY -> isDirectory
    DIRENT_PROP_ISFIFO -> isFIFO
    DIRENT_PROP_ISFILE -> isFile
    DIRENT_PROP_ISSOCKET -> isSocket
    DIRENT_PROP_ISSYMBOLICLINK -> isSymbolicLink
    DIRENT_PROP_PARENTPATH -> parentPath
    else -> null
  }

  /**
   * Implements a [DirectoryEntry] for a [File] instance.
   */
  public class FileEntry private constructor (public val file: File) : DirectoryEntry {
    private val fileAsPath by lazy { file.toPath() }
    override val name: String get() = file.name
    override val parentPath: String get() = file.parent
    override val isFile: Boolean get() = file.isFile
    override val isDirectory: Boolean get() = file.isDirectory
    override val isSymbolicLink: Boolean get() = Files.isSymbolicLink(fileAsPath)

    // Note: Not supported from here on down.
    override val isSocket: Boolean get() = false
    override val isBlockDevice: Boolean get() = false
    override val isCharacterDevice: Boolean get() = false
    override val isFIFO: Boolean get() = false

    internal companion object {
      // Create a file entry from a file.
      @JvmStatic fun of(file: File): FileEntry = FileEntry(file)
    }
  }

  /**
   * Implements a [DirectoryEntry] for a [Path] instance.
   */
  public class PathEntry private constructor (public val path: Path) : DirectoryEntry {
    override val name: String get() = path.name
    override val parentPath: String get() = path.parent.toString()
    override val isFile: Boolean get() = Files.isRegularFile(path)
    override val isDirectory: Boolean get() = Files.isDirectory(path)
    override val isSymbolicLink: Boolean get() = Files.isSymbolicLink(path)

    // Note: Not supported from here on down.
    override val isSocket: Boolean get() = false
    override val isBlockDevice: Boolean get() = false
    override val isCharacterDevice: Boolean get() = false
    override val isFIFO: Boolean get() = false

    internal companion object {
      // Create a path entry from a path.
      @JvmStatic fun of(path: Path): PathEntry = PathEntry(path)
    }
  }
}

/**
 * Shorthand to create a [DirectoryEntry] from a [File] instance.
 *
 * @receiver File to create from.
 * @return Directory entry for the file.
 */
public fun File.asDirectoryEntry(): DirectoryEntry.FileEntry = DirectoryEntry.forFile(this)

/**
 * Shorthand to create a [DirectoryEntry] from a [Path] instance.
 *
 * @receiver Path to create from.
 * @return Directory entry for the file.
 */
public fun Path.asDirectoryEntry(): DirectoryEntry.PathEntry = DirectoryEntry.forPath(this)

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
@file:Suppress("TopLevelPropertyNaming")

package elide.runtime.node.path

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.io.File
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.PathAPI
import elide.runtime.intrinsics.js.node.path.Path
import elide.runtime.intrinsics.js.node.path.PathFactory
import elide.runtime.node.path.NodePaths.SYMBOL
import elide.runtime.node.path.PathStyle.POSIX
import elide.runtime.node.path.PathStyle.WIN32
import elide.vm.annotations.Polyglot
import java.nio.file.Path as JavaPath
import kotlinx.io.files.Path as KotlinPath
import elide.runtime.intrinsics.js.node.path.Path as PathIntrinsic

// Filename / extension separator.
private const val filenameSep = "."

// Path separators and delimiters.
private const val unixPathSep = "/"
private const val win32PathSep = "\\"
private const val unixPathDelim = ":"
private const val win32DriveDelim = ":"
private const val win32PathDelim = ";"
private const val win32MinRoot = 4

// Universal relative path references.
private const val currentDir = "."
private const val parentDir = ".."

/**
 * ## Path Style
 *
 * Describes supported path styles - [POSIX] and [WIN32].
 */
public enum class PathStyle {
  /**
   * POSIX-style path semantics.
   */
  POSIX,

  /**
   * Windows-style path semantics.
   */
  WIN32;

  internal fun isAbsolute(path: KotlinPath): Boolean = when (this) {
    POSIX -> path.toString().startsWith(unixPathSep)
    WIN32 -> path.toString().let { it.length > win32MinRoot && it[1] == win32DriveDelim[0] && it[2] == win32PathSep[0] }
  }
}

// All member keys.
private val pathMemberKeys = arrayOf(
  "basename",
  "delimiter",
  "dirname",
  "extname",
  "format",
  "isAbsolute",
  "join",
  "normalize",
  "parse",
  "posix",
  "relative",
  "resolve",
  "sep",
  "toNamespacedPath",
  "win32",
)

// Member keys of a path buffer object.
private val pathBufMemberKeys = arrayOf(
  "base",
  "dir",
  "ext",
  "isAbsolute",
  "join",
  "length",
  "name",
  "root",
  "toString",
)

// Private path utilities.
internal object PathUtils {
  @JvmStatic fun activePathStyle(osName: String? = System.getProperty("os.name")): PathStyle {
    return when (osName?.lowercase()?.trim() ?: "unknown") {
      "windows", "win32", "win" -> WIN32
      else -> POSIX
    }
  }
}

// Default path style.
private val defaultStyle = PathUtils.activePathStyle()

// Return the path separator to use for the given path style.
private fun sepFor(style: PathStyle): String = when (style) {
  WIN32 -> win32PathSep
  POSIX -> unixPathSep
}

/**
 * # Path
 *
 * Implements a Node-style [PathIntrinsic] with a Kotlin multi-platform [Path] object.
 */
public class PathBuf private constructor(
  override val style: PathStyle,
  private val path: KotlinPath,
) : PathIntrinsic, ProxyObject {
  override fun toKotlinPath(): KotlinPath = path
  override fun toJavaPath(): JavaPath = JavaPath.of(stringRepr)
  override fun copy(): PathIntrinsic = PathBuf(style, KotlinPath(stringRepr))
  override fun split(): Sequence<String> = stringRepr.split(sepFor(style)).asSequence()

  private val stringRepr: String by lazy {
    path.toString()
  }

  /** Factory for path objects. */
  public companion object : PathFactory {
    @JvmStatic override fun from(path: KotlinPath): PathIntrinsic = from(path.toString())
    @JvmStatic override fun from(path: PathIntrinsic): PathIntrinsic = path.copy()
    @JvmStatic override fun from(path: String, style: PathStyle?): Path =
      PathBuf((style ?: defaultStyle), KotlinPath(path))

    @JvmStatic override fun from(first: String, vararg rest: String): Path =
      PathBuf(defaultStyle, KotlinPath(first, *rest))

    @JvmStatic override fun from(path: java.nio.file.Path): Path =
      PathBuf(defaultStyle, KotlinPath(path.toString()))

    @JvmStatic override fun from(path: File): Path =
      PathBuf(defaultStyle, KotlinPath(path.toPath().toString()))
  }

  @get:Polyglot override val length: Int get() = stringRepr.length
  @Polyglot override fun compareTo(other: Path): Int = stringRepr.compareTo(other.toKotlinPath().toString())
  @Polyglot override fun get(index: Int): Char = stringRepr[index]
  @Polyglot override fun subSequence(startIndex: Int, endIndex: Int): CharSequence =
    stringRepr.subSequence(startIndex, endIndex)

  @get:Polyglot override val dir: String get() = split().toList().dropLast(1).joinToString(sepFor(style))
  @get:Polyglot override val base: String get() = split().last()
  @get:Polyglot override val name: String get() = split().last().substringBeforeLast(filenameSep)
  @get:Polyglot override val root: String
    get() = if (!isAbsolute) "" else when (style) {
      POSIX -> "/"
      WIN32 -> split().firstOrNull { it.contains(win32DriveDelim) }?.let { "$it$win32PathSep" } ?: ""
    }

  @get:Polyglot override val isAbsolute: Boolean
    get() = when (defaultStyle == style) {
      true -> path.isAbsolute
      else -> style.isAbsolute(path)
    }

  @get:Polyglot override val ext: String
    get() = split().last().let {
      when (it.contains(filenameSep)) {
        true -> it.substringAfterLast(filenameSep).ifBlank { null }?.let { ext -> "$filenameSep$ext" } ?: ""
        else -> ""
      }
    }

  @Polyglot override fun toString(): String = stringRepr
  @Polyglot override fun join(other: Iterable<String>): String =
    from(split().plus(other).joinToString(sepFor(style))).toString()

  @Polyglot override fun join(vararg paths: Path): String =
    from(listOf(this).plus(paths).flatMap { it.split() }.joinToString(sepFor(style))).toString()

  @Polyglot override fun join(first: String, vararg rest: String): String =
    from(listOf(this).plus(first).plus(rest).joinToString(sepFor(style))).toString()

  @Polyglot override fun equals(other: Any?): Boolean {
    return when {
      this === other -> true
      other is PathBuf -> stringRepr == other.stringRepr
      other is KotlinPath || other is JavaPath || other is Path -> stringRepr == other.toString()
      other is String -> other == stringRepr
      else -> false
    }
  }

  @Polyglot override fun hashCode(): Int = path.hashCode()

  override fun putMember(key: String?, value: Value?) { /* no-op */
  }

  override fun removeMember(key: String?): Boolean = false
  override fun getMemberKeys(): Array<String> = pathBufMemberKeys
  override fun hasMember(key: String?): Boolean = key != null && key in pathBufMemberKeys

  override fun getMember(key: String?): Any? = when (key) {
    "base" -> base
    "dir" -> dir
    "ext" -> ext
    "isAbsolute" -> isAbsolute
    "join" -> ProxyExecutable { args ->
      when (args.size) {
        0 -> ""
        else -> join(args[0].asString(), *args.drop(1).map { it.asString() }.toTypedArray())
      }
    }

    "length" -> length
    "name" -> name
    "root" -> root
    "toString" -> ProxyExecutable { toString() }
    else -> null
  }
}

// Installs the Node paths module into the intrinsic bindings.
@Intrinsic
internal class NodePathsModule : AbstractNodeBuiltinModule() {
  private val instance = NodePaths.create()

  val paths: PathAPI get() = instance

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[SYMBOL.asJsSymbol()] = NodePaths.create()
  }
}

/**
 * # Node API: `path`
 */
internal object NodePaths {
  /** Primordial symbol where the paths API implementation is installed. */
  internal const val SYMBOL: String = "node_path"

  /**
   * ## Base Paths
   *
   * Abstract implementation of basic path utilities; Windows and POSIX-style paths are supported, with each type of
   * path extended into their own implementations.
   *
   * @see WindowsPaths for Win32-style paths
   * @see PosixPaths for POSIX-style paths
   */
  abstract class BasePaths protected constructor(private val mode: PathStyle) : PathAPI, ProxyObject {
    @get:Polyglot override val posix: PathAPI get() = posixPaths
    @get:Polyglot override val win32: PathAPI get() = windowsPaths

    override fun join(sequence: Sequence<Path>): String =
      sepFor(mode).let { sep ->
        sequence.flatMap {
          it.toString().split(sep)
        }.filterIndexed { index, segment ->
          index == 0 || segment.isNotEmpty()  // filters out double slashing, except for root slash presence
        }.joinToString(sep)
      }

    override fun resolve(sequence: Sequence<Path>): String =
      PathBuf.from(sequence.map { it.toJavaPath() }.reduce(JavaPath::resolve)).toString()

    @Polyglot override fun basename(path: PathIntrinsic, ext: String?): String {
      return path.split().last().let {
        if (ext.isNullOrBlank()) it else {
          // trim existing `.` if applicable
          val trimmedExt = if (ext[0] == '.') ext.drop(1) else ext
          it.removeSuffix(".$trimmedExt")
        }
      }
    }

    @Polyglot override fun dirname(path: PathIntrinsic): String? = path.dir.ifBlank {
      if (!isAbsolute(path)) "." else null
    }.let {
      // if it's just the root dir, it should apparently be `null` to conform with node's behavior
      if (it == "/") "" else it
    }

    @Polyglot override fun isAbsolute(path: PathIntrinsic): Boolean = path.isAbsolute
    @Polyglot override fun join(first: Path, vararg rest: Path): String = first.join(*rest)
    @Polyglot override fun extname(path: PathIntrinsic): String = path.ext

    @Polyglot override fun relative(from: PathIntrinsic, to: PathIntrinsic): String =
      PathBuf.from(from.toJavaPath().relativize(to.toJavaPath())).toString()

    @Polyglot override fun resolve(first: PathIntrinsic, vararg rest: PathIntrinsic): String =
      PathBuf.from(listOf(first).plus(rest).map { it.toJavaPath() }.reduce(JavaPath::resolve)).toString()

    @Polyglot override fun normalize(path: PathIntrinsic): String = when {
      // on the native platform, we can leverage JVM's path normalization
      defaultStyle == path.style -> PathBuf.from(path.toJavaPath().normalize()).toString()

      // otherwise we have to do it ourselves
      else -> {
        val parts = path.split().toList()
        val stack = mutableListOf<String>()
        for (part in parts) {
          when (part) {
            currentDir -> {}
            parentDir -> {
              if (stack.isNotEmpty() && stack.last() != parentDir) stack.removeAt(stack.size - 1)
              else stack.add(part)
            }

            else -> stack.add(part)
          }
        }
        PathBuf.from(stack.joinToString(sepFor(path.style))).toString()
      }
    }

    @Polyglot override fun toNamespacedPath(path: PathIntrinsic): String {
      TODO("Not yet implemented")
    }

    @Polyglot override fun format(pathObject: Any): String {
      val sep = sepFor(defaultStyle)
      fun doFormat(base: String?, root: String?, dir: String?, name: String?, ext: String?): String {
        val filename = when {
          base != null -> base
          name != null -> "$name${ext ?: ""}"
          else -> ""
        }
        val dirPrefixed = when {
          // special case: filename is empty
          filename.isEmpty() -> "$dir$sep"
          dir != null -> "$dir$sep$filename"
          else -> filename
        }
        val path = when {
          root != null && filename.isNotEmpty() -> "$root$sep$dirPrefixed"
          else -> dirPrefixed
        }
        return PathBuf.from(path).toString()
      }

      when (pathObject) {
        is PathIntrinsic -> return pathObject.toString()

        is Map<*, *> -> {
          val dir = pathObject["dir"] as? String
          val root = pathObject["root"] as? String
          val base = pathObject["base"] as? String
          val ext = pathObject["ext"] as? String
          val name = pathObject["name"] as? String
          return doFormat(base, root, dir, name, ext)
        }

        is Value -> when {
          pathObject.isProxyObject || pathObject.hasMembers() -> {
            val dir = pathObject.getMember("dir")?.asString()
            val root = pathObject.getMember("root")?.asString()
            val base = pathObject.getMember("base")?.asString()
            val ext = pathObject.getMember("ext")?.asString()
            val name = pathObject.getMember("name")?.asString()
            return doFormat(base, root, dir, name, ext)
          }

          pathObject.hasHashEntries() -> {
            val dir = pathObject.getHashValue("dir")?.asString()
            val root = pathObject.getHashValue("root")?.asString()
            val base = pathObject.getHashValue("base")?.asString()
            val ext = pathObject.getHashValue("ext")?.asString()
            val name = pathObject.getHashValue("name")?.asString()
            return doFormat(base, root, dir, name, ext)
          }

          else -> error("Cannot format guest value as path, of type: '${pathObject::class.java.simpleName}'")
        }

        else -> error("Cannot format unrecognized object as path, of type: '${pathObject::class.java.simpleName}'")
      }
    }

    override fun parse(path: String, pathStyle: PathStyle?): Path = PathBuf.from(path, mode).also {
      if (pathStyle != null)
        require(pathStyle == mode) { "${mode.name} path utils cannot parse paths of type '$pathStyle'" }
    }

    override fun hasMember(key: String): Boolean = key in pathMemberKeys
    override fun getMemberKeys(): Array<String> = pathMemberKeys

    override fun getMember(key: String): Any? = when (key) {
      "sep" -> sep
      "delimiter" -> delimiter
      "posix" -> posix
      "win32" -> win32

      "join" -> ProxyExecutable { args ->
        when (args.size) {
          0 -> ""
          else -> join(args[0].asString(), *args.drop(1).map { it.asString() }.toTypedArray())
        }
      }

      "resolve" -> ProxyExecutable { args ->
        when (args.size) {
          0 -> ""
          else -> resolve(args[0].asString(), *args.drop(1).map { it.asString() }.toTypedArray())
        }
      }

      "basename" -> ProxyExecutable { args ->
        when (args.size) {
          0 -> ""
          1 -> basename(args[0].asString())
          else -> basename(args[0].asString(), args[1].asString())
        }
      }

      "dirname" -> ProxyExecutable { args ->
        when (args.size) {
          0 -> ""
          else -> dirname(args[0].asString())
        }
      }

      "isAbsolute" -> ProxyExecutable { args ->
        when (args.size) {
          0 -> false
          else -> isAbsolute(args[0].asString())
        }
      }

      "extname" -> ProxyExecutable { args ->
        when (args.size) {
          0 -> ""
          else -> extname(args[0].asString())
        }
      }

      "relative" -> ProxyExecutable { args ->
        when (args.size) {
          0 -> ""
          1 -> ""
          else -> relative(args[0].asString(), args[1].asString())
        }
      }

      "normalize" -> ProxyExecutable { args ->
        when (args.size) {
          0 -> ""
          else -> normalize(args[0].asString())
        }
      }

      "toNamespacedPath" -> ProxyExecutable { args ->
        when (args.size) {
          0 -> ""
          else -> toNamespacedPath(args[0].asString())
        }
      }

      "format" -> ProxyExecutable { args ->
        when (args.size) {
          0 -> ""
          else -> format(args[0])
        }
      }

      "parse" -> ProxyExecutable { args ->
        when (args.size) {
          0 -> ""
          else -> parse(args[0].asString(), args.getOrNull(1)?.asString()?.let(PathStyle::valueOf))
        }
      }

      else -> null
    }

    override fun putMember(key: String?, value: Value?) {
      throw UnsupportedOperationException("Path utilities are read-only")
    }

    override fun removeMember(key: String?): Boolean {
      throw UnsupportedOperationException("Path utilities are read-only")
    }
  }

  // Implementation of Windows-style paths.
  class WindowsPaths : BasePaths(WIN32) {
    @get:Polyglot override val sep: String get() = win32PathSep
    @get:Polyglot override val delimiter: String get() = win32PathDelim
  }

  // Implementation of POSIX-style paths.
  class PosixPaths : BasePaths(POSIX) {
    @get:Polyglot override val sep: String get() = unixPathSep
    @get:Polyglot override val delimiter: String get() = unixPathDelim
  }

  private val windowsPaths: WindowsPaths by lazy { WindowsPaths() }
  private val posixPaths: PosixPaths by lazy { PosixPaths() }

  /**
   * Create Paths Module
   *
   * Create a paths module with the provided [style] as the default path style, if specified; if no path style is
   * specified, the current operating system is queried to determine the path style to use.
   *
   * @param style Overriding path style to use (optional)
   * @return A new instance of the Node path utilities
   */
  @JvmStatic fun create(style: PathStyle = defaultStyle): PathAPI {
    return when (style) {
      WIN32 -> windowsPaths
      POSIX -> posixPaths
    }
  }
}

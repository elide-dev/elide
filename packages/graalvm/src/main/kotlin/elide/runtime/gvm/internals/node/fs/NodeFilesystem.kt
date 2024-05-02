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

package elide.runtime.gvm.internals.node.fs

import com.google.common.util.concurrent.MoreExecutors
import org.graalvm.polyglot.Value
import java.io.BufferedReader
import java.io.IOException
import java.nio.channels.Channels
import java.nio.channels.SeekableByteChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.asCoroutineDispatcher
import kotlin.coroutines.CoroutineContext
import elide.runtime.gvm.internals.GuestVFS
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.internals.node.NodeStdlib
import elide.runtime.gvm.vfs.HostVFS
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.node.FilesystemAPI
import elide.runtime.intrinsics.js.node.FilesystemPromiseAPI
import elide.runtime.intrinsics.js.node.buffer.Buffer
import elide.runtime.intrinsics.js.node.fs.ReadFileCallback
import elide.runtime.intrinsics.js.node.fs.ReadFileOptions
import elide.runtime.intrinsics.js.node.fs.StringOrBuffer
import elide.runtime.intrinsics.js.node.path.Path

// Installs the Node `fs` and `fs/promises` modules into the intrinsic bindings.
@Intrinsic internal class NodeFilesystemModule : AbstractNodeBuiltinModule() {
  /** VM filesystem manager. */
  private val filesystem: GuestVFS by lazy {
    HostVFS.acquireWritable()
  }

  private val executor: ExecutorService by lazy {
    MoreExecutors.newDirectExecutorService()
  }

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[NodeFilesystem.SYMBOL_STD.asJsSymbol()] = NodeFilesystem.createStd(executor, filesystem)
    bindings[NodeFilesystem.SYMBOL_PROMISES.asJsSymbol()] = NodeFilesystem.createPromises(executor, filesystem)
  }
}

// Implements the Node built-in filesystem modules.
internal object NodeFilesystem {
  internal const val SYMBOL_STD: String = "__Elide_node_fs__"
  internal const val SYMBOL_PROMISES: String = "__Elide_node_fs_promises__"

  private val directExecutor by lazy {
    MoreExecutors.newDirectExecutorService()
  }

  /** @return Host-only implementation of the `fs` module. */
  fun createStd(): FilesystemAPI = NodeFilesystemProxy(directExecutor, HostVFS.acquireWritable())

  /** @return Host-only implementation of the `fs/promises` module. */
  fun createPromises(): FilesystemPromiseAPI = NodeFilesystemPromiseProxy(directExecutor, HostVFS.acquireWritable())

  /** @return Instance of the `fs` module. */
  fun createStd(
    exec: ExecutorService,
    filesystem: GuestVFS
  ): FilesystemAPI = NodeFilesystemProxy(exec, filesystem)

  /** @return Instance of the `fs/promises` module. */
  fun createPromises(
    exec: ExecutorService,
    filesystem: GuestVFS
  ): FilesystemPromiseAPI = NodeFilesystemPromiseProxy(
    exec,
    filesystem
  )
}

// Implements common baseline functionality for the Node filesystem modules.
private abstract class FilesystemBaseline (
  protected val exec: ExecutorService,
  protected val fs: GuestVFS,
  protected val dispatcher: CoroutineDispatcher = exec.asCoroutineDispatcher(),
  protected val fsContext: CoroutineContext = dispatcher + CoroutineName("node-fs"),
) {
  protected fun resolvePath(operation: String, path: Value): Path = when {
    path.isString -> NodeStdlib.path.parse(path.asString())
    else -> error("Unknown type passed to `fs` $operation: ${path.asString()}")
  }

  protected fun readFileOptions(options: Value?): ReadFileOptions {
    return when {
      options == null -> ReadFileOptions.DEFAULTS
      options.isString -> TODO("Read file with string options as encoding")
      options.hasMembers() -> {
        val encoding = options.getMember("encoding")

        if (encoding == null) {
          ReadFileOptions.DEFAULTS
        } else ReadFileOptions(
          encoding = when {
            encoding.isNull -> null
            encoding.isString -> encoding.asString()
            else -> error("Unknown encoding type passed to `fs` readFile: ${encoding.asString()}")
          },
        )
      }

      else -> error("Unknown options passed to `fs` readFile: ${options.asString()}")
    }
  }

  protected fun resolveEncoding(encoding: StringOrBuffer?): Charset? {
    return if (encoding == null) null else when (encoding) {
      is String -> resolveEncodingString(encoding)
      is Buffer -> TODO("`Buffer` is not supported yet for `fs` operations")
      else -> error("Unknown encoding passed to `fs` readFile: $encoding")
    }
  }

  protected fun resolveEncodingString(encoding: String): Charset = when (encoding.trim().lowercase()) {
    "utf8", "utf-8" -> StandardCharsets.UTF_8
    "utf16", "utf-16" -> StandardCharsets.UTF_16
    "utf32", "utf-32" -> TODO("UTF-32 is not implemented yet")
    "ascii" -> StandardCharsets.US_ASCII
    "latin1", "binary" -> StandardCharsets.ISO_8859_1
    else -> error("Unknown encoding passed to `fs` readFile: $encoding")
  }

  protected fun checkFileForRead(path: java.nio.file.Path) {
    when {
      !fs.existsAny(path) -> error("ENOENT: no such file or directory, open '${path}'")
      !Files.isRegularFile(path) -> error("EISDIR: illegal operation on a directory, open '${path}'")
      !Files.isReadable(path) -> error("EACCES: permission denied, open '${path}'")
    }
  }

  protected inline fun <reified T> openAndRead(path: java.nio.file.Path, op: SeekableByteChannel.() -> T): T {
    checkFileForRead(path)

    val channel = try {
      Files.newByteChannel(path, StandardOpenOption.READ)
    } catch (e: IOException) {
      error("EACCES: failed to open stream to '${path}': ${e.message}")
    }
    return channel.use {
      op(it)
    }
  }

  protected fun SeekableByteChannel.readFileData(path: java.nio.file.Path, encoding: Charset?): StringOrBuffer {
    return try {
      when (encoding) {
        null -> Channels.newInputStream(this).use {
          TODO("Binary read not implemented: fs.readFileSync(Value, ...) (Node API)")
        }

        StandardCharsets.UTF_8 -> Channels.newReader(this, StandardCharsets.UTF_8).let { reader ->
          BufferedReader(reader).use { it.readText() }
        }

        else -> error("Unsupported encoding for `fs` readFileSync: ${encoding.name()}")
      }
    } catch (ioe: IOException) {
      error("EIO: failed to read file '${path}': ${ioe.message}")
    }
  }
}

// Implements the Node `fs` module.
private class NodeFilesystemProxy (exec: ExecutorService, fs: GuestVFS) : FilesystemAPI, FilesystemBaseline(exec, fs) {
  override fun readFile(path: Value, options: Value, callback: ReadFileCallback) {
    val resolved = resolvePath("readFile", path)
    val opts = readFileOptions(options)
    val nioPath = resolved.toJavaPath()
    val encoding = resolveEncoding(opts.encoding)

    openAndRead(nioPath) {
      val out = try {
        readFileData(nioPath, encoding)
      } catch (err: Throwable) {
        callback(TypeError.create(err), null)
        return
      }

      callback(null, out)
    }
  }

  override fun readFile(path: Value, callback: ReadFileCallback) {
    val resolved = resolvePath("readFile", path)
    val nioPath = resolved.toJavaPath()
    val encoding = resolveEncoding(ReadFileOptions.DEFAULTS.encoding)

    openAndRead(nioPath) {
      val out = try {
        readFileData(nioPath, encoding)
      } catch (err: Throwable) {
        callback(TypeError.create(err), null)
        return
      }

      callback(null, out)
    }
  }

  override fun readFile(path: Path, options: ReadFileOptions, callback: ReadFileCallback) {
    val encoding = resolveEncoding(options.encoding)
    val nioPath = path.toJavaPath()

    openAndRead(nioPath) {
      val out = try {
        readFileData(nioPath, encoding)
      } catch (err: Throwable) {
        callback(TypeError.create(err), null)
        return
      }

      callback(null, out)
    }
  }

  override fun readFileSync(path: Value, options: Value?): StringOrBuffer {
    val resolved = resolvePath("readFileSync", path)
    val opts = readFileOptions(options)
    val nioPath = resolved.toJavaPath()
    val encoding = resolveEncoding(opts.encoding)

    return openAndRead(nioPath) {
      readFileData(nioPath, encoding)
    }
  }

  override fun readFileSync(path: Path, options: ReadFileOptions): StringOrBuffer {
    val nioPath = path.toJavaPath()
    val encoding = resolveEncoding(options.encoding)

    return openAndRead(nioPath) {
      readFileData(nioPath, encoding)
    }
  }
}

// Implements the Node `fs/promises` module.
private class NodeFilesystemPromiseProxy (executor: ExecutorService, fs: GuestVFS)
  : FilesystemPromiseAPI, FilesystemBaseline(executor, fs) {
  override fun readFile(path: Value, options: Value?): JsPromise<StringOrBuffer> {
    val op: () -> StringOrBuffer = {
      val resolved = resolvePath("readFile", path)
      val opts = readFileOptions(options)
      val nioPath = resolved.toJavaPath()
      val encoding = resolveEncoding(opts.encoding)

      openAndRead(nioPath) {
        readFileData(nioPath, encoding)
      }
    }

    return JsPromise.of(exec.submit<StringOrBuffer>(op))
  }
}

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
@file:Suppress("TooGenericExceptionCaught")

package elide.runtime.gvm.internals.node.fs

import com.google.common.util.concurrent.MoreExecutors
import org.graalvm.polyglot.Value
import java.io.BufferedReader
import java.io.IOException
import java.io.OutputStream
import java.nio.channels.Channels
import java.nio.channels.SeekableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.AccessMode
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.EnumSet
import java.util.concurrent.ExecutorService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.asCoroutineDispatcher
import kotlin.coroutines.CoroutineContext
import elide.runtime.gvm.internals.GuestVFS
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.JsError
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.internals.node.NodeStdlib
import elide.runtime.gvm.vfs.HostVFS
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.node.FilesystemAPI
import elide.runtime.intrinsics.js.node.FilesystemPromiseAPI
import elide.runtime.intrinsics.js.node.NodeFilesystemAPI
import elide.runtime.intrinsics.js.node.NodeFilesystemPromiseAPI
import elide.runtime.intrinsics.js.node.buffer.Buffer
import elide.runtime.intrinsics.js.node.fs.*
import elide.runtime.intrinsics.js.node.path.Path
import elide.vm.annotations.Polyglot

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

// Filesystem constant values used by Node.
internal object FilesystemConstants {
  const val F_OK: Int = 0
  const val R_OK: Int = 4
  const val W_OK: Int = 2
  const val X_OK: Int = 1
}

// Context for a file write operation with managed state.
internal interface FileWriterContext {
  companion object {
    @JvmStatic fun of(path: java.nio.file.Path, encoding: Charset?, channel: WritableByteChannel): FileWriterContext {
      return object : FileWriterContext {
        override val path: java.nio.file.Path = path
        override val encoding: Charset? = encoding
        override val channel: WritableByteChannel = channel
      }
    }
  }

  /** Path to the file which is being written to. */
  val path: java.nio.file.Path

  /** Encoding assignment for this write operation, if available. */
  val encoding: Charset?

  /** Writable channel where bytes should be sent. */
  val channel: WritableByteChannel

  /** Write a string to the channel. */
  fun writeString(data: String) {
    val bytes = data.toByteArray(encoding ?: StandardCharsets.UTF_8)
    val buffer = java.nio.ByteBuffer.wrap(bytes)
    channel.write(buffer)
  }

  /** Write a buffer to the channel. */
  fun writeBuffer(data: Buffer) {
    TODO("Binary write not implemented: fs.writeFileSync(Value, ...) (Node API)")
  }

  /** Write using a [StringOrBuffer] value. */
  fun writeStringOrBuffer(data: StringOrBuffer) {
    when (data) {
      is String -> writeString(data)
      is Buffer -> writeBuffer(data)
      else -> error("Unknown type passed to `fs` writeFile: $data")
    }
  }
}

// Implements common baseline functionality for the Node filesystem modules.
internal abstract class FilesystemBase (
  protected val exec: ExecutorService,
  protected val fs: GuestVFS,
  protected val dispatcher: CoroutineDispatcher = exec.asCoroutineDispatcher(),
  protected val fsContext: CoroutineContext = dispatcher + CoroutineName("node-fs"),
) {
  protected fun resolvePath(operation: String, path: Value): Path = when {
    path.isString -> NodeStdlib.path.parse(path.asString())
    else -> error("Unknown type passed to `fs` $operation: ${path.asString()}")
  }

  private fun constantToAccessMode(constant: Int = FilesystemConstants.F_OK): AccessMode {
    return when (constant) {
      FilesystemConstants.F_OK,
      FilesystemConstants.R_OK -> AccessMode.READ
      FilesystemConstants.W_OK -> AccessMode.WRITE
      FilesystemConstants.X_OK -> AccessMode.EXECUTE
      else -> error("Unknown constant passed to `fs` access: $constant")
    }
  }

  protected fun translateMode(options: Value?): AccessMode {
    return when {
      options == null || options.isNull -> constantToAccessMode()
      options.isNumber -> constantToAccessMode(options.asInt())
      else -> error("Unknown type passed as access mode: ${options.asString()}")
    }
  }

  protected fun readFileOptions(options: Value?): ReadFileOptions {
    return when {
      options == null -> ReadFileOptions.DEFAULTS
      options.isString -> ReadFileOptions(
        encoding = resolveEncodingString(options.asString()),
      )

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

  protected fun writeFileOptions(options: Value?): WriteFileOptions {
    return when {
      options == null -> WriteFileOptions.DEFAULTS
      options.isString -> WriteFileOptions(
        encoding = resolveEncodingString(options.asString()),
      )

      options.hasMembers() -> {
        val encoding = options.getMember("encoding")

        if (encoding == null) {
          WriteFileOptions.DEFAULTS
        } else WriteFileOptions(
          encoding = when {
            encoding.isNull -> null
            encoding.isString -> encoding.asString()
            else -> error("Unknown encoding type passed to `fs` writeFile: ${encoding.asString()}")
          },
        )
      }

      else -> error("Unknown options passed to `fs` writeFile: ${options.asString()}")
    }
  }

  protected fun resolveEncoding(encoding: StringOrBuffer?): Charset? {
    return if (encoding == null) null else when (encoding) {
      is String -> resolveEncodingString(encoding)
      is Buffer -> TODO("`Buffer` is not supported yet for `fs` operations")
      else -> error("Unknown encoding passed to `fs` readFile: $encoding")
    }
  }

  private fun resolveEncodingString(encoding: String): Charset = when (encoding.trim().lowercase()) {
    "utf8", "utf-8" -> StandardCharsets.UTF_8
    "utf16", "utf-16" -> StandardCharsets.UTF_16
    "utf32", "utf-32" -> TODO("UTF-32 is not implemented yet")
    "ascii" -> StandardCharsets.US_ASCII
    "latin1", "binary" -> StandardCharsets.ISO_8859_1
    else -> error("Unknown encoding passed to `fs` readFile: $encoding")
  }

  private fun checkFileExists(path: java.nio.file.Path) {
    when {
      !fs.existsAny(path) -> error("ENOENT: no such file or directory, open '${path}'")
      !Files.isRegularFile(path) -> error("EISDIR: illegal operation on a directory, open '${path}'")
    }
  }

  protected fun checkFileForRead(path: java.nio.file.Path) {
    checkFileExists(path)
    when {
      !Files.isReadable(path) -> error("EACCES: permission denied, open '${path}'")
    }
  }

  protected fun checkFileForWrite(path: java.nio.file.Path, expectExists: Boolean = false) {
    if (expectExists) checkFileExists(path)
    when {
      !Files.isWritable(path) -> error("EACCES: permission denied, open '${path}'")
    }
  }

  protected inline fun <reified T> openAndRead(path: java.nio.file.Path, op: SeekableByteChannel.() -> T): T {
    checkFileForRead(path)

    return (try {
      Files.newByteChannel(path, StandardOpenOption.READ)
    } catch (e: IOException) {
      error("EACCES: failed to open stream to '${path}': ${e.message}")
    }).use {
      op(it)
    }
  }

  protected fun SeekableByteChannel.readFileData(path: java.nio.file.Path, encoding: Charset?): StringOrBuffer {
    return try {
      when (encoding) {
        null -> Channels.newInputStream(this).use {
          TODO("Binary read not implemented: fs.readFileSync(Value, ...) (Node API)")
        }

        StandardCharsets.US_ASCII,
        StandardCharsets.UTF_8,
        StandardCharsets.UTF_16,
        StandardCharsets.UTF_16LE,
        StandardCharsets.UTF_16BE -> Channels.newReader(this, encoding).let { reader ->
          BufferedReader(reader).use { it.readText() }
        }

        else -> error("Unsupported encoding for `fs` readFileSync: ${encoding.name()}")
      }
    } catch (ioe: IOException) {
      error("EIO: failed to read file '${path}': ${ioe.message}")
    }
  }

  protected inline fun <reified T> openForWrite(path: java.nio.file.Path, op: WritableByteChannel.() -> T): T {
    checkFileForWrite(path)
    return op(Files.newByteChannel(path, EnumSet.of(StandardOpenOption.CREATE)))
  }

  protected fun <R> WritableByteChannel.writeFileData(
    path: java.nio.file.Path,
    encoding: Charset?,
    op: FileWriterContext.() -> R,
  ): R = Channels.newOutputStream(this).use {
    FileWriterContext.of(path, encoding, this).op()
  }
}

// Implements the Node `fs` module.
internal class NodeFilesystemProxy (exec: ExecutorService, fs: GuestVFS) : NodeFilesystemAPI, FilesystemBase(exec, fs) {
  @Polyglot override fun access(path: Value, callback: Value) = access(
    resolvePath("access", path),
    AccessMode.READ
  ) {
    callback.executeVoid(it)
  }

  @Polyglot override fun access(path: Value, mode: Value, callback: Value) = access(
    resolvePath("access", path),
    translateMode(mode),
  ) {
    callback.executeVoid(it)
  }

  @Polyglot override fun access(path: Path, mode: AccessMode, callback: AccessCallback) = try {
    checkFileForRead(path.toJavaPath())
  } catch (err: Throwable) {
    callback(JsError.wrap(err))
  }

  @Polyglot override fun accessSync(path: Value) =
    accessSync(resolvePath("accessSync", path))

  @Polyglot override fun accessSync(path: Value, mode: Value) =
    accessSync(resolvePath("accessSync", path), translateMode(mode))

  @Polyglot override fun accessSync(path: Path, mode: AccessMode) {
    try {
      checkFileForRead(path.toJavaPath())
    } catch (err: Throwable) {
      throw JsError.wrap(err)
    }
  }

  @Polyglot override fun exists(path: Value, callback: Value) = exists(resolvePath("exists", path)) {
    callback.executeVoid(it)
  }

  @Polyglot override fun exists(path: Path, callback: (Boolean) -> Unit) {
    callback(Files.exists(path.toJavaPath()))
  }

  @Polyglot override fun existsSync(path: Value): Boolean = existsSync(resolvePath("existsSync", path))
  @Polyglot override fun existsSync(path: Path): Boolean = Files.exists(path.toJavaPath())

  @Polyglot override fun readFile(path: Value, options: Value, callback: ReadFileCallback) {
    val resolved = resolvePath("readFile", path)
    val opts = readFileOptions(options)
    val nioPath = resolved.toJavaPath()
    val encoding = resolveEncoding(opts.encoding)

    openAndRead(nioPath) {
      callback(null, try {
        readFileData(nioPath, encoding)
      } catch (err: Throwable) {
        callback(TypeError.create(err), null)
        return
      })
    }
  }

  @Polyglot override fun readFile(path: Value, callback: ReadFileCallback) {
    val resolved = resolvePath("readFile", path)
    val nioPath = resolved.toJavaPath()
    val encoding = resolveEncoding(ReadFileOptions.DEFAULTS.encoding)

    openAndRead(nioPath) {
      callback(null, try {
        readFileData(nioPath, encoding)
      } catch (err: Throwable) {
        callback(TypeError.create(err), null)
        return
      })
    }
  }

  @Polyglot override fun readFile(path: Path, options: ReadFileOptions, callback: ReadFileCallback) {
    val encoding = resolveEncoding(options.encoding)
    val nioPath = path.toJavaPath()

    openAndRead(nioPath) {
      callback(null, try {
        readFileData(nioPath, encoding)
      } catch (err: Throwable) {
        callback(TypeError.create(err), null)
        return
      })
    }
  }

  @Polyglot override fun readFileSync(path: Value, options: Value?): StringOrBuffer {
    val resolved = resolvePath("readFileSync", path)
    val opts = readFileOptions(options)
    val nioPath = resolved.toJavaPath()
    val encoding = resolveEncoding(opts.encoding)

    return openAndRead(nioPath) {
      readFileData(nioPath, encoding)
    }
  }

  @Polyglot override fun readFileSync(path: Path, options: ReadFileOptions): StringOrBuffer {
    val nioPath = path.toJavaPath()
    val encoding = resolveEncoding(options.encoding)

    return openAndRead(nioPath) {
      readFileData(nioPath, encoding)
    }
  }

  @Polyglot override fun writeFile(path: Value, data: StringOrBuffer, options: Value, callback: WriteFileCallback) {
    val resolved = resolvePath("writeFile", path)
    val opts = writeFileOptions(options)
    val nioPath = resolved.toJavaPath()
    val encoding = resolveEncoding(opts.encoding)

    try {
      openForWrite(nioPath) {
        writeFileData(nioPath, encoding) {
          writeStringOrBuffer(data)
        }
      }
    } catch (err: Throwable) {
      callback(TypeError.create(err))
    }
  }

  @Polyglot override fun writeFileSync(path: Value, data: StringOrBuffer, options: Value?) {
    val resolved = resolvePath("writeFile", path)
    val opts = writeFileOptions(options)
    val nioPath = resolved.toJavaPath()
    val encoding = resolveEncoding(opts.encoding)

    openForWrite(nioPath) {
      writeFileData(nioPath, encoding) {
        writeStringOrBuffer(data)
      }
    }
  }
}

// Implements the Node `fs/promises` module.
private class NodeFilesystemPromiseProxy (executor: ExecutorService, fs: GuestVFS)
  : NodeFilesystemPromiseAPI, FilesystemBase(executor, fs) {
  @Polyglot override fun readFile(path: Value, options: Value?): JsPromise<StringOrBuffer> {
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

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

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.io.BufferedReader
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.SeekableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.AccessMode
import java.nio.file.AccessMode.*
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import kotlinx.coroutines.CoroutineDispatcher
import elide.annotations.Eager
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.gvm.GuestExecutor
import elide.runtime.gvm.GuestExecutorProvider
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.JsError
import elide.runtime.gvm.internals.intrinsics.js.JsPromiseImpl.Companion.spawn
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.internals.node.NodeStdlib
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.err.AbstractJsException
import elide.runtime.intrinsics.js.err.Error
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.node.FilesystemAPI
import elide.runtime.intrinsics.js.node.FilesystemPromiseAPI
import elide.runtime.intrinsics.js.node.NodeFilesystemAPI
import elide.runtime.intrinsics.js.node.NodeFilesystemPromiseAPI
import elide.runtime.intrinsics.js.node.buffer.BufferInstance
import elide.runtime.intrinsics.js.node.fs.*
import elide.runtime.intrinsics.js.node.path.Path
import elide.runtime.plugins.vfs.VfsListener
import elide.runtime.vfs.GuestVFS
import elide.vm.annotations.Polyglot

// Default copy mode value.
private const val DEFAULT_COPY_MODE: Int = 0

// Listener bean for VFS init.
@Singleton @Eager public class VfsInitializerListener : VfsListener, Supplier<GuestVFS> {
  private val filesystem: AtomicReference<GuestVFS> = AtomicReference()

  override fun onVfsCreated(fileSystem: GuestVFS) {
    filesystem.set(fileSystem)
  }

  override fun get(): GuestVFS = filesystem.get().also { assert(it != null) { "VFS is not initialized" } }
}

// Installs the Node `fs` and `fs/promises` modules into the intrinsic bindings.
@Intrinsic @Factory internal class NodeFilesystemModule (
  private val vfs: VfsInitializerListener,
  private val executorProvider: GuestExecutorProvider,
) : AbstractNodeBuiltinModule() {
  private val std: FilesystemAPI by lazy {
    NodeFilesystem.createStd(executorProvider.executor(), vfs.get())
  }

  private val promises: FilesystemPromiseAPI by lazy {
    NodeFilesystem.createPromises(executorProvider.executor(), vfs.get())
  }

  @Singleton fun provideStd(): FilesystemAPI = std
  @Singleton fun providePromises(): FilesystemPromiseAPI = promises

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[NodeFilesystem.SYMBOL_STD.asJsSymbol()] = ProxyExecutable { provideStd() }
    bindings[NodeFilesystem.SYMBOL_PROMISES.asJsSymbol()] = ProxyExecutable { providePromises() }
  }
}

// Implements the Node built-in filesystem modules.
internal object NodeFilesystem {
  internal const val SYMBOL_STD: String = "node_fs"
  internal const val SYMBOL_PROMISES: String = "node_fs_promises"

  /** @return Instance of the `fs` module. */
  fun createStd(
    exec: GuestExecutor,
    filesystem: GuestVFS
  ): FilesystemAPI = NodeFilesystemProxy(exec, filesystem)

  /** @return Instance of the `fs/promises` module. */
  fun createPromises(
    exec: GuestExecutor,
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
  const val COPYFILE_EXCL: Int = 1
  const val COPYFILE_FICLONE: Int = 2
  const val COPYFILE_FICLONE_FORCE: Int = 4
}

// Context for a file write operation with managed state.
@OptIn(DelicateElideApi::class) internal interface FileWriterContext {
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

  /** Write an array of bytes to the channel. */
  fun writeBytes(data: ByteArray): Int = channel.write(ByteBuffer.wrap(data))

  /** Write a string to the channel. */
  fun writeString(data: String): Int = writeBytes(data.toByteArray(encoding ?: StandardCharsets.UTF_8))

  /** Write a buffer to the channel. */
  fun writeBuffer(data: BufferInstance) {
    TODO("Binary write not implemented: fs.writeFileSync(Value, ...) (Node API)")
  }

  /** Write using a [StringOrBuffer] value. */
  fun writeStringOrBuffer(data: StringOrBuffer) {
    when (data) {
      is ByteArray -> writeBytes(data)
      is String -> writeString(data)
      is BufferInstance -> writeBuffer(data)
      else -> JsError.error("Unknown type passed to `fs` writeFile: $data")
    }
  }
}

// Convert a file data input value to a raw byte array suitable for reading or writing.
@OptIn(DelicateElideApi::class)
internal fun guestToStringOrBuffer(value: Any?, encoding: Charset = StandardCharsets.UTF_8): ByteArray = when (value) {
  null -> throw JsError.valueError("Cannot read or write `null` as file data")
  is ByteArray -> value
  is String -> value.toByteArray(encoding)
  is BufferInstance -> TODO("Node buffers are not supported for filesystem operations yet")

  is Value -> when {
    value.isNull -> throw JsError.valueError("Cannot read or write `null` as file data")
    value.isString -> value.asString().toByteArray(encoding)
    value.hasMembers() -> value.getMember("toString").execute(value).asString().toByteArray(encoding)
    else -> throw JsError.valueError("Unknown guest type passed as file data: $value")
  }
  else -> throw JsError.valueError("Unknown host type passed as file data: $value")
}

// From Node constants, determine the set of `CopyOption` values to use.
private fun copyOpts(mode: Int = DEFAULT_COPY_MODE): Array<out CopyOption> = when (mode) {
  FilesystemConstants.COPYFILE_EXCL -> arrayOf()
  FilesystemConstants.COPYFILE_FICLONE -> arrayOf(StandardCopyOption.COPY_ATTRIBUTES)
  FilesystemConstants.COPYFILE_FICLONE_FORCE -> arrayOf(
    StandardCopyOption.COPY_ATTRIBUTES,
    StandardCopyOption.REPLACE_EXISTING,
  )

  else -> arrayOf(StandardCopyOption.REPLACE_EXISTING)
}

// From Node constants, determine the set of `CopyOption` values to use, as a guest value.
private fun copyOpts(mode: Value?): Int = when (mode) {
  null -> DEFAULT_COPY_MODE  // default mode value
  else -> when {
    mode.isNull -> DEFAULT_COPY_MODE
    mode.isNumber -> {
      assert(mode.fitsInInt()) { "Mode for `copyFile` must be no bigger than an integer" }
      mode.asInt()
    }

    else -> throw JsError.typeError("Unknown type passed to `fs` copyFile: ${mode.asString()}")
  }
}

// Perform a file copy operation.
private fun doCopyFile(src: Path, dest: Path, mode: Int = 0, callback: ((Throwable?) -> Unit)? = null): Throwable? {
  var exc: Throwable? = null
  try {
    Files.copy(src.toJavaPath(), dest.toJavaPath(), *copyOpts(mode))
  } catch (ioe: IOException) {
    exc = ioe
  }
  callback?.invoke(exc)
  return exc
}

// Perform a file copy operation.
private fun doCopyFileGuest(src: Path, dest: Path, mode: Int, callback: Value?): Throwable? {
  return doCopyFile(src, dest, mode) { err ->
    if (callback != null && callback.canExecute()) {
      if (err != null) callback.executeVoid(err)
      else callback.executeVoid()
    }
  }
}

// Implements common baseline functionality for the Node filesystem modules.
@OptIn(DelicateElideApi::class)
internal abstract class FilesystemBase (
    protected val exec: GuestExecutor,
    protected val fs: GuestVFS,
    protected val dispatcher: CoroutineDispatcher = exec.dispatcher,
) {
  protected fun resolvePath(operation: String, path: Value): Path = when {
    path.isString -> NodeStdlib.path.parse(path.asString())
    else -> JsError.error("Unknown type passed to `fs` $operation: ${path.asString()}")
  }

  // Obtain current context, if any, and then execute in the background; once execution completes, the context is again
  // entered, and execution continues.
  protected inline fun <reified T> withExec(noinline op: () -> T): JsPromise<T> = exec.spawn { op() }

  private fun constantToAccessMode(constant: Int = FilesystemConstants.F_OK): AccessMode {
    return when (constant) {
      FilesystemConstants.F_OK,
      FilesystemConstants.R_OK -> READ
      FilesystemConstants.W_OK -> WRITE
      FilesystemConstants.X_OK -> EXECUTE
      else -> JsError.error("Unknown constant passed to `fs` access: $constant")
    }
  }

  protected fun translateMode(options: Value?): AccessMode = when {
    options == null || options.isNull -> constantToAccessMode()
    options.isNumber -> constantToAccessMode(options.asInt())
    else -> JsError.error("Unknown type passed as access mode: ${options.asString()}")
  }

  protected fun readFileOptions(options: Value?): ReadFileOptions {
    return when {
      options == null || options.isNull -> ReadFileOptions.DEFAULTS
      options.isString -> ReadFileOptions(
        encoding = resolveEncodingString(options.asString()),
      )

      options.hasMembers() -> options.getMember("encoding").let { encoding ->
        if (encoding == null) ReadFileOptions.DEFAULTS else ReadFileOptions.fromGuest(options)
      }

      else -> JsError.error("Unknown options passed to `fs` readFile: $options")
    }
  }

  protected fun writeFileOptions(options: Value?): WriteFileOptions {
    return when {
      options == null || options.isNull -> WriteFileOptions.DEFAULTS
      options.isString -> WriteFileOptions(
        encoding = resolveEncodingString(options.asString()),
      )

      options.hasMembers() -> {
        val encoding = options.getMember("encoding")

        if (encoding == null) WriteFileOptions.DEFAULTS else WriteFileOptions(
          encoding = when {
            encoding.isNull -> null
            encoding.isString -> encoding.asString()
            else -> JsError.error("Unknown encoding type passed to `fs` writeFile: ${encoding.asString()}")
          },
        )
      }

      else -> JsError.error("Unknown options passed to `fs` writeFile: ${options.asString()}")
    }
  }

  protected fun resolveEncoding(encoding: StringOrBuffer?): Charset? {
    return if (encoding == null) null else resolveEncodingString(
      encoding as? String ?: JsError.error("Unknown encoding passed to `fs` readFile: $encoding")
    )
  }

  private fun resolveEncodingString(encoding: String): Charset = when (encoding.trim().lowercase()) {
    "utf8", "utf-8" -> Charsets.UTF_8
    "utf16", "utf-16" -> Charsets.UTF_16
    "utf32", "utf-32" -> Charsets.UTF_32
    "ascii" -> Charsets.US_ASCII
    "latin1", "binary" -> Charsets.ISO_8859_1
    else -> JsError.error("Unknown encoding passed to `fs` readFile: $encoding")
  }

  private fun checkFileExists(path: java.nio.file.Path) {
    when {
      !fs.existsAny(path) -> JsError.error("ENOENT: no such file or directory, open '${path}'")
      !Files.isRegularFile(path) -> JsError.error("EISDIR: illegal operation on a directory, open '${path}'")
    }
  }

  protected fun checkFile(path: java.nio.file.Path, mode: AccessMode = READ) {
    checkFileExists(path)
    when (mode) {
      READ -> checkFileForRead(path)
      WRITE -> checkFileForWrite(path)
      EXECUTE -> checkFileForExec(path)
    }
  }

  protected fun checkFileForRead(path: java.nio.file.Path) {
    checkFileExists(path)
    when {
      !Files.isReadable(path) -> throw JsError.of(
        "EPERM: operation not permitted, access '$path'",
        errno = -1,
        "code" to "EPERM",
        "syscall" to "access",
        "path" to path.toString(),
      )
    }
  }

  protected fun checkFileForWrite(path: java.nio.file.Path, expectExists: Boolean = false) {
    if (expectExists) checkFileExists(path)
    when {
      !Files.isWritable(path.parent) -> JsError.error("EACCES: permission denied for write, open '${path}'")
    }
  }

  protected fun checkFileForExec(path: java.nio.file.Path) {
    when {
      !Files.isExecutable(path.parent) -> JsError.error("EACCES: permission denied for exec, open '${path}'")
    }
  }

  // Open and read a file, closing it when finished.
  protected inline fun <reified T> openAndRead(path: java.nio.file.Path, op: SeekableByteChannel.() -> T): T =
    try {
      Files.newByteChannel(path, StandardOpenOption.READ).use { op(it) }
    } catch (ioe: NoSuchFileException) {
      JsError.error(
        "ENOENT: no such file or directory at '$path'",
        cause = ioe,
        errno = -2,
        "syscall" to "open",
      )
    } catch (ioe: IOException) {
      JsError.error("I/O error during file read", cause = ioe)
    }

  protected fun SeekableByteChannel.readFileData(path: java.nio.file.Path, encoding: Charset?): StringOrBuffer {
    return try {
      when (encoding) {
        null -> Channels.newInputStream(this).use {
          TODO("Binary read not implemented: fs.readFileSync(Value, ...) (Node API)")
        }

        Charsets.US_ASCII,
        Charsets.UTF_8,
        Charsets.UTF_16,
        Charsets.UTF_32 -> Channels.newReader(this, encoding).let { reader ->
          BufferedReader(reader).use { it.readText() }
        }

        else -> throw JsError.valueError("Unsupported encoding for `fs` readFileSync: ${encoding.name()}")
      }
    } catch (ioe: NoSuchFileException) {
      JsError.error("ENOENT: no such file or directory at '$path'", cause = ioe)
    } catch (ioe: IOException) {
      JsError.error("I/O error during buffered file read", cause = ioe)
    }
  }

  protected inline fun <reified T> checkForDirectoryCreate(path: java.nio.file.Path, op: () -> T): T {
    when {
      Files.exists(path) -> JsError.error("EEXIST: file already exists, mkdir '${path}'")
      !Files.isWritable(path.parent) -> JsError.error("EACCES: permission denied, mkdir '${path}'")
    }
    return op()
  }

  protected fun createDirectory(path: java.nio.file.Path, recursive: Boolean = false, op: ((Path) -> Unit)? = null) {
    try {
      if (recursive) Files.createDirectories(path) else Files.createDirectory(path)
    } catch (ioe: IOException) {
      throw JsError.wrap(ioe)
    }
    op?.invoke(Path.from(path))
  }

  protected inline fun <reified T> openForWrite(
    path: java.nio.file.Path,
    create: Boolean = false,
    exists: Boolean = false,
    op: WritableByteChannel.() -> T,
  ): T {
    checkFileForWrite(path)
    return op(Files.newByteChannel(path, when {
      create -> EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
      exists -> EnumSet.of(StandardOpenOption.WRITE)
      else -> EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    }))
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
internal class NodeFilesystemProxy (exec: GuestExecutor, fs: GuestVFS) : NodeFilesystemAPI, FilesystemBase(exec, fs) {
  @Polyglot override fun access(path: Value, callback: Value) {
    if (
      !callback.canExecute() &&
      !callback.isHostObject
    ) throw JsError.typeError("Callback to `fs.access` should be executable")

    access(
      resolvePath("access", path),
      READ
    ) {
      callback.executeVoid(it)
    }
  }

  @Polyglot override fun access(path: Value, mode: Value, callback: Value) {
    if (
      !callback.canExecute() &&
      !callback.isHostObject
    ) throw JsError.typeError("Callback to `fs.access` should be executable")

    access(
      resolvePath("access", path),
      translateMode(mode),
    ) {
      callback.executeVoid(it)
    }
  }

  @Polyglot override fun access(path: Path, mode: AccessMode, callback: AccessCallback) {
    withExec {
      try {
        val nioPath = path.toJavaPath()
        when (mode) {
          READ -> checkFileForRead(nioPath)
          WRITE -> checkFileForWrite(nioPath)
          EXECUTE -> checkFileForExec(nioPath)
        }
        null
      } catch (typeError: TypeError) {
        throw typeError  // immediately throw type errors, so they surface at callsites
      } catch (ioe: Error) {
        ioe
      }.let {
        callback.invoke(it)
      }
    }
  }

  @Polyglot override fun accessSync(path: Value) =
    accessSync(resolvePath("accessSync", path))

  @Polyglot override fun accessSync(path: Value, mode: Value) =
    accessSync(resolvePath("accessSync", path), translateMode(mode))

  @Polyglot override fun accessSync(path: Path, mode: AccessMode) {
    checkFileForRead(path.toJavaPath())
  }

  @Polyglot override fun exists(path: Value, callback: Value) = exists(resolvePath("exists", path)) {
    if (callback.canExecute()) callback.executeVoid(it)
  }

  @Polyglot override fun exists(path: Path, callback: (Boolean) -> Unit) {
    withExec {
      callback(Files.exists(path.toJavaPath()))
    }
  }

  @Polyglot override fun existsSync(path: Value): Boolean = existsSync(resolvePath("existsSync", path))
  @Polyglot override fun existsSync(path: Path): Boolean = Files.exists(path.toJavaPath())

  @Polyglot override fun readFile(path: Value, options: Value?, callback: ReadFileCallback?) {
    val opts = readFileOptions(options)
    val optionsIsCallback = callback == null && options != null && options.canExecute()
    val encoding = resolveEncoding(opts.encoding) ?: when {
      optionsIsCallback -> StandardCharsets.UTF_8
      else -> null
    }

    val cbk: (AbstractJsException?, StringOrBuffer?) -> Unit = { err, data ->
      if (callback != null) {
        if (err != null) callback(err, null)
        else callback(null, data)
      } else if (optionsIsCallback) {
        if (err != null) options!!.executeVoid(err, null)
        else options!!.executeVoid(null, data)
      } else {
        throw JsError.typeError("Callback for `readFile` must be a function")
      }
    }

    withExec {
      val nioPath = resolvePath("readFile", path).toJavaPath()
      openAndRead(nioPath) {
        cbk(null, readFileData(nioPath, encoding))
      }
    }
  }

  @Polyglot override fun readFile(path: Path, options: ReadFileOptions, callback: ReadFileCallback) {
    val encoding = resolveEncoding(options.encoding)

    withExec {
      val nioPath = path.toJavaPath()
      openAndRead(nioPath) {
        callback(null, try {
          readFileData(nioPath, encoding)
        } catch (err: Throwable) {
          callback(TypeError.create(err), null)
        })
      }
    }
  }

  @Polyglot override fun readFileSync(path: Value, options: Value?): StringOrBuffer {
    val resolved = resolvePath("readFileSync", path)
    val opts = readFileOptions(options)
    val nioPath = resolved.toJavaPath()
    val encoding = resolveEncoding(opts.encoding) ?: when {
      options == null || options.isNull -> StandardCharsets.UTF_8
      else -> null
    }
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

  @Polyglot override fun writeFile(path: Value, data: Value, callback: Value) =
    writeFile(resolvePath("writeFile", path), guestToStringOrBuffer(data), WriteFileOptions.DEFAULTS) {
      callback.executeVoid(it)
    }

  @Polyglot override fun writeFile(path: Value, data: Value, options: Value?, callback: Value) =
    writeFile(resolvePath("writeFile", path), guestToStringOrBuffer(data), writeFileOptions(options)) {
      callback.executeVoid(it)
    }

  override fun writeFile(path: Path, data: String, options: WriteFileOptions, callback: WriteFileCallback) =
    writeFile(
      path,
      data.toByteArray(resolveEncoding(options.encoding) ?: StandardCharsets.UTF_8),
      options,
      callback,
    )

  override fun writeFile(path: Path, data: ByteArray, options: WriteFileOptions, callback: WriteFileCallback) {
    val encoding = resolveEncoding(options.encoding)
    withExec {
      val nioPath = path.toJavaPath()
      openForWrite(nioPath) {
        writeFileData(nioPath, encoding) {
          writeStringOrBuffer(data)
        }
      }
    }
  }

  @Polyglot override fun writeFileSync(path: Value, data: Value) {
    return writeFileSync(
      resolvePath("writeFileSync", path),
      guestToStringOrBuffer(data),
      WriteFileOptions.DEFAULTS,
    )
  }

  @Polyglot override fun writeFileSync(path: Value, data: Value, options: Value?) {
    return writeFileSync(
      resolvePath("writeFileSync", path),
      guestToStringOrBuffer(data),
      writeFileOptions(options),
    )
  }

  override fun writeFileSync(path: Path, data: String, options: WriteFileOptions) {
    return writeFileSync(
      path,
      data.toByteArray(resolveEncoding(options.encoding) ?: StandardCharsets.UTF_8),
      options,
    )
  }

  override fun writeFileSync(path: Path, data: ByteArray, options: WriteFileOptions) {
    val nioPath = path.toJavaPath()
    val encoding = resolveEncoding(options.encoding)

    openForWrite(nioPath) {
      writeFileData(nioPath, encoding) {
        writeStringOrBuffer(data)
      }
    }
  }

  @Polyglot override fun mkdir(path: Value, callback: Value?) = mkdir(path, null, callback)

  @Polyglot override fun mkdir(path: Value, options: Value?, callback: Value?) {
    assert(callback != null && !callback.isNull) {
      "Callback for `mkdir` cannot be `null` or missing"
    }
    withExec {
      mkdir(
        resolvePath("mkdir", path),
        MkdirOptions.fromGuest(options),
      ) {
        callback?.executeVoid(it)
      }
    }
  }

  @Polyglot override fun mkdir(path: Path, options: MkdirOptions, callback: MkdirCallback) {
    withExec {
      val nioPath = path.toJavaPath()
      checkForDirectoryCreate(nioPath) {
        createDirectory(nioPath, options.recursive) {
          callback.invoke(null)
        }
      }
    }
  }

  @Polyglot override fun mkdirSync(path: Value): String =
    mkdirSync(resolvePath("mkdirSync", path), MkdirOptions.DEFAULTS)

  @Polyglot override fun mkdirSync(path: Value, options: Value?): String =
    mkdirSync(resolvePath("mkdirSync", path), MkdirOptions.fromGuest(options))

  @Polyglot override fun mkdirSync(path: Path, options: MkdirOptions): String {
    return path.toJavaPath().let { nioPath ->
      checkForDirectoryCreate(nioPath) {
        createDirectory(nioPath, options.recursive)
      }
      nioPath
    }.toString()
  }

  override fun copyFile(src: Path, dest: Path, mode: Int, callback: ((Throwable?) -> Unit)?) {
    doCopyFile(src, dest, mode, callback)
  }

  @Polyglot override fun copyFile(src: Value, dest: Value, callback: Value) {
    copyFile(src, dest, mode = null, callback)
  }

  @Polyglot override fun copyFile(src: Value, dest: Value, mode: Value?, callback: Value) {
    val modeValue = if (mode?.isNumber != true) 0 else {
      assert(mode.fitsInInt()) { "Mode for `copyFile` must be no bigger than an integer" }
      mode.asInt()
    }
    val cbk = when {
      // if `callback` is `null`-like, and `mode` is executable, the `mode` should default, and the callback should be
      // used via `mode`.
      callback.isNull && mode != null && mode.canExecute() -> mode

      // otherwise, use the provided callback
      else -> callback
    }

    withExec {
      val srcPath = resolvePath("copyFile", src)
      val destPath = resolvePath("copyFile", dest)

      doCopyFileGuest(srcPath, destPath, modeValue, cbk)
    }
  }

  override fun copyFileSync(src: Path, dest: Path, mode: Int) {
    when (val err = doCopyFile(src, dest, mode)) {
      null -> {}
      else -> throw err
    }
  }

  @Polyglot override fun copyFileSync(src: Value, dest: Value) {
    copyFileSync(src, dest, mode = null)
  }

  @Polyglot override fun copyFileSync(src: Value, dest: Value, mode: Value?) {
    doCopyFile(
      resolvePath("copyFileSync", src),
      resolvePath("copyFileSync", dest),
      copyOpts(mode),
    ) {
      if (it != null) throw it
    }
  }
}

// Implements the Node `fs/promises` module.
private class NodeFilesystemPromiseProxy (executor: GuestExecutor, fs: GuestVFS)
  : NodeFilesystemPromiseAPI, FilesystemBase(executor, fs) {
  @Polyglot override fun readFile(path: Value): JsPromise<StringOrBuffer> =
    readFile(resolvePath("readFile", path), ReadFileOptions.DEFAULTS)

  @Polyglot override fun readFile(path: Value, options: Value?): JsPromise<StringOrBuffer> =
    readFile(resolvePath("readFile", path), readFileOptions(options))

  override fun readFile(path: Path, options: ReadFileOptions?) = withExec<StringOrBuffer> {
    val encoding = resolveEncoding(options?.encoding)
    path.toJavaPath().let { nioPath ->
      openAndRead(nioPath) {
        readFileData(nioPath, encoding)
      }
    }
  }

  @Polyglot override fun access(path: Value): JsPromise<Unit> =
    access(resolvePath("access", path), READ)

  @Polyglot override fun access(path: Value, mode: Value?): JsPromise<Unit> =
    access(resolvePath("access", path), translateMode(mode))

  override fun access(path: Path, mode: AccessMode?): JsPromise<Unit> = withExec<Unit> {
    checkFile(path.toJavaPath(), mode ?: READ)
  }

  @Polyglot override fun writeFile(path: Value, data: Value): JsPromise<Unit> =
    writeFile(resolvePath("writeFile", path), data, null)

  @Polyglot override fun writeFile(path: Value, data: Value, options: Value?): JsPromise<Unit> =
    writeFile(resolvePath("writeFile", path), data, writeFileOptions(options))

  override fun writeFile(path: Path, data: StringOrBuffer, options: WriteFileOptions?): JsPromise<Unit> {
    return withExec<Unit> {
      val encoding = resolveEncoding(options?.encoding)
      val nioPath = path.toJavaPath()

      openForWrite(nioPath) {
        writeFileData(nioPath, encoding) {
          writeStringOrBuffer(data)
        }
      }
    }
  }

  @Polyglot override fun mkdir(path: Value): JsPromise<StringOrBuffer> =
    mkdir(resolvePath("mkdir", path), null)

  @Polyglot override fun mkdir(path: Value, options: Value?): JsPromise<StringOrBuffer> =
    mkdir(resolvePath("mkdir", path), MkdirOptions.fromGuest(options))

  override fun copyFile(src: Path, dest: Path, mode: Int?): JsPromise<Value> = withExec {
    doCopyFile(src, dest, mode = mode ?: DEFAULT_COPY_MODE) {
      if (it != null) throw it
    }
    Value.asValue(null)
  }

  @Polyglot override fun copyFile(src: Value, dest: Value): JsPromise<Value> =
    copyFile(resolvePath("copyFile", src), resolvePath("copyFile", dest))

  @Polyglot override fun copyFile(src: Value, dest: Value, mode: Int): JsPromise<Value> {
    return withExec<Value> {
      val srcPath = resolvePath("copyFile", src)
      val destPath = resolvePath("copyFile", dest)
      when (val err = doCopyFile(srcPath, destPath)) {
        null -> Value.asValue(null)
        else -> throw err
      }
    }
  }

  override fun mkdir(path: Path, options: MkdirOptions?): JsPromise<StringOrBuffer> = withExec<StringOrBuffer> {
    path.toJavaPath().let { nioPath ->
      checkForDirectoryCreate(nioPath) {
        createDirectory(nioPath, options?.recursive ?: false)
      }
      nioPath
    }.toString()
  }
}

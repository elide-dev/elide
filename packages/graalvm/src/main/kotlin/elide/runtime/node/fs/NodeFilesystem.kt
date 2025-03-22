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
@file:Suppress("TooGenericExceptionCaught")

package elide.runtime.node.fs

import io.micronaut.core.annotation.ReflectiveAccess
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.io.ByteSequence
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.io.BufferedReader
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.SeekableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.nio.file.AccessMode.*
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier
import kotlinx.coroutines.CoroutineDispatcher
import elide.annotations.Eager
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.exec.GuestExecutor
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.internals.intrinsics.js.JsPromiseImpl.Companion.spawn
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.err.AbstractJsException
import elide.runtime.intrinsics.js.err.Error
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.node.*
import elide.runtime.intrinsics.js.node.buffer.BufferInstance
import elide.runtime.intrinsics.js.node.fs.*
import elide.runtime.intrinsics.js.node.path.Path
import elide.runtime.lang.javascript.NodeModuleName
import elide.runtime.node.path.NodePathsModule
import elide.runtime.plugins.vfs.VfsListener
import elide.runtime.vfs.GuestVFS
import elide.vm.annotations.Polyglot

// Default copy mode value.
private const val DEFAULT_COPY_MODE: Int = 0

// Whether to enable synthesized modules.
private const val ENABLE_SYNTHESIZED: Boolean = true

// Names of `fs` methods, properties, and constants.

internal const val FS_F_ACCESS = "access"
private const val FS_F_ACCESS_SYNC = "accessSync"
internal const val FS_F_EXISTS = "exists"
private const val FS_F_EXISTS_SYNC = "existsSync"
internal const val FS_F_READ_FILE = "readFile"
private const val FS_F_READ_FILE_SYNC = "readFileSync"
internal const val FS_F_WRITE_FILE = "writeFile"
private const val FS_F_WRITE_FILE_SYNC = "writeFileSync"
internal const val FS_F_MKDIR = "mkdir"
private const val FS_F_MKDIR_SYNC = "mkdirSync"
internal const val FS_F_COPY_FILE = "copyFile"
private const val FS_F_COPY_FILE_SYNC = "copyFileSync"
private const val FS_P_CONSTANTS = "constants"
private const val FS_ENCODING_UTF8 = "utf8"
private const val FS_ENCODING_UTF8_ALT = "utf-8"
private const val FS_ENCODING_UTF16 = "utf16"
private const val FS_ENCODING_UTF16_ALT = "utf-16"
private const val FS_ENCODING_UTF32 = "utf32"
private const val FS_ENCODING_UTF32_ALT = "utf-32"
private const val FS_ENCODING_ASCII = "ascii"
private const val FS_ENCODING_US_ASCII = "us-ascii"
private const val FS_ENCODING_US_ASCII_ALT = "usascii"
private const val FS_ENCODING_LATIN1 = "latin1"
private const val FS_ENCODING_BINARY = "binary"
private const val FS_C_F_OK = "F_OK"
private const val FS_C_R_OK = "R_OK"
private const val FS_C_W_OK = "W_OK"
private const val FS_C_X_OK = "X_OK"
private const val FS_C_COPYFILE_EXCL = "COPYFILE_EXCL"
private const val FS_C_COPYFILE_FICLONE = "COPYFILE_FICLONE"
private const val FS_C_COPYFILE_FICLONE_FORCE = "COPYFILE_FICLONE_FORCE"

// Listener bean for VFS init.
@Singleton @Eager public class VfsInitializerListener : VfsListener, Supplier<GuestVFS> {
  private val filesystem: AtomicReference<GuestVFS> = AtomicReference()

  override fun onVfsCreated(fileSystem: GuestVFS) {
    filesystem.set(fileSystem)
  }

  override fun get(): GuestVFS = filesystem.get().also { assert(it != null) { "VFS is not initialized" } }
}

// Installs the Node `fs` and `fs/promises` modules into the intrinsic bindings.
@Intrinsic
@Factory @Singleton internal class NodeFilesystemModule(
  private val pathModule: NodePathsModule,
  private val vfs: VfsInitializerListener,
  private val executorProvider: GuestExecutorProvider,
) : AbstractNodeBuiltinModule() {
  private val std: FilesystemAPI by lazy {
    NodeFilesystem.createStd(executorProvider.executor(), pathModule.paths, vfs.get())
  }

  private val promises: FilesystemPromiseAPI by lazy {
    NodeFilesystem.createPromises(executorProvider.executor(), pathModule.paths, vfs.get())
  }

  @Singleton fun provideStd(): FilesystemAPI = std
  @Singleton fun providePromises(): FilesystemPromiseAPI = promises

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[NodeFilesystem.SYMBOL_STD.asJsSymbol()] = ProxyExecutable { provideStd() }
    bindings[NodeFilesystem.SYMBOL_PROMISES.asJsSymbol()] = ProxyExecutable { providePromises() }
  }

  init {
    if (ENABLE_SYNTHESIZED) {
      ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.FS)) { std }
      ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.FS_PROMISES)) { promises }
    }
  }
}

/**
 * Resolve a charset from a string encoding value.
 *
 * @param encoding The encoding string to resolve.
 * @return The resolved charset.
 */
internal fun resolveEncodingString(encoding: String): Charset = when (encoding.trim().lowercase()) {
  FS_ENCODING_UTF8, FS_ENCODING_UTF8_ALT -> Charsets.UTF_8
  FS_ENCODING_UTF16, FS_ENCODING_UTF16_ALT -> Charsets.UTF_16
  FS_ENCODING_UTF32, FS_ENCODING_UTF32_ALT -> Charsets.UTF_32
  FS_ENCODING_ASCII, FS_ENCODING_US_ASCII, FS_ENCODING_US_ASCII_ALT -> Charsets.US_ASCII
  FS_ENCODING_LATIN1, FS_ENCODING_BINARY -> Charsets.ISO_8859_1
  else -> throw JsError.valueError("Unknown encoding passed to `fs` readFile: $encoding")
}

// Implements the Node built-in filesystem modules.
internal object NodeFilesystem {
  internal const val SYMBOL_STD: String = "node_${NodeModuleName.FS}"
  internal const val SYMBOL_PROMISES: String = "node_fs_promises"

  /** @return Instance of the `fs` module. */
  fun createStd(
    exec: GuestExecutor,
    path: PathAPI,
    filesystem: GuestVFS
  ): FilesystemAPI = NodeFilesystemProxy(
    path,
    exec,
    filesystem,
  )

  /** @return Instance of the `fs/promises` module. */
  fun createPromises(
    exec: GuestExecutor,
    path: PathAPI,
    filesystem: GuestVFS
  ): FilesystemPromiseAPI = NodeFilesystemPromiseProxy(
    path,
    exec,
    filesystem,
  )
}

// Filesystem constant values used by Node.
internal object FilesystemConstants : ReadOnlyProxyObject {
  const val F_OK: Int = 0
  const val R_OK: Int = 4
  const val W_OK: Int = 2
  const val X_OK: Int = 1
  const val COPYFILE_EXCL: Int = 1
  const val COPYFILE_FICLONE: Int = 2
  const val COPYFILE_FICLONE_FORCE: Int = 4

  override fun getMemberKeys(): Array<String> = arrayOf(
    FS_C_F_OK,
    FS_C_R_OK,
    FS_C_W_OK,
    FS_C_X_OK,
    FS_C_COPYFILE_EXCL,
    FS_C_COPYFILE_FICLONE,
    FS_C_COPYFILE_FICLONE_FORCE,
  )

  override fun getMember(key: String?): Any? = when (key) {
    FS_C_F_OK -> F_OK
    FS_C_R_OK -> R_OK
    FS_C_W_OK -> W_OK
    FS_C_X_OK -> X_OK
    FS_C_COPYFILE_EXCL -> COPYFILE_EXCL
    FS_C_COPYFILE_FICLONE -> COPYFILE_FICLONE
    FS_C_COPYFILE_FICLONE_FORCE -> COPYFILE_FICLONE_FORCE
    else -> null
  }
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
  is ByteBuffer -> value.array()
  is String -> value.toByteArray(encoding)
  is BufferInstance -> TODO("Node buffers are not supported for filesystem operations yet")

  is Value -> when {
    value.isNull -> throw JsError.valueError("Cannot read or write `null` as file data")
    value.isString -> value.asString().toByteArray(encoding)
    value.hasBufferElements() -> value.`as`(ByteSequence::class.java).toByteArray()
    value.isHostObject -> TODO("Host object buffers are not supported yet")
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
@Suppress("SpreadOperator")
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
internal abstract class FilesystemBase(
  protected val path: PathAPI,
  protected val exec: GuestExecutor,
  protected val fs: GuestVFS,
  protected val dispatcher: CoroutineDispatcher = exec.dispatcher,
) {
  protected fun resolvePath(operation: String, path: Value): Path = when {
    path.isString -> this.path.parse(path.asString())
    else -> JsError.error("Unknown type passed to `fs` $operation: ${path.asString()}")
  }

  // Obtain current context, if any, and then execute in the background; once execution completes, the context is again
  // entered, and execution continues.
  protected inline fun <reified T> withExec(noinline op: () -> T): JsPromise<T> = try {
    exec.spawn<T> { op() }
  } catch (err: Throwable) {
    JsPromise.rejected(err)
  }

  private fun constantToAccessMode(constant: Int = FilesystemConstants.F_OK): AccessMode {
    return when (constant) {
      FilesystemConstants.W_OK -> WRITE
      FilesystemConstants.X_OK -> EXECUTE
      else -> READ
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
      encoding as? String ?: JsError.error("Unknown encoding passed to `fs` readFile: $encoding"),
    )
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
        "syscall" to FS_F_ACCESS,
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
    return op(
      Files.newByteChannel(
        path,
        when {
          create -> EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
          exists -> EnumSet.of(StandardOpenOption.WRITE)
          else -> EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        },
      ),
    )
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
@ReflectiveAccess internal class NodeFilesystemProxy(path: PathAPI, exec: GuestExecutor, fs: GuestVFS) :
  NodeFilesystemAPI,
  ReadOnlyProxyObject,
  FilesystemBase(path, exec, fs) {
  override fun getMemberKeys(): Array<String> = arrayOf(
    FS_F_ACCESS,
    FS_F_ACCESS_SYNC,
    FS_F_EXISTS,
    FS_F_EXISTS_SYNC,
    FS_F_READ_FILE,
    FS_F_READ_FILE_SYNC,
    FS_F_WRITE_FILE,
    FS_F_WRITE_FILE_SYNC,
    FS_F_MKDIR,
    FS_F_MKDIR_SYNC,
    FS_F_COPY_FILE,
    FS_F_COPY_FILE_SYNC,
    FS_P_CONSTANTS,
  )

  override fun getMember(key: String?): Any? = when (key) {
    FS_F_ACCESS -> ProxyExecutable {
      when (it.size) {
        2 -> access(it.first(), it[1])
        3 -> access(it.first(), it[1], it[2])
        else -> throw JsError.typeError("Invalid number of arguments to `fs.access`")
      }
    }

    FS_F_ACCESS_SYNC -> ProxyExecutable {
      when (it.size) {
        1 -> accessSync(it.first())
        2 -> accessSync(it.first(), it[1])
        else -> throw JsError.typeError("Invalid number of arguments to `fs.access`")
      }
    }

    FS_F_EXISTS -> ProxyExecutable {
      when (it.size) {
        2 -> exists(it.first(), it[1])
        else -> throw JsError.typeError("Invalid number of arguments to `fs.exists`")
      }
    }

    FS_F_EXISTS_SYNC -> ProxyExecutable {
      when (it.size) {
        1 -> existsSync(it.first())
        else -> throw JsError.typeError("Invalid number of arguments to `fs.existsSync`")
      }
    }

    FS_F_READ_FILE -> ProxyExecutable {
      when (it.size) {
        2 -> readFile(it.first(), it[1])
        3 -> readFile(it.first(), it[1]) { err, data ->
          val cbk = it.getOrNull(2)
          if (cbk != null && cbk.canExecute()) cbk.executeVoid(err, data)
        }
        else -> throw JsError.typeError("Invalid number of arguments to `fs.readFile` (got ${it.size})")
      }
    }

    FS_F_READ_FILE_SYNC -> ProxyExecutable {
      when (it.size) {
        1 -> readFileSync(it.first())
        2 -> readFileSync(it.first(), it[1])
        else -> throw JsError.typeError("Invalid number of arguments to `fs.readFileSync`")
      }
    }

    FS_F_WRITE_FILE -> ProxyExecutable {
      when (it.size) {
        3 -> writeFile(it.first(), it[1], it[2])
        4 -> writeFile(it.first(), it[1], it[2], it[3])
        else -> throw JsError.typeError("Invalid number of arguments to `fs.writeFile`")
      }
    }

    FS_F_WRITE_FILE_SYNC -> ProxyExecutable {
      when (it.size) {
        2 -> writeFileSync(it.first(), it[1])
        3 -> writeFileSync(it.first(), it[1], it[2])
        else -> throw JsError.typeError("Invalid number of arguments to `fs.writeFileSync`")
      }
    }

    FS_F_MKDIR -> ProxyExecutable {
      when (it.size) {
        2 -> mkdir(it.first(), it[1])
        3 -> mkdir(it.first(), it[1], it[2])
        else -> throw JsError.typeError("Invalid number of arguments to `fs.mkdir`")
      }
    }

    FS_F_MKDIR_SYNC -> ProxyExecutable {
      when (it.size) {
        1 -> mkdirSync(it.first())
        2 -> mkdirSync(it.first(), it[1])
        else -> throw JsError.typeError("Invalid number of arguments to `fs.mkdirSync`")
      }
    }

    FS_F_COPY_FILE -> ProxyExecutable {
      when (it.size) {
        3 -> copyFile(it.first(), it[1], it[2])
        4 -> copyFile(it.first(), it[1], it[2], it[3])
        else -> throw JsError.typeError("Invalid number of arguments to `fs.copyFile` (got ${it.size})")
      }
    }

    FS_F_COPY_FILE_SYNC -> ProxyExecutable {
      when (it.size) {
        2 -> copyFileSync(it.first(), it[1])
        3 -> copyFileSync(it.first(), it[1], it[2])
        else -> throw JsError.typeError("Invalid number of arguments to `fs.copyFileSync`")
      }
    }

    FS_P_CONSTANTS -> FilesystemConstants

    else -> null
  }

  @Polyglot override fun access(path: Value, callback: Value) {
    if (
      !callback.canExecute() &&
      !callback.isHostObject
    ) throw JsError.typeError("Callback to `fs.access` should be executable")

    access(
      resolvePath("access", path),
      READ,
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
        callback(
          null,
          try {
            readFileData(nioPath, encoding)
          } catch (err: Throwable) {
            callback(TypeError.create(err), null)
          },
        )
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
@ReflectiveAccess private class NodeFilesystemPromiseProxy(path: PathAPI, executor: GuestExecutor, fs: GuestVFS) :
  NodeFilesystemPromiseAPI,
  ReadOnlyProxyObject,
  FilesystemBase(path, executor, fs) {
  override fun getMemberKeys(): Array<String> = arrayOf(
    FS_F_ACCESS,
    FS_F_READ_FILE,
    FS_F_WRITE_FILE,
    FS_F_MKDIR,
    FS_F_COPY_FILE,
    FS_P_CONSTANTS,
  )

  override fun getMember(key: String?): Any? = when (key) {
    FS_F_ACCESS -> ProxyExecutable {
      when (it.size) {
        1 -> access(it.first())
        2 -> access(it.first(), it[1])
        else -> throw JsError.typeError("Invalid number of arguments to `fs.access`")
      }
    }

    FS_F_READ_FILE -> ProxyExecutable {
      when (it.size) {
        1 -> readFile(it.first())
        2 -> readFile(it.first(), it[1])
        else -> throw JsError.typeError("Invalid number of arguments to `fs.readFile`")
      }
    }

    FS_F_WRITE_FILE -> ProxyExecutable {
      when (it.size) {
        2 -> writeFile(it.first(), it[1])
        3 -> writeFile(it.first(), it[1], it[2])
        else -> throw JsError.typeError("Invalid number of arguments to `fs.writeFile`")
      }
    }

    FS_F_MKDIR -> ProxyExecutable {
      when (it.size) {
        1 -> mkdir(it.first())
        2 -> mkdir(it.first(), it[1])
        else -> throw JsError.typeError("Invalid number of arguments to `fs.mkdir`")
      }
    }

    FS_F_COPY_FILE -> ProxyExecutable {
      when (it.size) {
        2 -> copyFile(it.first(), it[1])
        3 -> copyFile(it.first(), it[1], it[2].asInt())
        else -> throw JsError.typeError("Invalid number of arguments to `fs.copyFile`")
      }
    }

    FS_P_CONSTANTS -> FilesystemConstants

    else -> null
  }

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

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
@file:Suppress(
  "NestedBlockDepth",
  "NOTHING_TO_INLINE",
  "TooManyFunctions",
  "CyclomaticComplexMethod",
  "ThrowsCount",
)

package elide.runtime.node.childProcess

import com.google.common.util.concurrent.Futures.withTimeout
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.rmi.RemoteException
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.util.function.Supplier
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.absolutePathString
import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.exec.GuestExecutor
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.ProcessManager
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.URL
import elide.runtime.intrinsics.js.node.ChildProcessAPI
import elide.runtime.intrinsics.js.node.childProcess.*
import elide.runtime.intrinsics.js.node.childProcess.StdioSymbols.PIPE
import elide.runtime.intrinsics.js.node.events.EventEmitter
import elide.runtime.intrinsics.js.node.events.EventTarget
import elide.runtime.intrinsics.js.node.fs.StringOrBuffer
import elide.runtime.intrinsics.js.node.path.Path
import elide.runtime.lang.javascript.SyntheticJSModule
import elide.runtime.node.events.EventAware
import elide.runtime.node.fs.NodeFilesystemModule
import elide.runtime.node.fs.resolveEncodingString
import elide.vm.annotations.Polyglot
import com.google.common.util.concurrent.ListenableFuture as Future

// Name of the child process module.
private const val CHILD_PROCESS_MODULE_NAME = "child_process"

// Internal symbol where the Node built-in module is installed.
private const val CHILD_PROCESS_MODULE_SYMBOL = "node_${CHILD_PROCESS_MODULE_NAME}"

// Internal symbol where the Node `ChildProcess` class is installed.
private const val CHILD_PROCESS_CLASS_SYMBOL = "NodeChildProcess"

// Symbol indicating a no-encoding return.
private const val NO_ENCODING = ChildProcessDefaults.ENCODING

// Names of events which are emitted for/upon `ChildProcess` objects.
internal data object ChildProcessEvents {
  const val CLOSE = "close"
  const val DISCONNECT = "disconnect"
  const val ERROR = "error"
  const val EXIT = "exit"
  const val MESSAGE = "message"
}

// IPC server.
private val globalIpcServer by lazy { InterElideIPCServer() }

// Installs the Node child process module into the intrinsic bindings.
@Intrinsic
@Factory internal class NodeChildProcessModule(
  filesystem: NodeFilesystemModule,
  executorProvider: GuestExecutorProvider,
) : SyntheticJSModule<ChildProcessAPI>, AbstractNodeBuiltinModule() {
  private val defaultStandardStreams = object : StandardStreamsProvider {
    override fun stdin(): InputStream? = System.`in`
    override fun stdout(): OutputStream? = System.out
    override fun stderr(): OutputStream? = System.err
  }

  private val instance = NodeChildProcess.obtain(
    { globalIpcServer },
    filesystem,
    executorProvider,
    defaultStandardStreams,
  )

  @Singleton override fun provide(): ChildProcessAPI = instance
  @Singleton internal fun ipcServer(): InterElideIPCServer = globalIpcServer

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[CHILD_PROCESS_MODULE_SYMBOL.asJsSymbol()] = provide()
    bindings[CHILD_PROCESS_CLASS_SYMBOL.asJsSymbol()] = ChildProcessHandle::class.java
  }
}

// Process stream wrappers are closeable.
public sealed interface ProcessStream : AutoCloseable

// Stream which emits data to a subprocess, through the process' standard-in facility.
@JvmInline public value class ProcessInputStream private constructor(private val stream: OutputStream) :
  ProcessStream,
  AutoCloseable by stream {
  // Factories for input streams.
  internal companion object {
    // Create a new input stream from a given output stream.
    @JvmStatic fun from(stream: OutputStream): ProcessInputStream = ProcessInputStream(stream)
  }
}

// Stream which consumes data from a subprocess, through the process' standard-out or standard-err facilities.
@JvmInline public value class ProcessOutputStream private constructor(private val stream: InputStream) :
  ProcessStream,
  AutoCloseable by stream {
  // Factories for input streams.
  internal companion object {
    // Create a new output stream from a given input stream.
    @JvmStatic fun from(stream: InputStream): ProcessOutputStream = ProcessOutputStream(stream)
  }

  // Read the stream into a string or buffer.
  internal fun readToStringOrBuffer(opts: ProcOptions): StringOrBuffer = stream.use {
    it.readAllBytes().let { bytes ->
      when (val encoding = opts.encoding) {
        NO_ENCODING, null -> bytes
        else -> bytes.toString(resolveEncodingString(encoding))
      }
    }
  }
}

// Required interface for call info implementations.
internal sealed interface ChildProcessExecution<T> where T : ProcOptions {
  // Parsed or materialized command name which should be invoked.
  val command: String

  // Parsed or materialized arguments to provide for the command invocation.
  val args: LinkedList<String>

  // Call arguments to apply, in specialized type `T` of base `ProcOptions`.
  val options: T

  // Standard-in stream to provide for this call, if any.
  val stdin: ProcessInputStream?

  // Standard-out stream to provide for this call, if any.
  val stdout: ProcessOutputStream?

  // Standard-error stream to provide for this call, if any.
  val stderr: ProcessOutputStream?
}

// Describes an execution which is to occur through a command call.
@ConsistentCopyVisibility
@JvmRecord private data class CommandExec<T : ProcOptions> private constructor(
  override val command: String,
  override val args: LinkedList<String>,
  override val options: T,
  override val stdin: ProcessInputStream? = null,
  override val stdout: ProcessOutputStream? = null,
  override val stderr: ProcessOutputStream? = null,
) : ChildProcessExecution<T> {
  companion object {
    @JvmStatic
    fun of(command: String, args: LinkedList<String>, options: ProcOptions): CommandExec<ProcOptions> =
      CommandExec(command, args, options)
  }
}

// Describes an execution which is to occur through a specific file.
@ConsistentCopyVisibility
@JvmRecord private data class FileExec<T : ProcOptions> private constructor(
  // Resolved to the file which is to be executed.
  val path: Path,
  override val command: String,
  override val args: LinkedList<String>,
  override val options: T,
  override val stdin: ProcessInputStream? = null,
  override val stdout: ProcessOutputStream? = null,
  override val stderr: ProcessOutputStream? = null,
) : ChildProcessExecution<T> {
  companion object {
    @JvmStatic fun of(
      path: Path,
      command: String,
      args: LinkedList<String>,
      options: ProcOptions,
    ): FileExec<ProcOptions> = FileExec(path, command, args, options)
  }
}

// Exit code integer type.
private typealias ExitCode = UInt

// Describes an exit code paired with a failure signal: processes that fail (but never exit) are modeled here.
@JvmInline internal value class ExitResult(private val exit: Triple<Boolean?, Boolean, ExitCode?>) {
  // Exit code of the process.
  val code: ExitCode? get() = exit.third

  // Success flag of the process.
  val success: Boolean get() = exit.first ?: false

  // Success flag of the process.
  val didTimeout: Boolean get() = exit.second

  // Companion object for exit result construction.
  companion object {
    // Construct a successful exit result.
    fun success(): ExitResult = ExitResult(Triple(true, false, 0u))

    // Construct a failed (timed out) exit result.
    fun timeout(): ExitResult = ExitResult(Triple(false, true, 1u))

    // Construct a failed exit result.
    fun failure(code: ExitCode): ExitResult = ExitResult(Triple(true, false, code))
  }
}

// Required interface for call result implementations.
internal sealed interface CallResult<T> where T : ProcOptions {
  // Command execution relating to this result.
  val execution: ChildProcessExecution<T>

  // Exit code of the process.
  val exit: ExitResult?

  // Simple success or failure flag, calculated from the `exitCode`.
  val success: Boolean get() = exit?.success ?: false

  // Pair of `(stdout, stderr)` availability indicators.
  val outputStreamsAvailable: Pair<Boolean, Boolean>

  companion object {
    // Build a raw call result into a `CallResult` structure, using the appropriate return type depending on whether
    // output streams are available.
    @JvmStatic fun <T : ProcOptions> build(
      exec: ChildProcessExecution<T>,
      exitResult: ExitResult,
      stdin: ProcessInputStream?,
      stdout: ProcessOutputStream?,
      stderr: ProcessOutputStream?,
    ): CallResult<T> {
      return when {
        // if we have streams at all, we affix them
        stdout != null || stderr != null -> StreamsResult(
          exitResult to exec,
          stdin,
          stdout,
          stderr,
        )

        // otherwise we just return a `NoStreamsResult` which has less overhead
        else -> NoStreamsResult(exitResult to exec)
      }
    }
  }
}

// Pending `Future<CallResult<T>>` type for async process spawn.
internal class PendingCallResult<T> private constructor(
  override val execution: ChildProcessExecution<T>,
  private val stdin: ProcessInputStream?,
  private val stdout: ProcessOutputStream?,
  private val stderr: ProcessOutputStream?,
) : StreamsCallResult<T> where T : ProcOptions {
  override val exit: ExitResult? get() = null
  override val success: Boolean get() = false
  override val outputStreamsAvailable: Pair<Boolean, Boolean> get() = (stdout != null) to (stderr != null)
  override fun stdin(): ProcessInputStream? = stdin
  override fun stdout(): ProcessOutputStream? = stdout
  override fun stderr(): ProcessOutputStream? = stderr

  companion object {
    // Create an in-flight call result, which holds onto a future to produce the actual `CallResult<T>` later on; in the
    // meantime, streams and process information are still available.
    @JvmStatic fun <T : ProcOptions> inFlight(
      exec: ChildProcessExecution<T>,
      stdin: ProcessInputStream?,
      stdout: ProcessOutputStream?,
      stderr: ProcessOutputStream?,
    ): CallResult<T> = PendingCallResult(
      exec,
      stdin,
      stdout,
      stderr,
    )
  }
}

// Required interface for call result implementations which provide resulting streams.
private sealed interface StreamsCallResult<T> : CallResult<T> where T : ProcOptions {
  // Obtain the standard input stream for this call result.
  fun stdin(): ProcessInputStream?

  // Obtain the standard output stream for this call result.
  fun stdout(): ProcessOutputStream?

  // Obtain the standard error stream for this call result.
  fun stderr(): ProcessOutputStream?
}

// Describes a call result which does not carry any streams.
@JvmInline private value class NoStreamsResult<T>(
  private val result: Pair<ExitResult, ChildProcessExecution<T>>,
) : CallResult<T> where T : ProcOptions {
  override val execution: ChildProcessExecution<T> get() = result.second
  override val success: Boolean get() = result.first.success
  override val exit: ExitResult get() = result.first
  override val outputStreamsAvailable: Pair<Boolean, Boolean> get() = false to false
}

// Describes a call result which carries raw (untransformed) streams.
@JvmRecord private data class StreamsResult<T>(
  private val result: Pair<ExitResult, ChildProcessExecution<T>>,
  private val stdin: ProcessInputStream? = null,
  private val stdout: ProcessOutputStream? = null,
  private val stderr: ProcessOutputStream? = null,
) : StreamsCallResult<T> where T : ProcOptions {
  override val execution: ChildProcessExecution<T> get() = result.second
  override val success: Boolean get() = result.first.success
  override val exit: ExitResult get() = result.first
  override val outputStreamsAvailable: Pair<Boolean, Boolean> get() = (stdout != null) to (stderr != null)
  override fun stdin(): ProcessInputStream? = stdin
  override fun stdout(): ProcessOutputStream? = stdout
  override fun stderr(): ProcessOutputStream? = stdout
}

// Using a `CommandExecution`, perform a call asynchronously using `callAsync`, and then block based on the assigned
// timeout for the command, if any.
private inline fun <T : ProcOptions> ChildProcessExecution<T>.callAndBlock(
  callAsync: (ChildProcessExecution<T>) -> Future<Pair<Process, CallResult<T>>>,
): Pair<Process, CallResult<T>> = callAsync(this).let { fut ->
  when (val timeout = options.timeout) {
    null -> fut.get()
    else -> fut.get(timeout.inWholeMilliseconds, MILLISECONDS)
  }
}

/**
 * ## Standard Streams Provider
 *
 * Provider interface for standard parent streams, which are used in cases where stdio is inherited from the parent
 * process to a child process.
 *
 * The default implementation provides access to the JVM parent's streams.
 */
public interface StandardStreamsProvider {
  public fun stdin(): InputStream? = null
  public fun stdout(): OutputStream? = null
  public fun stderr(): OutputStream? = null
}

/**
 * Parse a string into a "command" list, with the first list position being the command name, and all remaining list
 * entries being command arguments.
 *
 * String arguments are processed according to shell-style quoting; in other words, arguments are split at ` ` character
 * boundaries, with consideration for quoting.
 *
 * @param cmd Command string
 * @param extraArgs Additional arguments parameter (from guest) to consider
 * @return List implementing the command; first element is the command name, all remaining elements are arguments.
 */
internal inline fun tokenizeCommand(cmd: String, extraArgs: Value? = null): Pair<String, LinkedList<String>> {
  // walk the string up to the first space, considering that first portion the "command name"; this becomes the first
  // element in the resulting array.
  val firstTokenIndex = cmd.indexOf(' ')
  val commandName = if (firstTokenIndex > 0) cmd.substring(0, cmd.indexOf(' ')) else cmd
  val commandArgs = cmd.drop(commandName.length + 1).let { argsString ->
    var inQuote: String? = null
    val args = LinkedList<String>()
    var arg = StringBuilder()
    for (char in argsString) {
      when (char) {
        // handle single and double quotes, which establish a quoted argument value
        '"', '\'' -> when (inQuote) {
          null -> inQuote = char.toString()
          char.toString() -> inQuote = null
          else -> arg.append(char)
        }

        // handle spaces, which delineate arguments (when *not* operating in a quoted argument value)
        ' ' -> if (inQuote != null) arg.append(char) else {
          args.add(arg.toString())
          arg = StringBuilder()
        }

        // otherwise, simply add to the argument we are building
        else -> arg.append(char)
      }
    }

    // by now, all quote segments should have been settled; if not, we have an unbalanced quote error.
    if (inQuote != null) throw JsError.valueError("Unbalanced quote in command string")

    // add last buffered arg if any
    if (arg.isNotEmpty()) args.add(arg.toString())

    // if we have `extraArgs`, factor that in, too
    if (extraArgs != null) when {
      extraArgs.isNull -> { /* no-op */
      }

      extraArgs.isHostObject -> {
        extraArgs.`as`(Array<String>::class.java).forEach { args.add(it) }
      }

      extraArgs.hasArrayElements() -> {
        val elementCount = extraArgs.arraySize
        for (i in 0 until elementCount) {
          val element = extraArgs.getArrayElement(i)
          when {
            element.isString -> args.add(element.asString())
            else -> error("Unrecognized `extraArgs` array element type")
          }
        }
      }

      else -> error("Unrecognized `args` value type")
    }

    args
  }
  return commandName to commandArgs
}

// Convert a `CommandExecution<T>` to a `ProcessBuilder` instance.
private inline fun <T : ProcOptions> ChildProcessExecution<T>.toProcBuilder(): ProcessBuilder {
  return ProcessBuilder().apply {
    // assign command
    val (cmd, isShell) = when (this@toProcBuilder) {
      is CommandExec<*> -> when (val shell = options.shell) {
        null -> command to false
        else -> shell to true
      }

      is FileExec<*> -> when (val shell = options.shell) {
        null -> path.toString() to false
        else -> shell to true
      }
    }

    // apply command and arguments, accounting for shell options
    val cmdArgs = LinkedList<String>()
    cmdArgs.add(cmd)
    if (!isShell) cmdArgs.addAll(args) else {
      cmdArgs.add("-c")
      cmdArgs.add(
        StringBuilder().apply {
          append(command)
          append(" ")
          args.forEach { append(it) }
        }.toString(),
      )
    }

    // assign as command args
    command(cmdArgs)

    // apply cwd, other options
    options.cwdString?.let { directory(File(it)) }

    // apply environment
    options.env?.let { env ->
      when {
        env.isEmpty() -> environment().clear()
        else -> environment().let { targetEnv ->
          targetEnv.putAll(env)
          (targetEnv.keys - env.keys).forEach { targetEnv.remove(it) }
        }
      }
    }

    // apply streams
    options.stdio.applyTo(this)
  }
}

// Prepare to execute a file, first checking that it exists and is executable.
@Suppress("SwallowedException")
private fun prepareFileExec(file: Value, args: Value?): Triple<Path, String, LinkedList<String>> {
  val (pathAsUrl, pathAsString) = when {
    file.isString -> null to file.asString()

    file.isHostObject -> try {
      file.`as`(URL::class.java) to null
    } catch (e: ClassCastException) {
      null to null
    }

    else -> null to null
  }

  val path: Path = when {
    pathAsUrl != null -> error("Paths as URLs are not suppoted for `execFileSync` at this time")
    pathAsString != null -> Path.from(pathAsString)
    else -> throw JsError.typeError("The file argument must be a string or a URL")
  }

  // validations of various kinds
  val nioPath = path.toJavaPath()
  when {
    !Files.exists(nioPath) -> throw JsError.valueError("The file at path `$path` does not exist")
    !Files.isExecutable(nioPath) -> throw JsError.valueError("The file at path `$path` is not executable")
  }
  val (cmd, finalizedArgs) = tokenizeCommand(pathAsString, args)
  return Triple(path, cmd, finalizedArgs)
}

// Unwrap a synchronous process call to a string-or-buffer, depending on encoding options.
private fun <T : ProcOptions> unwrapSyncToStringOrBuffer(result: CallResult<T>, opts: T): StringOrBuffer? {
  return when (result) {
    is PendingCallResult<*> -> error("Invalid state: Pending call result for sync operation")
    is NoStreamsResult -> null
    is StreamsResult -> result.outputStreamsAvailable.let { (stdoutAvailable, _) ->
      if (stdoutAvailable) result.stdout()?.readToStringOrBuffer(opts) else null
    }
  }
}

/**
 * ## Child Process Exception
 *
 * Base exception type for exceptions which can occur when launching or interacting with child processes.
 */
public abstract class ChildProcessException(message: String, cause: Throwable? = null) :
  RuntimeException(message, cause)

/**
 * ## Child Process Failure
 *
 * Thrown when a synchronously launched child process fails with a non-zero exit code.
 *
 * @property exitCode Exit code of the child process.
 */
public class ChildProcessFailure(public val exitCode: Int) :
  ChildProcessException("Child process failed with exit code $exitCode")

/**
 * ## Child Process Timeout
 *
 * Thrown when a child process which was spawned or executed through the Node API times out.
 */
public class ChildProcessTimeout : ChildProcessException("Child process call timed out")

// Tracks child process terminal/exit conditions.
internal class ChildProcessTermination {
  @Volatile var exitCode: Int = -1
  @Volatile var exitSignal: String = ""

  // Whether the process has closed.
  val closed: Boolean get() = exitCode != -1 || exitSignal.isNotEmpty()

  fun exited(code: Int) {
    EXIT_CODE_UPDATER.set(this, code)
  }

  fun signal(signal: String) {
    EXIT_SIGNAL_UPDATER.set(this, signal)
  }

  private companion object {
    private val EXIT_CODE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(
      ChildProcessTermination::class.java,
      ChildProcessTermination::exitCode.name,
    )

    private val EXIT_SIGNAL_UPDATER = AtomicReferenceFieldUpdater.newUpdater(
      ChildProcessTermination::class.java,
      String::class.java,
      ChildProcessTermination::exitSignal.name,
    )
  }
}

// Properties and methods of `ChildProcess` which are exposed to guest code.
private val CHILD_PROCESS_HANDLE_PROPS_AND_METHODS = arrayOf(
  "pid",
  "stdin",
  "stdout",
  "stderr",
  "exitCode",
  "signalCode",
  "wait",
  "stdio",
  "connected",
  "channel",
  "killed",
  "kill",
  "disconnect",
  "ref",
  "unref",
  "send",
)

/**
 * ## Abstract Child Process Handle
 *
 * Implements common base functionality for handles which address child processes; there are two implementations of this
 * base provided by Elide:
 *
 * - [ChildProcessHandle]: Async-capable child process handle, which needs to address things like standard streams,
 *   signals, and so on; created for a live process.
 * - [ChildProcessSyncHandle]: Synchronous child process handle, which is typically created for a process which has
 *   already closed or otherwise been terminated.
 *
 *  @param events Override the event manager to use for this process handle
 */
public abstract class AbstractChildProcessHandle internal constructor(
  result: CallResult<*>? = null,
  private val events: EventAware = EventAware.create(),
) : EventEmitter by events, EventTarget by events {
  internal val result: AtomicReference<CallResult<*>> = AtomicReference(result)
  internal val termination: ChildProcessTermination = ChildProcessTermination()
}

/**
 * ## Node API: Child Process
 *
 * Object returned from async process spawn/exec methods which provide access to the underlying child process; the
 * object exposes access to the process [pid], [stdin], [stdout], [stderr], and terminal exit information, in the form
 * of an [exitCode], [signalCode], [killed] state, and so on.
 *
 * When this object is first created and provided, the child process backing the object may still be alive (in many
 * cases, such as long-running server processes, this is intended behavior).
 *
 * If the backing process is still alive, terminal information will not be available, and [signalCode], [exitCode], and
 * related properties return `null`.
 *
 * When the process exits, or is terminated through a signal, these fields are filled in by the runtime and become
 * visible to the caller.
 *
 * @property pid Process ID for the child process.
 * @property stdin Standard input stream for the child process.
 * @property stdout Standard output stream for the child process.
 * @property stderr Standard error stream for the child process.
 * @property exitCode Exit code for the process, or `null` if the subprocess terminated due to a signal.
 */
public class ChildProcessHandle private constructor(
  private val proc: Process,
  private val pending: PendingCallResult<*>,
) : ProxyObject, ChildProcess, AbstractChildProcessHandle() {
  // Bound IPC channel for this process handle, if available.
  private val liveIpcChannel: AtomicRef<InterElideIPCClient?> = atomic(null)

  @Volatile private var didKill: Boolean = false
  @get:Polyglot override val pid: Long get() = proc.pid()
  @get:Polyglot override val exitCode: Int? get() = termination.exitCode.takeIf { it != -1 }
  @get:Polyglot override val signalCode: String? get() = termination.exitSignal.takeIf { it != "" }
  @get:Polyglot override val stdin: ProcessInputStream? get() = pending.stdin()
  @get:Polyglot override val stdout: ProcessOutputStream? get() = pending.stdout()
  @get:Polyglot override val stderr: ProcessOutputStream? get() = pending.stderr()
  @get:Polyglot override val connected: Boolean get() = !termination.closed
  @get:Polyglot override val channel: ProcessChannel? get() = liveIpcChannel.value
  @get:Polyglot override val killed: Boolean get() = didKill
  @Polyglot override fun wait(): Int = when (val timeout = pending.execution.options.timeout) {
    null -> proc.waitFor()
    else -> proc.waitFor(timeout.inWholeMilliseconds, MILLISECONDS).let {
      if (!proc.isAlive) proc.exitValue() else {
        kill("SIGKILL")
        throw ChildProcessTimeout()
      }
    }
  }

  @get:Polyglot override val stdio: ProcessIOChannels
    get() = object : ProcessIOChannels {
      @get:Polyglot override val stdin: ProcessInputStream? get() = pending.stdin()
      @get:Polyglot override val stdout: ProcessOutputStream? get() = pending.stdout()
      @get:Polyglot override val stderr: ProcessOutputStream? get() = pending.stderr()
    }

  @Suppress("TooGenericExceptionCaught")
  @Polyglot override fun kill(signal: String?): Unit = (signal ?: pending.execution.options.killSignal).let {
    try {
      ChildProcessNative.killWith(pid, it)
      termination.signal(it)
    } catch (e: Throwable) {
      throw JsError.valueError("Failed to send signal to process: ${e.message}", e)
    }

    didKill = true
    result.set(
      CallResult.build(
        pending.execution,
        ExitResult.success(),
        pending.stdin(),
        pending.stdout(),
        pending.stderr(),
      ),
    )
    emit(
      ChildProcessEvents.EXIT,
      exitCode,
      signalCode,
    )
  }

  @Polyglot override fun disconnect() {
    TODO("Not yet implemented: `ChildProcessHandle.disconnect`")
  }

  @Polyglot override fun ref(): ProcessChannel {
    TODO("Not yet implemented: `ChildProcessHandle.ref`")
  }

  @Polyglot override fun unref(): ProcessChannel {
    TODO("Not yet implemented: `ChildProcessHandle.unref`")
  }

  @Polyglot override fun send(message: Value, sendHandle: Value?, options: Value?, callback: Value?): Boolean =
    channel?.send(message, sendHandle, options, callback) ?: false

  // Bind an IPC channel established for this child process. Internal use only.
  internal fun bindChannel(channel: InterElideIPCClient) {
    liveIpcChannel.value = channel
  }

  override fun getMemberKeys(): Array<String> = CHILD_PROCESS_HANDLE_PROPS_AND_METHODS
  override fun hasMember(key: String?): Boolean = key != null && key in CHILD_PROCESS_HANDLE_PROPS_AND_METHODS
  override fun putMember(key: String?, value: Value?) {/* no-op */
  }

  override fun removeMember(key: String?): Boolean = false

  override fun getMember(key: String?): Any? = when (key) {
    "pid" -> pid
    "stdin" -> stdin
    "stdout" -> stdout
    "stderr" -> stderr
    "exitCode" -> exitCode
    "signalCode" -> signalCode
    "wait" -> ProxyExecutable { wait() }
    "stdio" -> stdio
    "connected" -> connected
    "channel" -> channel
    "killed" -> killed
    "kill" -> ProxyExecutable { args ->
      val arg = args.getOrNull(0) ?: throw JsError.typeError("Expected signal argument")
      if (!arg.isString) throw JsError.typeError("Expected signal argument to be a string")
      kill(arg.asString())
    }

    "disconnect" -> ProxyExecutable { disconnect() }
    "ref" -> ProxyExecutable { ref() }
    "unref" -> ProxyExecutable { unref() }
    "send" -> ProxyExecutable { args ->
      val message = args.getOrNull(0) ?: throw JsError.typeError("Expected message argument")
      val sendHandle = args.getOrNull(1)
      val options = args.getOrNull(2)
      val callback = args.getOrNull(3)
      send(message, sendHandle, options, callback)
    }

    else -> null
  }

  public companion object {
    // Create a handle for a live process.
    @JvmStatic internal fun live(
      handle: Process,
      pending: PendingCallResult<*>,
    ): ChildProcessHandle = ChildProcessHandle(
      handle,
      pending,
    )
  }
}

// Properties and methods of `ChildProcess` which are exposed to guest code.
private val CHILD_PROCESS_SYNC_HANDLE_PROPS_AND_METHODS = arrayOf(
  "pid",
  "status",
  "stdout",
  "stderr",
  "output",
  "signal",
  "error",
)

/**
 * ## Node API: Child Process Handle (Synchronous)
 *
 * Represents a [ChildProcess] which executed synchronously, and therefore has already terminated; this object provides
 * access to the [pid] used for the process, plus [stdout] and [stderr] streams, and the [status] exit of the process
 * (or a [signal] if the process exited through a signal).
 *
 * @property pid Process ID for the child process.
 * @property status Exit status for the process, or `null` if the subprocess terminated due to a signal.
 * @property stdout Standard output stream for the child process.
 * @property stderr Standard error stream for the child process.
 * @property output Standard output and error streams for the child process.
 * @property signal Signal which caused the process to terminate, if any.
 * @property error Error object, if the process failed to start or exited with a non-zero exit code.
 */
public class ChildProcessSyncHandle private constructor(
  private val handle: ProcessHandle,
  result: CallResult<ProcOptions>,
) : ProxyObject, ChildProcessSync, AbstractChildProcessHandle(result) {
  @get:Polyglot override val pid: Long get() = handle.pid()
  @get:Polyglot override val status: Int? get() = result.get()?.exit?.code?.toInt()
  @get:Polyglot override val signal: String? get() = termination.exitSignal.takeIf { it.isNotEmpty() }
  @get:Polyglot override val stdout: ProcessOutputStream?
    get() = when (val out = result.get()) {
      is StreamsCallResult<*> -> out.stdout()
      else -> null
    }

  @get:Polyglot override val stderr: ProcessOutputStream?
    get() = when (val out = result.get()) {
      is StreamsCallResult<*> -> out.stderr()
      else -> null
    }

  @get:Polyglot override val output: LazyProcessOutput
    get() = TODO("Not yet implemented: `ChildProcessSyncHandle.output`")

  // @TODO(sgammon): not yet implemented
  @get:Polyglot override val error: elide.runtime.intrinsics.js.err.JsError? get() = null

  override fun getMemberKeys(): Array<String> = CHILD_PROCESS_SYNC_HANDLE_PROPS_AND_METHODS
  override fun hasMember(key: String?): Boolean = key != null && key in CHILD_PROCESS_SYNC_HANDLE_PROPS_AND_METHODS
  override fun putMember(key: String?, value: Value?) { /* no-op */
  }

  override fun removeMember(key: String?): Boolean = false

  override fun getMember(key: String?): Any? = when (key) {
    "pid" -> pid
    "status" -> status
    "stdout" -> stdout
    "stderr" -> stderr
    "output" -> output
    "signal" -> signal
    "error" -> error
    else -> null
  }

  internal companion object {
    @JvmStatic fun of(handle: ProcessHandle, result: CallResult<ProcOptions>): ChildProcessSyncHandle =
      ChildProcessSyncHandle(handle, result)
  }
}

// Underlying implementation to spawn a subprocess asynchronously; used from all implementations, which block at the
// call-site, if necessary, according to their return contract.
private inline fun <T : ProcOptions> callAsync(
  executor: GuestExecutor,
  exec: ChildProcessExecution<T>,
  wait: Boolean = true,
): Future<Pair<Process, CallResult<T>>> {
  return exec.toProcBuilder().let { builder ->
    executor.submit<Pair<Process, CallResult<T>>> {
      runBlocking(executor.dispatcher) {
        builder.start().let { proc ->
          val stdin: ProcessInputStream? = when {
            // in `stdin=PIPE` mode, we should obtain it and attach
            exec.options.stdio.stdin == PIPE -> ProcessInputStream.from(requireNotNull(proc.outputStream))
            else -> null
          }
          val stdout: ProcessOutputStream? = when {
            // in `stdout=PIPE` mode, we should read until it yields, and decode in the configured encoding.
            exec.options.stdio.stdout == PIPE -> ProcessOutputStream.from(requireNotNull(proc.inputStream))
            else -> null
          }
          val stderr: ProcessOutputStream? = when {
            // in `stderr=PIPE` mode, we should read until it yields, and decode in the configured encoding.
            exec.options.stdio.stderr == PIPE -> ProcessOutputStream.from(requireNotNull(proc.errorStream))
            else -> null
          }

          if (!wait) {
            proc to PendingCallResult.inFlight(
              exec,
              stdin,
              stdout,
              stderr,
            )
          } else {
            val result = runCatching {
              when (val timeout = exec.options.timeout) {
                null -> proc.waitFor()
                else -> proc.waitFor(timeout.inWholeMilliseconds, MILLISECONDS).let { returned ->
                  when (returned) {
                    false -> throw ChildProcessTimeout()
                    true -> proc.exitValue()
                  }
                }
              }
            }

            // obtain an exit code, or `null` if the process was canceled and didn't terminate.
            val exitResult = when (val exitCode = result.getOrThrow()) {
              0 -> ExitResult.success()
              else -> ExitResult.failure(
                exitCode.toUInt().also {
                  assert(exitCode > 0) { "Cannot have negative exit code" }
                },
              )
            }
            proc to CallResult.build(
              exec,
              exitResult,
              stdin,
              stdout,
              stderr,
            )
          }
        }
      }
    }.let { fut ->
      when (val timeout = exec.options.timeout) {
        null -> fut
        else -> withTimeout(fut, timeout.inWholeMilliseconds, MILLISECONDS, executor)
      }
    }
  }
}

// Decide whether to start the RMI IPC server at initialization-time; this is governed by the presence of special-cased
// environment variables, which communicate to an Elide process which parent process is spawning it.
private fun shouldStartIpcAtInit(): Boolean = System.getenv().let { env ->
  return env.containsKey("ELIDE_PARENT_PID")
}

/**
 * # Node API: `child_process`
 *
 * Implements the `child_process` module as part of the Node API, detailed by the interface [ChildProcessAPI].
 * Child processes are launched through the JDK's [ProcessBuilder] interface, and managed through either the [Process]
 * or [ProcessHandle] interfaces, as proxied by [ChildProcess] and [ChildProcessSync].
 *
 * @TODO: enforcement of sub-process permissions
 */
internal class NodeChildProcess(
  private val ipcSupplier: Supplier<InterElideIPCServer>,
  private val filesystem: Optional<Supplier<NodeFilesystemModule>>,
  private val standardStreams: StandardStreamsProvider,
  private val executorProvider: GuestExecutorProvider
) : ChildProcessAPI {
  // Obtained executor for child-process operations.
  private val executor by lazy { executorProvider.executor() }

  init {
    Dispatchers.IO.dispatch(EmptyCoroutineContext) {
      if (shouldStartIpcAtInit()) {
        ipcSupplier.get().initialize()
      }
    }
  }

  // Underlying implementation to spawn a subprocess synchronously; used from all sync implementations, performing
  // blocking on behalf of the call-site.
  //
  // This variant operates with no callback.
  private inline fun <T : ProcOptions> callSync(exec: ChildProcessExecution<T>): Pair<Process, CallResult<T>> =
    exec.callAndBlock { callAsync(executor, exec, wait = true) }

  // Host-side internal method which performs a synchronous child-process call, on top of `callAsync`; this method is
  // used by guest-side calls like `execSync`.
  @VisibleForTesting fun hostExecSync(command: String, options: ExecSyncOptions): StringOrBuffer? {
    val (cmd, args) = tokenizeCommand(command)
    return try {
      unwrapSyncToStringOrBuffer(callSync(CommandExec.of(cmd, args, options)).second, options)
    } catch (exe: ExecutionException) {
      when (val cause = exe.cause ?: exe) {
        is ChildProcessTimeout, is ChildProcessFailure -> throw cause
        else -> throw JsError.of("Failed to launch process", cause = cause)
      }
    }
  }

  @Polyglot override fun spawn(command: Value, args: Value?, options: Value?): ChildProcess {
    if (!command.isString) throw JsError.typeError("The command argument must be a string")
    val (cmd, finalizedArgs) = tokenizeCommand(command.asString(), args)
    val opts = SpawnOptions.from(options)
    val (handle, result) = callAsync(executor, CommandExec.of(cmd, finalizedArgs, opts), wait = false).get()
    return ChildProcessHandle.live(handle, result as PendingCallResult<*>)
  }

  @Polyglot override fun exec(command: Value, options: Value?, callback: Value?): ChildProcess {
    if (!command.isString) throw JsError.typeError("The command argument must be a string")
    val (cmd, finalizedArgs) = tokenizeCommand(command.asString())
    val opts = ExecOptions.from(options)
    return try {
      val (handle, result) = callAsync(executor, CommandExec.of(cmd, finalizedArgs, opts), wait = false).get()
      val proc = ChildProcessHandle.live(handle, result as PendingCallResult<*>)
      if (callback != null && callback.canExecute()) {
        callback.executeVoid(null, proc.stdout, proc.stderr)
      }
      proc
    } catch (err: ChildProcessException) {
      if (callback != null && callback.canExecute()) {
        callback.executeVoid(JsError.of(err.message ?: "Failed to launch child process", cause = err))
      }
      throw err
    }
  }

  @Polyglot override fun execFile(file: Value, args: Value?, options: Value?, callback: Value?): ChildProcess {
    val (_, cmd, finalizedArgs) = prepareFileExec(file, args)
    val opts = ExecOptions.from(options)
    val (handle, result) = callAsync(executor, CommandExec.of(cmd, finalizedArgs, opts), wait = false).get()
    return ChildProcessHandle.live(handle, result as PendingCallResult<*>)
  }

  @Polyglot override fun fork(modulePath: Value, args: Value?, options: Value?): ChildProcess {
    val self = ProcessHandle.current()
    val mgr = ProcessManager.acquire()
    val binpath = mgr.binpath() ?: throw JsError.wrap(IllegalStateException("Cannot `fork`: no bin-path is available"))

    // we need to trigger a subprocess against the current binary, and include an injected env variable for the child to
    // notice how it is being spawned, and open an IPC channel back to the parent.
    val (handle, result) = CommandExec.of(
      binpath.absolutePathString(),
      LinkedList(),
      ForkOptions.from(self, options),
    ).let { spec ->
      callAsync(executor, spec).get()
    }

    // now, we need to obtain an IPC client for this process.
    val client = try {
      InterElideIPCClient.connect(handle.pid())
    } catch (err: RemoteException) {
      handle.destroy()
      throw JsError.of("Failed to establish IPC channel with forked process", cause = err)
    }

    // finally, we need to inform the process result of the new channel, and return to the caller.
    return ChildProcessHandle.live(handle, result as PendingCallResult<*>).also { proc ->
      proc.bindChannel(client)
    }
  }

  @Polyglot override fun spawnSync(command: Value, args: Value?, options: Value?): ChildProcessSync {
    if (!command.isString) throw JsError.typeError("The command argument must be a string")
    val (cmd, finalizedArgs) = tokenizeCommand(command.asString(), args)
    val opts = SpawnSyncOptions.from(options)
    val (handle, result) = callSync(CommandExec.of(cmd, finalizedArgs, opts))
    return ChildProcessSyncHandle.of(handle.toHandle(), result)
  }

  @Polyglot override fun execSync(command: Value, options: Value?): StringOrBuffer? {
    if (!command.isString) throw JsError.typeError("The command argument must be a string")
    val (cmd, args) = tokenizeCommand(command.asString())
    val opts = ExecSyncOptions.from(options)
    return unwrapSyncToStringOrBuffer(callSync(CommandExec.of(cmd, args, opts)).second, opts)
  }

  @Polyglot override fun execFileSync(file: Value, args: Value?, options: Value?): StringOrBuffer? {
    val (path, cmd, finalizedArgs) = prepareFileExec(file, args)
    val opts = ExecSyncOptions.from(options)
    return unwrapSyncToStringOrBuffer(callSync(FileExec.of(path, cmd, finalizedArgs, opts)).second, opts)
  }

  /** Factory methods for creating and obtaining instances of [NodeChildProcess]. */
  internal companion object {
    // Child process manager singleton.
    private val SINGLETON = AtomicReference<NodeChildProcess>(null)

    /** @return Singleton instance of the [NodeChildProcess] module; it is initialized if not yet available. */
    internal fun obtain(
      ipcSupplier: Supplier<InterElideIPCServer>,
      fs: NodeFilesystemModule,
      executorProvider: GuestExecutorProvider,
      standardStreams: StandardStreamsProvider,
    ): NodeChildProcess = SINGLETON.get().let {
      when (it) {
        null -> NodeChildProcess(
          ipcSupplier,
          Optional.of(Supplier { fs }),
          standardStreams,
          executorProvider,
        ).also { childProc -> SINGLETON.set(childProc) }

        else -> it
      }
    }
  }
}

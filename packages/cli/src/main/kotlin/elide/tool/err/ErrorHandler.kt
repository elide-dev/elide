/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

@file:Suppress("DataClassPrivateConstructor")

package elide.tool.err

import dev.elide.uuid.Uuid
import dev.elide.uuid.uuid4
import picocli.CommandLine.IExitCodeExceptionMapper
import java.io.PrintWriter
import java.io.StringWriter
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.system.exitProcess
import elide.runtime.Logger
import elide.tool.cli.GuestLanguage
import elide.tool.cli.err.AbstractToolError
import elide.tool.err.ErrorHandler.Action.*
import elide.tool.err.ErrorHandler.ErrorActionStrategy.*
import elide.tool.err.ErrorHandler.ErrorStackFrame.Companion.toFrame
import elide.tool.err.ErrorHandler.ErrorUtils.buildStacktrace
import elide.tool.err.ErrorHandler.ErrorUtils.walkFrames

/**
 * # Error Handler
 *
 * Describes a class which can accept [Throwable] instances caught during execution of Elide, and then materialize
 * context for such errors so that they may be safely rendered, reported, and logged.
 *
 * &nbsp;
 *
 * ## Error Recorder
 *
 * The Error Handler manages the [ErrorRecorder], which is charged with persisting errors in a safe and repeatable
 * manner, so that they may be de-serialized and acted upon at a later time (for example, to report such errors to
 * GitHub).
 *
 * &nbsp;
 *
 * ## Process Exit
 *
 * Error Handler instances can be used as an [IExitCodeExceptionMapper], which Picocli will consult when deciding the
 * error code to use on process exit. For errors which are fatal and report a custom exit code, this will be preserved
 * via the implementation of this interface.
 *
 * &nbsp;
 *
 * ## Thread Exceptions
 *
 * Error handlers can also be used as [Thread.UncaughtExceptionHandler] listeners. Background thread errors are reported
 * like other types of errors, modulo settings for reporting of non-fatal exceptions.
 *
 * &nbsp;
 *
 * ## Guest Code
 *
 * Exceptions which arise during execution of guest code are decorated with additional information indicating where the
 * error took place (what file, line, etc.). Such information is passed on unmodified to the underlying [ErrorRecorder]
 * for further processing.
 *
 * @see ErrorRecorder for error recording functionality
 * @see IExitCodeExceptionMapper for the exit-code-mapper interface used by Picocli
 */
interface ErrorHandler : IExitCodeExceptionMapper, Thread.UncaughtExceptionHandler {
  /**
   * ## Error Utilities
   *
   * General utilities for working with [Throwable] types; for example, building stacktraces or tracing errors to their
   * original cause.
   */
  data object ErrorUtils {
    /**
     * ### Build stacktrace
     *
     * Build a string stacktrace as a formatted string; this uses the builtin [printStackTrace] function to render an
     * error as the JVM does normally.
     *
     * @return String-formatted stacktrace.
     */
    @JvmStatic fun Throwable.buildStacktrace(): String = StringWriter().let {
      fillInStackTrace().printStackTrace(PrintWriter(it))
      it.flush()
      it.close()
      it.toString()
    }

    /**
     * ### Walk error frames
     *
     * Walk the stack of error frames which describe where an error took place; return location information in an array
     * of all frames.
     *
     * @return Array of pairs, where each pair is itself a pair o pairs; the first inner pair of `(String, String)`
     *   describes the `(File, Function or method)` where the error took place. The second pair of `(Int, Int)`
     *   describes the `(Line, Column)` where the error took place.
     */
    @JvmStatic fun Throwable.walkFrames(): List<ErrorStackFrame> = fillInStackTrace().let { thr ->
      return thr.stackTrace.drop(1).map { it.toFrame() }.toImmutableList()
    }
  }

  /**
   * ## Error: Stack Frame
   *
   * Describes a single stack frame within the stack trace for an error; this record is serializable, allowing a set of
   * frames to be written to disk.
   *
   * @param moduleName Name of the JVM module from which this error arose, as applicable.
   * @param moduleVersion Version of the JVM module from which this error arose, as applicable.
   * @param classLoaderName Class-loader which loaded the class where this error took place, as applicable.
   * @param className Name of the class where this error took place, as applicable.
   * @param methodName Name of the method or function where this error took place, as applicable.
   * @param fileName Name of the file where this error took place, as applicable.
   * @param lineNumber Source line number where this error took place, as applicable.
   * @param columnNumber Source line column number where this error took place, as applicable.
   * @param isNativeMethod Indicates whether this frame is describing a native method (if not, it is a JVM method).
   */
  @JvmRecord @Serializable data class ErrorStackFrame private constructor (
    val moduleName: String? = null,
    val moduleVersion: String? = null,
    val classLoaderName: String? = null,
    val className: String? = null,
    val methodName: String? = null,
    val fileName: String? = null,
    val lineNumber: Int? = null,
    val columnNumber: Int? = null,
    val isNativeMethod: Boolean = false,
  ) {
    companion object {
      /**
       * Create a serializable stack frame record from a [StackTraceElement].
       *
       * @return [ErrorStackFrame] from this trace element.
       */
      @JvmStatic fun StackTraceElement.toFrame(): ErrorStackFrame = ErrorStackFrame(
        moduleName = moduleName,
        moduleVersion = moduleVersion,
        classLoaderName = classLoaderName,
        className = className,
        methodName = methodName,
        fileName = fileName,
        lineNumber = lineNumber,
        isNativeMethod = isNativeMethod,
      )
    }
  }

  /**
   * ## Error Context
   *
   * Describes context surrounding an error event which is being reported to the error handler. This record carries such
   * details as the stacktrace, top error coordinates, and the active language runtime.
   *
   * @param uuid Unique ID assigned to this error.
   * @param fatal Whether the error was fatal and caused a crash; defaults to `true`.
   * @param guest Indicates that this error arose during execution of guest code.
   * @param coordinates Indicates where the error took place (line, file, etc.), if known.
   * @param timestamp Timestamp indicating when this error arose.
   * @param stacktrace Stacktrace for the error, if one has already been generated.
   * @param frames Frames indicating where this error took place, if already known.
   * @param language Language runtime which was active when this error arose.
   * @param uuid UUID for this error; transient for the purposes of serialization.
   * @param thread Thread where this error occurred; transient for the purposes of serialization.
   */
  @Serializable data class ErrorContext(
    val fatal: Boolean = true,
    val guest: Boolean = false,
    val coordinates: ErrorCoordinates? = null,
    val timestamp: Instant = Clock.System.now(),
    val stacktrace: String? = null,
    val frames: List<ErrorStackFrame>? = null,
    val language: GuestLanguage? = null,
    @Transient val uuid: Uuid = uuid4(),
    @Transient val thread: Thread? = null,
  ) {
    companion object {
      /** Default (empty) error context. */
      @JvmStatic val DEFAULT: ErrorContext = ErrorContext()
    }
  }

  /**
   * ## Error Coordinates
   *
   * Describes where an error took place during execution, to the extent such information is known.
   *
   * @param file File where the error happened, if known.
   * @param line Line where the error happened, if known.
   * @param column Column where the error happened, if known.
   * @param threadId ID of the thread where the error took place, if known.
   * @param threadName Name of the thread where the error took place, if known.
   * @param language Language runtime which was active when this error arose, if applicable and if known.
   */
  @JvmRecord @Serializable data class ErrorCoordinates(
    val file: String? = null,
    val line: Int? = null,
    val column: Int? = null,
    val threadId: Long? = null,
    val threadName: String? = null,
    val language: String? = null,
  ) {
    companion object {
      /**
       * Generate a set of [ErrorCoordinates] for the top frame of the receiver [ErrorEvent].
       *
       * @receiver Error event to generate coordinates for.
       * @return Coordinates for the error.
       */
      @JvmStatic fun ErrorEvent.coordinates(): ErrorCoordinates {
        val firstFrame = frames?.firstOrNull()

        return ErrorCoordinates(
          file = firstFrame?.fileName,
          line = firstFrame?.lineNumber,
          column = firstFrame?.columnNumber,
          threadId = context.thread?.threadId(),
          threadName = context.thread?.name,
          language = language?.id,
        )
      }
    }
  }

  /**
   * # Error Event
   *
   * Describes the interface provided by an error event; this includes a unique ID ([uuid]), a [timestamp], any
   * available [context] for the error, and the [error] itself.
   */
  @Suppress("unused") sealed interface ErrorEvent : Comparable<ErrorEvent> {
    /** Return the UUID assigned to this error. */
    val uuid: Uuid

    /** Timestamp provided for this event. */
    val timestamp: Instant

    /** Return the error which this event describes. */
    val error: Throwable

    /** Return the context describing this error. */
    val context: ErrorContext

    /** Return the context describing this error. */
    val language: GuestLanguage?

    /** Rendered stacktrace for this error. */
    val stacktrace: String?

    /** Frame stack for this error. */
    val frames: List<ErrorStackFrame>?

    /** Error message provided by the underlying error, if any. */
    val message: String? get() = error.message

    /** Localized error message provided by the underlying error, if any. */
    val localizedMessage: String? get() = error.localizedMessage

    /** Fully qualified class name of the error type. */
    val errorType: String get() = error::class.java.name

    override fun compareTo(other: ErrorEvent): Int {
      return uuid.compareTo(other.uuid)
    }

    companion object {
      /**
       * Wrap the provided [err] and [context].
       *
       * @param err Error to wrap.
       * @param context Context for the error.
       * @return Error event wrapping the provided inputs.
       */
      @JvmStatic fun of(err: Throwable, context: ErrorContext? = null): ErrorEvent = ErrorEventImpl.of(
        err,
        (context ?: ErrorContext.DEFAULT).let {
          // fill in the stack trace and frames if not already present
          if (it.stacktrace == null || it.frames == null) {
            it.copy(
              stacktrace = it.stacktrace ?: err.buildStacktrace(),
              frames = it.frames ?: err.walkFrames(),
            )
          } else it
        },
      )
    }
  }

  /**
   * ## Error Event
   *
   * Lightweight class which pairs a caught [Throwable] with generated (or provided) [ErrorContext]; the context in
   * question, if available, describes where and why the error arose.
   *
   * @param err Error pair carried by this event.
   */
  @JvmInline
  private value class ErrorEventImpl private constructor (private val err: Pair<Throwable, ErrorContext>): ErrorEvent {
    companion object {
      /**
       * Wrap the provided [err] and [context].
       *
       * @param err Error to wrap.
       * @param context Context for the error.
       * @return Error event wrapping the provided inputs.
       */
      @JvmStatic fun of(err: Throwable, context: ErrorContext): ErrorEvent = ErrorEventImpl(err to context)
    }

    /** Return the UUID assigned to this error. */
    override val uuid: Uuid get() = err.second.uuid

    /** Timestamp provided for this event. */
    override val timestamp: Instant get() = err.second.timestamp

    /** Return the error which this event describes. */
    override val error: Throwable get() = err.first

    /** Return the context describing this error. */
    override val context: ErrorContext get() = err.second

    /** Return the rendered stacktrace for this error. */
    override val stacktrace: String? get() = err.second.stacktrace

    /** Return the list of frames for this error. */
    override val frames: List<ErrorStackFrame>? get() = err.second.frames

    /** Active language runtime where this error occurred. */
    override val language: GuestLanguage? get() = err.second.language
  }

  /**
   * ## Error Action
   *
   * Enumerates supported/available actions which can take place in follow-up to a caught error.
   */
  enum class Action {
    /**
     * ## Default
     *
     * Keep processing and perform whatever step would normally come next.
     */
    DEFAULT,

    /**
     * ## Suppress
     *
     * Take no further action on the error.
     */
    SUPPRESS,

    /**
     * ## Re-throw
     *
     * Throw the error again and let the caller handle it.
     */
    RETHROW,

    /**
     * ## Crash
     *
     * Force-crash the VM.
     */
    CRASH
  }

  /**
   * ## Error Action Strategy
   *
   * Describes the nature of difference follow-up actions which can be suggested in response to an error by the Error
   * Handler implementation; these strategies are only supported when an invoking consumer directly supports this
   * interface (i.e. threads and other system-internals will not respect this value).
   */
  sealed interface ErrorActionStrategy {
    /**
     * Describes the original error event.
     */
    val event: ErrorEvent

    /**
     * Describes the follow-up action which should take place.
     */
    val action: Action

    /**
     * Error that caused this crash originally, **or** error that should be re-thrown (in [Action.RETHROW] mode); in
     * many cases these are the same error.
     */
    val throwable: Throwable get() = event.error

    /**
     * Exit code to crash with.
     */
    val exitCode: Int get() = -1

    /**
     * Whether this error should be sent to logs.
     */
    val loggable: Boolean get() = true

    /**
     * Warning message that should result from this error.
     */
    val warning: String? get() = null

    /**
     * Logger which the error handler is offering for additional logging information, and which the error handler used
     * to log information, as applicable.
     */
    val logger: Logger

    /**
     * "Invoke" the follow-up default action; for each mode, this action is listed below.
     *
     * - [Action.CRASH]: The VM crashes with the assigned [exitCode], via [exitProcess].
     * - [Action.RETHROW]: The assigned [throwable] is thrown.
     * - [Action.SUPPRESS]: No action is taken; logs are emitted (modulo logging settings).
     */
    operator fun invoke(): Unit = when (action) {
      DEFAULT, SUPPRESS -> { { /* no action taken */ } }
      RETHROW -> { { throw throwable } }
      CRASH -> { { exitProcess(exitCode) } }
    }.let { action ->
      if (loggable) logger.error(throwable)
      action.invoke()
    }

    /**
     * ### Error Strategy: Crash
     *
     * This strategy advises that the caller should force-crash the VM, due to the provided [event], with the provided
     * [exitCode].
     *
     * @param logger Logger used/to use for this error.
     * @param event Error event which caused this crash advisory.
     * @param exitCode Exit code to report.
     */
    @JvmRecord data class Crash (
      override val logger: Logger,
      override val event: ErrorEvent,
      override val exitCode: Int = 1,
    ): ErrorActionStrategy {
      override val action: Action get() = CRASH
    }

    /**
     * ### Error Strategy: Re-throw
     *
     * This strategy advises that the caller should force-crash the VM, due to the provided [event], with the provided
     * [exitCode].
     *
     * @param logger Logger used/to use for this error.
     * @param event Error event which caused this re-throw.
     * @param throwable Throwable which should override the original error, if desired; defaults to `null`.
     */
    @JvmRecord data class Rethrow (
      override val logger: Logger,
      override val event: ErrorEvent,
      override val throwable: Throwable = event.error,
    ): ErrorActionStrategy {
      override val action: Action get() = RETHROW
    }

    /**
     * ### Error Strategy: Suppression
     *
     * This strategy advises that the caller should force-crash the VM, due to the provided [event], with the provided
     * [exitCode].
     *
     * @param warning Warning message that should result from this error.
     * @param logger Logger used/to use for this error.
     * @param event Error event which caused this re-throw.
     */
    @JvmRecord data class Suppress (
      override val warning: String?,
      override val logger: Logger,
      override val event: ErrorEvent,
    ): ErrorActionStrategy {
      override val action: Action get() = SUPPRESS
    }

    /**
     * ### Error Strategy: Default
     *
     * This strategy provides no specific error advice to the caller; optionally, a [warning] can be provided, in which
     * case it is logged.
     *
     * @param warning Warning message that should result from this error.
     * @param logger Logger used/to use for this error.
     * @param event Error event which caused this re-throw.
     */
    @JvmRecord data class Default (
      override val warning: String?,
      override val logger: Logger,
      override val event: ErrorEvent,
    ): ErrorActionStrategy {
      override val action: Action get() = DEFAULT
    }
  }

  /**
   * ## Error Handler: Context
   *
   * Describes the DSL context in which the error handler executes; this context provides utilities for creating various
   * [ErrorActionStrategy] responses to a given error event, and other utilities.
   *
   * Properties and utilities:
   *
   * - [event]: Access to the original error event.
   * - [suppress]: Generate a [ErrorActionStrategy.Suppress] response.
   * - [rethrow]: Generate a [ErrorActionStrategy.Rethrow] response.
   * - [crash]: Generate a [ErrorActionStrategy.Crash] response.
   */
  interface ErrorHandlerContext {
    /**
     * Original error event.
     */
    val event: ErrorEvent

    /**
     * Create an [ErrorActionStrategy] response which suppresses the error; optionally, a [warning] message can be
     * provided which is emitted to the log.
     *
     * @return Error action strategy.
     */
    fun suppress(warning: String? = null): ErrorActionStrategy.Suppress

    /**
     * Create an [ErrorActionStrategy] response which re-throws the error; optionally, an [override] exception may be
     * provided which wraps/overrides the error.
     *
     * @param override Throwable to throw instead.
     * @return Error action strategy.
     */
    fun rethrow(override: Throwable? = null): Rethrow

    /**
     * Create an [ErrorActionStrategy] response which crashes the VM; optionally, an override [exitCode] may be provided
     * in which case it is used instead of the error's normal translated exit code.
     *
     * @return Error action strategy.
     */
    fun crash(exitCode: Int? = null): Crash

    /**
     * Create an [ErrorActionStrategy] response which provides no specific error advice; optionally, a [warning] message
     * can be provided, which is logged.
     *
     * @param warning Warning message to show; logged if provided.
     * @return Error action strategy.
     */
    fun default(warning: String? = null): Default
  }

  /**
   * ## Error Handler: Context Factory
   *
   * Utilities for creating execution contexts which can be used with [ErrorHandler].
   */
  object Factory {
    /**
     * Create an error handler context.
     *
     * @param handler Handler which we are creating context for.
     * @param event Error event to create context for.
     * @return Execution context for the error handler.
     */
    @JvmStatic fun create(handler: ErrorHandler, event: ErrorEvent): ErrorHandlerContext {
      return object: ErrorHandlerContext {
        override val event: ErrorEvent get() = event

        override fun suppress(warning: String?): ErrorActionStrategy.Suppress = Suppress(
          event = event,
          logger = handler.logging,
          warning = warning,
        )

        override fun rethrow(override: Throwable?): Rethrow = Rethrow(
          event = event,
          logger = handler.logging,
          throwable = override ?: event.error,
        )

        override fun crash(exitCode: Int?): Crash = Crash(
          event = event,
          logger = handler.logging,
          exitCode = exitCode ?: 1,
        )

        override fun default(warning: String?): Default = Default(
          event = event,
          logger = handler.logging,
          warning = warning,
        )
      }
    }
  }

  /**
   * Logger to use for error reporting.
   */
  val logging: Logger

  /**
   * ## Handle Error: Background Exception
   *
   * Handles an [error] which arose during execution in a background [thread].
   *
   * @param thread Thread where this error took place.
   * @param error Error which took place.
   */
  override fun uncaughtException(thread: Thread, error: Throwable): Unit = handleError(
    error,
    context = ErrorContext.DEFAULT.copy(
      thread = thread,
    )
  ).invoke()  // take action unconditionally; thread caller won't invoke this

  /**
   * ## Handle Error: Process Exit
   *
   * Translate a fatal [exception] into a process exit code.
   *
   * @param exception Fatal exception which took place.
   * @return Exit code to use.
   */
  override fun getExitCode(exception: Throwable?): Int = when (exception) {
    // user code errors arising from the repl/shell/server
    is AbstractToolError -> {
      val exitCode = exception.exitCode
      val inner = exception.cause ?: exception.exception ?: exception
      logging.error("Execution failed with code $exitCode due to ${inner.message}")
      logging.error(
        StringBuilder().apply {
          append("Stacktrace:\n")
          append(exception.buildStacktrace())
        }.toString(),
      )
      exitCode
    }

    else -> {
      logging.error("Exiting with code -1 due to uncaught $exception")
      -1
    }
  }

  /**
   * ## Handle Error: Event
   *
   * This is the implementation entrypoint for the error handler; a uniform [ErrorEvent] is provided by the interface,
   * with a convenience function ([handleError]) which can materialize context.
   *
   * @param event Error event to process.
   * @return Advised error action strategy.
   */
  suspend fun ErrorHandlerContext.handleError(event: ErrorEvent): ErrorActionStrategy

  /**
   * ## Handle Error
   *
   * This is the main entrypoint for an Error Handler; the [error] is provided with optional [context] describing where
   * and why the error arose.
   *
   * @param error Error that was caught and is being handled.
   * @param context Optional additional context for this error.
   * @return Error followup strategy to apply.
   */
  fun handleError(error: Throwable, context: ErrorContext? = null): ErrorActionStrategy = runBlocking {
    ErrorEvent.of(error, context).let { event ->
      Factory.create(
        this@ErrorHandler,
        event,
      ).handleError(event)
    }
  }
}

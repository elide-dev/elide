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
package elide.tool.cli.cmd.dev

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.io.IOAccess
import org.graalvm.tools.lsp.instrument.EnvironmentProvider
import org.graalvm.tools.lsp.instrument.LSPInstrument
import org.graalvm.tools.lsp.server.ContextAwareExecutor
import org.graalvm.tools.lsp.server.LSPFileSystem
import org.graalvm.tools.lsp.server.LanguageServerImpl
import org.graalvm.tools.lsp.server.TruffleAdapter
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.io.PrintWriter
import java.lang.ref.WeakReference
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import jakarta.inject.Inject
import jakarta.inject.Provider
import kotlinx.coroutines.runBlocking
import kotlin.io.path.name
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.core.internals.graalvm.GraalVMContext
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.GuestLanguage
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState
import elide.tool.cli.options.LspOptions
import elide.tool.project.ProjectManager
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.ElideProject
import elide.tooling.project.load

private const val USE_BUILTIN_LSP = false

/** Starts an LSP for an Elide project. */
@Command(
  name = "lsp",
  description = ["Run an LSP instance for an Elide project."],
  mixinStandardHelpOptions = true,
  showDefaultValues = true,
  abbreviateSynopsis = true,
  usageHelpAutoWidth = true,
  synopsisHeading = "",
)
@Introspected
@ReflectiveAccess
internal class LspCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var projectManagerProvider: Provider<ProjectManager>

  private var waitForClose: Boolean = false
  private lateinit var activeContext: PolyglotContext
  private lateinit var executorWrapper: ContextAwareExecutor
  private lateinit var builder: PolyglotContextBuilder
  private lateinit var envProvider: EnvironmentProvider
  private lateinit var truffleAdapter: TruffleAdapter
  private lateinit var lspSocket: InetSocketAddress
  private lateinit var bindAddr: InetAddress
  private lateinit var serverSocket: ServerSocket
  private lateinit var info: PrintWriter
  private lateinit var err: PrintWriter
  private lateinit var delegates: List<org.graalvm.collections.Pair<String?, SocketAddress>>
  private lateinit var lsp: LanguageServerImpl

  /** File to run within the VM. */
  @Parameters(
    index = "0",
    arity = "0..1",
    paramLabel = "FILE|CODE",
    description = [
      "File to run to start the LSP; defaults to project entrypoint(s).",
    ],
  )
  internal var runnable: String? = null

  /** Settings which apply to LSP. */
  @CommandLine.ArgGroup(
    validate = false,
    heading = "%nLanguage Server:%n",
  )
  internal var lspOptions: LspOptions = LspOptions()

  @CommandLine.Option(
    names = ["--wait"],
    description = ["Wait for the LSP server to close before exiting."],
    defaultValue = "true",
  )
  internal var doWait: Boolean = true

  private class ContextAwareExecutorImpl private constructor (
    private val builder: PolyglotContextBuilder,
  ): ContextAwareExecutor {
    private var lastNestedContext: Context? = null
    private var workerThread: WeakReference<Thread> = WeakReference(null)

    private val executor: ExecutorService by lazy {
      val factory = Executors.defaultThreadFactory()

      Executors.newSingleThreadExecutor {
        factory.newThread(it).apply {
          name = WORKER_THREAD_ID
        }.also { workerThread = WeakReference(it) }
      }
    }

    companion object {
      private const val WORKER_THREAD_ID = "Elide LS Context-aware Worker"
      @JvmStatic fun create(builder: PolyglotContextBuilder): ContextAwareExecutorImpl {
        return ContextAwareExecutorImpl(builder)
      }
    }

    private fun <T> execute(callable: Callable<T>): Future<T> {
      if (Thread.currentThread() == this.workerThread.get()) {
        val futureTask = FutureTask(callable)
        futureTask.run()
        return futureTask
      }
      return executor.submit(callable)
    }

    private fun <T> wrapWithNewContext(taskWithResult: Callable<T>, cached: Boolean): Callable<T> {
      return Callable {
        val context: Context = if (!cached) builder.build() else {
          when (val nested = lastNestedContext) {
            null -> builder.build().also { lastNestedContext = it }
            else -> nested
          }
        }

        context.enter()
        try {
          try {
            return@Callable taskWithResult.call()
          } finally {
            context.leave()
          }
        } finally {
          if (!cached) {
            context.close()
          }
        }
      }
    }

    override fun <T> executeWithDefaultContext(taskWithResult: Callable<T>): Future<T> {
      return execute(taskWithResult)
    }

    override fun <T : Any> executeWithNestedContext(taskWithResult: Callable<T?>, cached: Boolean): Future<T?> {
      return execute(wrapWithNewContext(taskWithResult, cached))
    }

    @Suppress("TooGenericExceptionCaught")
    override fun <T : Any> executeWithNestedContext(
      taskWithResult: Callable<T?>,
      timeoutMillis: Int,
      onTimeoutTask: Callable<T?>,
    ): Future<T?> = if (timeoutMillis <= 0) {
      executeWithNestedContext(taskWithResult)
    } else execute(wrapWithNewContext(taskWithResult, false)).let { fut ->
      return try {
        CompletableFuture.completedFuture(fut.get(timeoutMillis.toLong(), TimeUnit.MILLISECONDS))
      } catch (_: TimeoutException) {
        fut.cancel(true)
        try {
          CompletableFuture.completedFuture(onTimeoutTask.call())
        } catch (e: Exception) {
          CompletableFuture<T>().apply { completeExceptionally(e) }
        }
      } catch (ixe: InterruptedException) {
        CompletableFuture<T>().apply { completeExceptionally(ixe) }
      }
    }

    override fun resetContextCache() {
      lastNestedContext?.close()
      lastNestedContext = builder.build()
    }

    override fun shutdown() {
      executor.shutdownNow()
    }
  }

  // Configure a polyglot context for LSP services.
  @Suppress("KotlinConstantConditions")
  private fun PolyglotContextBuilder.configureLSP(project: ElideConfiguredProject?, engine: Engine) {
    // apply general options
    allowAllAccess(true)
    allowExperimentalOptions(true)

    // use lsp fs
    builder = this
    executorWrapper = ContextAwareExecutorImpl.create(this)
    val lsp = engine.instruments[LSPInstrument.ID] ?: error("Failed to resolve LSP instrument")
    envProvider = lsp.lookup(EnvironmentProvider::class.java) ?: error("No environment provider found for LSP")
    truffleAdapter = TruffleAdapter(envProvider.environment, lspOptions.lspDebug)
    val ioAccess = IOAccess.newBuilder().fileSystem(LSPFileSystem.newReadOnlyFileSystem(truffleAdapter)).build()
    allowIO(ioAccess)
    if (!USE_BUILTIN_LSP) {
      waitForClose = true
    }

    // configure delegated second-order lsp servers
    lspOptions.allLanguageServerDelegates(project).ifEmpty { null }.let { delegates ->
      this@LspCommand.delegates = when (delegates) {
        null -> emptyList()
        else -> delegates.also {
          option("lsp.Delegates", it.joinToString(",") { delegate ->
            buildString {
              // language id if present
              delegate.first?.let { langId ->
                append(langId)
                append("@")
              }
              // then host string
              append(delegate.second)
            }
          })
        }.map { delegate ->
          val langId = delegate.first?.takeIf { it.isNotBlank() }
          val hostString = delegate.second.takeIf { it.isNotBlank() } ?: error(
            "Invalid LSP delegate host string: $delegate"
          )
          org.graalvm.collections.Pair.create(
            langId,
            InetSocketAddress.createUnresolved(hostString, requireNotNull(lspOptions.lspPort))
          )
        }
      }
    }
  }

  @Synchronized private fun notifyClose() {
    this.waitForClose = false
    (this as Object).notifyAll()
  }

  @Synchronized private fun waitForClose() {
    while(true) {
      if (this.waitForClose) {
        try {
          (this as Object).wait()
          continue
        } catch (_: InterruptedException) {
          // ignore
          Thread.interrupted()
        }
      }
      return
    }
  }

  @Suppress("MagicNumber", "TooGenericExceptionCaught")
  private suspend fun CommandContext.startLSPServer(project: ElideProject? = null) {
    System.out.writer(StandardCharsets.UTF_8).also { info = PrintWriter(it) }
    System.err.writer(StandardCharsets.UTF_8).also { err = PrintWriter(it) }

    val hostPortString = lspOptions.lspHostString()
    output {
      if (project != null) {
        val name = project.manifest.name ?: project.root.name
        append("Running LSP for project '$name' at $hostPortString")
      } else {
        append("Starting LSP at $hostPortString")
      }
    }
    executorWrapper.executeWithDefaultContext {
      val port = (hostPortString.substringAfter(':').toIntOrNull() ?: error(
        "Invalid port in LSP host string: $hostPortString"
      )).also {
        require(it > 0 && it < 65536) {
          "Port must be between 1 and 65535, got: $it"
        }
      }

      val context = builder.build()
      context.enter()

      try {
        truffleAdapter.register(envProvider.environment, executorWrapper)
        bindAddr = when {
          hostPortString.startsWith("127.0.0.1") -> InetAddress.getLoopbackAddress()
          else -> InetAddress.getByName(hostPortString.substringBefore(':'))
        }
        lspSocket = InetSocketAddress(
          bindAddr,
          port,
        )
        serverSocket = ServerSocket(port, -1, bindAddr)
        LanguageServerImpl.create(truffleAdapter, info, err).also { lsp = it }.apply {
          start(serverSocket, delegates).apply {
            thenRun {
              runCatching {
                executorWrapper.executeWithDefaultContext {
                  context.leave()
                }.get()
              }
              executorWrapper.shutdown()
              notifyClose()
            }.exceptionally { throwable ->
              throwable.printStackTrace(err)
              notifyClose()
              null
            }
          }
        }
      } catch (ixe: InterruptedException) {
        Thread.interrupted()
        executorWrapper.shutdown()
        notifyClose()
        throw ixe
      } catch (err: Exception) {
        err.printStackTrace(System.err)
        notifyClose()
        throw err
      }
    }
  }

  @Suppress("UNUSED_PARAMETER", "PrintStackTrace", "TooGenericExceptionCaught")
  private suspend fun CommandContext.lspEntry() {
    try {
      waitForClose = doWait
      startLSPServer()
      waitForClose()
    } catch (_: InterruptedException) {
      Thread.interrupted()
      lsp.shutdown().get(5, TimeUnit.SECONDS)
    } catch (err: Exception) {
      output {
        append("Failed to start LSP: ${err.message ?: err.javaClass.name}")
      }
      err.printStackTrace(System.err)
      notifyClose()
      throw err
    } finally {
      waitForClose()
    }
  }

  @Suppress("SpreadOperator")
  override fun withDeferredContext(
    langs: Set<GuestLanguage>,
    cfg: PolyglotContextBuilder.(Engine) -> Unit,
    shared: Boolean,
    detached: Boolean,
    block: (() -> PolyglotContext) -> Unit,
  ) {
    val engine = Engine.newBuilder().apply {
      allowExperimentalOptions(true)
      if (USE_BUILTIN_LSP) {
        option("lsp", lspOptions.lspHostString())
      }
    }.build()

    builder = with(Context.newBuilder(*langs.map { it.id }.toTypedArray())) {
      engine(engine)
      cfg(this, engine)
      this
    }

    block.invoke {
      GraalVMContext(builder.build())
    }
  }

  @Suppress("KotlinConstantConditions")
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val project = projectManagerProvider.get().resolveProject(projectOptions().projectPath())
    if (project == null) {
      if (runnable == null) {
        // nothing to start
        return err("No LSP start entrypoint (via project or files).")
      }
    }
    val configured = project?.load()

    withDeferredContext(emptySet(), shared = false, cfg = { configureLSP(configured, it) }) { accessor ->
      runBlocking {
        // warm it before use (two calls are deliberate)
        accessor()
        activeContext = accessor()
        if (!USE_BUILTIN_LSP) {
          lspEntry()
        }
      }
    }
    return success()
  }
}

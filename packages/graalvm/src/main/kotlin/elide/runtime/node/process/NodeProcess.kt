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
@file:OptIn(DelicateElideApi::class)
@file:Suppress("MnInjectionPoints")

package elide.runtime.node.process

import org.graalvm.nativeimage.ImageInfo
import org.graalvm.nativeimage.ProcessProperties
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.util.concurrent.atomic.AtomicReference
import jakarta.inject.Provider
import kotlin.collections.Map.Entry
import kotlin.system.exitProcess
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.ProcessManager
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.ProcessAPI
import elide.runtime.intrinsics.js.node.process.*
import elide.runtime.lang.javascript.NodeModuleName
import elide.runtime.plugins.env.EnvConfig
import elide.runtime.plugins.env.Environment
import elide.runtime.plugins.js.JavaScript
import elide.vm.annotations.Polyglot

// Installs the Node process module into the intrinsic bindings.
@Intrinsic(NodeProcess.PUBLIC_SYMBOL, internal = false)
@Singleton internal class NodeProcessModule @Inject constructor(
  private val envConfigProvider: Provider<EnvConfig?> = Provider { null },
) : AbstractNodeBuiltinModule() {
  private fun envConfig(): EnvConfig? = envConfigProvider.get()

  fun provide(): ProcessAPI = envConfig().let { envConfig ->
    if (envConfig == null) NodeProcess.obtain() else NodeProcess.create(
      env = EnvAccessor.of(envConfig),
    )
  }

  private val singleton by lazy { provide() }

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[NodeProcess.PUBLIC_SYMBOL.asPublicJsSymbol()] = singleton
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.PROCESS)) { singleton }
  }
}

// Implements standard `process` module logic which applies regardless of isolation settings.
internal abstract class NodeProcessBaseline : ProcessAPI {
  override fun nextTick(callback: (args: Array<Any>) -> Unit, vararg args: Any) {
    // nothing (not implemented)
  }
}

/**
 * # Environment Accessor
 *
 * Provides a mechanism for accessing and modifying the environment variables of the host process; certain restrictions
 * may apply.
 */
internal interface EnvAccessor {
  /**
   * ## All Environment Variables
   *
   * Retrieve all environment variables available to the guest program.
   *
   * @return Map of environment variable names to their values.
   */
  fun all(): Map<String, String>

  /**
   * ## Get Environment Variable
   *
   * Retrieve the value of a specific environment variable.
   *
   * @param key Name of the environment variable to retrieve.
   * @return Value of the environment variable, or `null` if the variable is not set.
   */
  operator fun get(key: String): String?

  /**
   * ## Set Environment Variable
   *
   * Set the value of a specific environment variable.
   *
   * @param key Name of the environment variable to set.
   * @param value Value to set the environment variable to.
   */
  operator fun set(key: String, value: String)

  /**
   * ## Remove Environment Variable
   *
   * Remove a specific environment variable.
   *
   * @param key Name of the environment variable to remove.
   */
  fun remove(key: String)

  companion object {
    private val EMPTY_ENV = fromMap(mutableMapOf())

    /**
     * ## From Map
     *
     * Create an environment accessor from a mutable map.
     *
     * @param map Map to use as the backing store for the accessor.
     * @return Environment accessor backed by the provided map.
     */
    @JvmStatic fun fromMap(map: MutableMap<String, String>): EnvAccessor = object : EnvAccessor {
      override fun all(): Map<String, String> = map
      override fun get(key: String): String? = map[key]
      override fun set(key: String, value: String) = error("Elide forbids writes to environment variables")
      override fun remove(key: String) = error("Elide forbids writes to environment variables")
    }

    /**
     * ## Empty
     *
     * Create an empty environment accessor.
     *
     * @return Empty environment accessor.
     */
    @JvmStatic fun empty(): EnvAccessor = EMPTY_ENV

    /**
     * ## From Configuration
     *
     * Create an environment accessor from an environment configuration.
     *
     * @param config Environment configuration to use.
     * @return Environment accessor backed by the provided configuration.
     */
    @JvmStatic fun of(config: EnvConfig): EnvAccessor = object : EnvAccessor {
      override fun all(): Map<String, String> = config
        .app
        .isolatedEnvironmentVariables
        .filterValues { it.isPresent }
        .mapValues { requireNotNull(it.value.value) }

      override fun get(key: String): String? = config
        .app
        .isolatedEnvironmentVariables[key]?.value

      override fun set(key: String, value: String) = error("Elide forbids writes to environment variables")
      override fun remove(key: String) = error("Elide forbids writes to environment variables")
    }
  }
}

/**
 * # VM Exit Handler
 *
 * Provides a mechanism for handling VM exit requests; this may include halting the VM, returning control to the host,
 * or other actions.
 */
@FunctionalInterface internal fun interface VmExitHandler {
  /**
   * ## Exit VM
   *
   * Exit the VM, returning control to the host environment.
   *
   * @param code Exit code to use.
   */
  fun exit(code: Int)
}

/**
 * # Process Arguments Accessor
 *
 * Provides a mechanism for accessing the command-line arguments of the host process.
 */
@FunctionalInterface internal fun interface ArgvAccessor {
  /**
   * ## Command-Line Arguments
   *
   * Retrieve the command-line arguments of the host process.
   *
   * @return List of command-line arguments.
   */
  fun all(): Array<String>
}

/**
 * # Working Directory Accessor
 *
 * Provides a mechanism for accessing the current working directory.
 */
@FunctionalInterface internal fun interface CwdAccessor {
  /**
   * ## Current Working Directory
   *
   * Retrieve the current working directory of the host process.
   *
   * @return Current working directory.
   */
  fun cwd(): String
}

/**
 * # Process ID Accessor
 *
 * Provides a mechanism for accessing the current process ID.
 */
@FunctionalInterface internal fun interface PidAccessor {
  /**
   * ## Current Process ID
   *
   * Retrieve the ID of the currently-running process.
   *
   * @return Current process ID.
   */
  fun pid(): Long
}

// Mediate access to an environment accessor for the Node process module.
private class EnvironmentAccessMediator(private val access: EnvAccessor) : ProcessEnvironmentAPI {
  override val entries: Set<Entry<String, String>> get() = all().entries
  override val keys: Set<String> get() = all().keys
  override val size: Int get() = all().size
  override val values: Collection<String> get() = all().values

  override fun all(): Map<String, String> = access.all()
  override fun get(key: String): String? = access[key]
  override fun containsKey(key: String): Boolean = access[key] != null
  override fun containsValue(value: String): Boolean = all().values.contains(value)
  override fun isEmpty(): Boolean = all().isEmpty()
}

/**
 * # Node Process API
 */
internal object NodeProcess {
  internal const val SYMBOL: String = "node_process"
  internal const val PUBLIC_SYMBOL: String = "process"

  // Implements the Node process module for a host environment.
  internal class NodeProcessModuleImpl internal constructor(
    private val activePlatform: ProcessPlatform,
    private val activeArch: ProcessArch,
    private val argvMediator: ArgvAccessor,
    private val envApi: EnvAccessor,
    private val exiter: VmExitHandler,
    private val cwdAccessor: CwdAccessor,
    private val pidAccessor: PidAccessor,
  ) : ProcessAPI, NodeProcessBaseline() {
    private companion object {
      private const val STDOUT_FD = 1
      private const val STDERR_FD = 2
      private const val STDIN_FD = 0
      private const val ELIDE = "elide"
      private val BOOT_PROGRAM_NAME = when {
        ImageInfo.inImageBuildtimeCode() -> ELIDE
        ImageInfo.isSharedLibrary() -> ELIDE
        else -> if (!ImageInfo.inImageCode()) ELIDE else {
          ProcessProperties.getArgumentVectorProgramName() ?: ELIDE  // probably running on JVM
        }
      }
    }

    // Process title/program name override.
    private val programNameOverride: AtomicReference<String> = AtomicReference()

    @get:Polyglot override val env: PolyglotValue get() = Environment.forLanguage(JavaScript, Context.getCurrent())

    @get:Polyglot override val argv: Array<String> get() = argvMediator.all()
    @get:Polyglot override val pid: Long get() = pidAccessor.pid()
    @get:Polyglot override val arch: String get() = activeArch.symbol
    @get:Polyglot override val platform: String get() = activePlatform.symbol

    @get:Polyglot @set:Polyglot override var title: String?
      get() = programNameOverride.get() ?: BOOT_PROGRAM_NAME
      set(value) {
        require(!value.isNullOrEmpty() && value.isNotBlank()) {
          "Program name must be a non-empty, non-blank string"
        }
        try {
          programNameOverride.set(value)
          if (ImageInfo.inImageRuntimeCode()) {
            ProcessProperties.setArgumentVectorProgramName(value)
          }
        } catch (_: UnsupportedOperationException) {
          // no-op (swallow)
        }
      }

    @Polyglot override fun cwd(): String = cwdAccessor.cwd()
    @Polyglot override fun exit(code: Int?) = exiter.exit(code ?: 0)
    @Polyglot override fun exit() = exiter.exit(0)

    @Polyglot override fun exit(code: Value?) {
      when {
        code == null || code.isNull -> exiter.exit(0)
        code.isNumber && code.fitsInInt() -> exiter.exit(code.asInt())
        else -> error("Cannot exit with value '$code'")
      }
    }

    @get:Polyglot override val stdout: ProcessStandardOutputStream
      get() = ProcessStandardOutputStream.wrap(STDOUT_FD, System.out)

    @get:Polyglot override val stderr: ProcessStandardOutputStream
      get() = ProcessStandardOutputStream.wrap(STDERR_FD, System.err)

    @get:Polyglot override val stdin: ProcessStandardInputStream
      get() = ProcessStandardInputStream.wrap(STDIN_FD, System.`in`)

    override fun getMember(key: String): Any? = when (key) {
      "env" -> env
      "argv" -> argv
      "stdout" -> stdout
      "stderr" -> stderr
      "stdin" -> stdin
      "pid" -> pid
      "arch" -> arch
      "platform" -> platform
      "title" -> title
      "cwd" -> ProxyExecutable { cwd() }
      "exit" -> ProxyExecutable { exit(it.getOrNull(0)) }
      else -> null
    }
  }

  // Resolve the currently active operating system (platform) value.
  @JvmStatic private fun resolveCurrentPlatform(): ProcessPlatform = ProcessPlatform.host()

  // Resolve the currently active CPU architecture value.
  @JvmStatic private fun resolveCurrentArchitecture(): ProcessArch = ProcessArch.host()

  // Host-side process implementation.
  @JvmStatic internal val stubbed by lazy {
    NodeProcessModuleImpl(
      resolveCurrentPlatform(),
      resolveCurrentArchitecture(),
      { emptyArray<String>() },
      EnvAccessor.empty(),
      { },
      { "" },
      { -1 },
    )
  }

  // Host-side process implementation.
  @JvmStatic internal val host by lazy {
    ProcessManager.acquire().let { mgr ->
      NodeProcessModuleImpl(
        resolveCurrentPlatform(),
        resolveCurrentArchitecture(),
        { mgr.arguments() },
        EnvAccessor.fromMap(System.getenv()),
        { exitProcess(it) },
        { mgr.workingDirectory().ifBlank { null } ?: System.getProperty("user.dir") },
        { ProcessHandle.current().pid() },
      )
    }
  }

  /**
   * Obtain an instance of the Node Process API.
   *
   * @return Node Process API instance.
   */
  @JvmStatic fun obtain(allow: Boolean = true): ProcessAPI {
    return if (allow) host else stubbed
  }

  /**
   * Create a new instance of the Node Process API.
   *
   * @param argv Command-line arguments accessor.
   * @param env Environment accessor.
   * @param exiter VM exit handler.
   * @param cwd Current working directory accessor.
   * @param pid Process ID accessor.
   * @return Node Process API instance.
   */
  @JvmStatic fun create(
    argv: ArgvAccessor = ArgvAccessor { emptyArray<String>() },
    env: EnvAccessor = EnvAccessor.empty(),
    exiter: VmExitHandler = VmExitHandler { exitProcess(it) },
    cwd: CwdAccessor = CwdAccessor { System.getProperty("user.dir") },
    pid: PidAccessor = PidAccessor { ProcessHandle.current().pid() },
  ): ProcessAPI = NodeProcessModuleImpl(
    resolveCurrentPlatform(),
    resolveCurrentArchitecture(),
    argv,
    env,
    exiter,
    cwd,
    pid,
  )
}

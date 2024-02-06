/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package elide.embedded.impl

import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.server.EmbeddedServer
import org.graalvm.nativeimage.LogHandler
import org.graalvm.nativeimage.c.type.CCharPointer
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.SandboxPolicy.*
import org.graalvm.word.UnsignedWord
import tools.elide.call.VMConfiguration.VMFlag
import tools.elide.call.VMConfiguration.VMFlag.ValueCase.BOOL
import tools.elide.meta.GuestLanguage
import java.util.TreeMap
import java.util.TreeSet
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Handler
import java.util.logging.LogRecord
import kotlinx.atomicfu.atomic
import elide.annotations.Context
import elide.annotations.Eager
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.embedded.Streams
import elide.embedded.api.*
import elide.embedded.api.Constants.Flag
import elide.embedded.api.Constants.Defaults.Engine as EngineDefaults
import elide.embedded.api.EmbeddedRuntime.EmbeddedDispatcher

/**
 * # Embedded Runtime: V1 Implementation
 *
 * Implements access to an [EmbeddedRuntime] interface powered by Elide. Host applications are expected to initialize,
 * configure, and start the runtime, at which point applications can be installed, and requests can be dispatched to
 * them through this API.
 */
@Singleton @Context @Eager public class EmbeddedRuntimeImpl @Inject constructor (
  private val hostConfig: InstanceConfiguration,
) : EmbeddedRuntime {
  private companion object {
    // Default activated languages.
    private val defaultLanguages = listOf(
      GuestLanguage.JAVASCRIPT,
      GuestLanguage.WASM,
    )

    // Default suite of flags.
    private val defaultEngineFlags = listOf(
      // `engine.MaxIsolateMemory`
      vmFlag(Flag.MAX_ISOLATE_MEMORY, EngineDefaults.MAX_ISOLATE_MEMORY),

      // `engine.Compilation`
      vmFlag(Flag.COMPILATION, EngineDefaults.COMPILATION),

      // `engine.SpawnIsolate`
      vmFlag(Flag.SPAWN_ISOLATE, EngineDefaults.SPAWN_ISOLATE),

      // `engine.UntrustedCodeMitigation`
      vmFlag(Flag.UNTRUSTED_CODE_MITIGATION, EngineDefaults.UNTRUSTED_CODE_MITIGATION),
    )

    @JvmStatic private fun vmFlag(name: String, value: String): VMFlag =
      VMFlag.newBuilder().setFlag(name).setString(value).build()

    @JvmStatic private fun vmFlag(name: String, value: Boolean): VMFlag =
      VMFlag.newBuilder().setFlag(name).setBool(value).build()

    @JvmStatic private fun engineFlag(vmFlag: VMFlag): Pair<String, String> {
      return vmFlag.flag to when (vmFlag.valueCase) {
        BOOL -> vmFlag.bool.toString()
        else -> vmFlag.string
      }
    }

    @JvmStatic private fun flagsWithDefaults(
      defaults: List<VMFlag>,
      overrides: List<VMFlag>? = null,
    ): List<Pair<String, String>> {
      val flags = TreeMap<String, String>()
      defaults.plus(overrides ?: emptyList()).map(::engineFlag).forEach {
        flags[it.first] = it.second
      }
      return flags.toList()
    }
  }

  // Manages instance configuration and defaults.
  private inner class ConfigurationManager {
    // Active configuration installed by the host application.
    private val activeConfig: AtomicReference<InstanceConfiguration> = AtomicReference(hostConfig)

    /**
     *
     */
    private fun obtain(): InstanceConfiguration {
      require(activeConfig.get() != null) {
        "Embedded runtime is not configured"
      }
      return activeConfig.get()
    }

    /** Active configuration. */
    val active: InstanceConfiguration get() = obtain()
  }

  // Whether the embedded runtime has completed the initialization step.
  private val initialized = atomic(false)

  // Whether the embedded runtime is currently running.
  private val running = atomic(false)

  // Set of capabilities declared by the host application.
  private val capabilities = TreeSet<Capability>()

  // Configuration manager.
  private val configManager = ConfigurationManager()

  // Active application context.
  @Inject private lateinit var ctx: ApplicationContext

  // Active embedded server engine.
  @Inject private lateinit var server: EmbeddedServer

  // Logging handler.
  private val engineLogHandler: Handler = object: Handler() {
    override fun publish(record: LogRecord?) {
      TODO("Not yet implemented")
    }

    override fun flush() {
      TODO("Not yet implemented")
    }

    override fun close() {
      TODO("Not yet implemented")
    }
  }

  // Active GraalVM engine.
  private val engine: Engine = Engine.newBuilder(*enabledLanguages()).apply {
    val config = configManager.active

    // system streams, or delegated streams
    `in`(Streams.stub.stdin)
    out(Streams.stub.stdout)
    err(Streams.stub.stderr)

    // begin adding engine options
    allowExperimentalOptions(true)
    useSystemProperties(false)

    // isolated sandbox by default
    sandbox(TRUSTED)
    logHandler(engineLogHandler)

    // apply flags for host engine
    flagsWithDefaults(defaultEngineFlags, config.engine.hvm.flagList).forEach {
      option(it.first, it.second)
    }
  }.build()

  override val isConfigured: Boolean get() = true
  override val isRunning: Boolean get() = running.value

  private fun GuestLanguage.toSymbol(): String = when (this) {
    GuestLanguage.JAVASCRIPT -> "js"
    GuestLanguage.PYTHON -> "python"
    GuestLanguage.RUBY -> "ruby"
    GuestLanguage.WASM -> "wasm"
    else -> error("Cannot resolve language symbol: '$name'")
  }

  private fun enabledLanguages(): Array<String> = configManager.active.engine.languageList.let {
    it.ifEmpty { defaultLanguages }
  }.map {
    it.toSymbol()
  }.toTypedArray()

  private inline fun <R> requireConfigured(crossinline op: () -> R): R {
    require(isConfigured) {
      "Embedded runtime is not configured"
    }
    return op.invoke()
  }

  private inline fun <R> requireNotConfigured(crossinline op: () -> R): R {
    require(!isConfigured) {
      "Embedded runtime is already configured"
    }
    return op.invoke()
  }

  private inline fun <R> withActive(crossinline op: () -> R): R {
    require(running.value) {
      "Embedded runtime is not running"
    }
    return op.invoke()
  }

  override fun initialize() {
    require(!initialized.value) {
      "Cannot initialize embedded runtime more than once"
    }
    initialized.value = true
  }

  override fun enable(capability: Capability): Unit = requireNotConfigured {
    capabilities.add(capability)
  }

  override fun start(): Unit = requireConfigured {
    running.value = true
  }

  override fun notify(capabilities: Set<Capability>) {
    capabilities.forEach {
      enable(it)
    }
    start()
  }

  override fun dispatcher(): EmbeddedDispatcher = withActive {
    object : EmbeddedDispatcher {
      override suspend fun handle(call: UnaryNativeCall) {
        TODO("Not yet implemented")
      }
    }
  }
}

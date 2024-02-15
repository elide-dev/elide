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

@file:Suppress("FunctionOnlyReturningConstant")

package elide.embedded

import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.Micronaut
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.graalvm.nativeimage.ImageInfo
import org.slf4j.bridge.SLF4JBridgeHandler
import tools.elide.call.HostConfiguration
import tools.elide.call.v1alpha1.UnaryInvocationResponse
import java.security.Security
import java.util.SortedSet
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess
import elide.annotations.Eager
import elide.embedded.api.*
import elide.embedded.err.InitializationError

/**
 * # Elide: Embedded
 *
 * Entrypoint for embedded use of the Elide runtime; in this mode, a host runtime is created and managed as a thin layer
 * underneath one or more applications. Each application is configured independently and managed via cooperation between
 * two APIs: the Host Control API and Host Call API.
 *
 * &nbsp;
 *
 * ## API Interfaces
 *
 * This engine provides API interfaces to facilitate control of running applications, and to invoke user code. See below
 * for a description of provided APIs.
 *
 * &nbsp;
 *
 * ### Host Control API
 *
 * The Host Control API facilitates management of running Elide apps on an embedded instance. Instance lifecycle and
 * deployment of container apps is up to the hosting application.
 *
 * &nbsp;
 *
 * ### Host Call API
 *
 * The Host Call API provides remote invocation facilities for applications hosted by an embedded instance of Elide.
 * Calls are submitted to endpoints (TCP, Unix socket, etc.), and routed to the appropriate application. Methods are
 * provided in the Host Control API for resolving an application ID.
 */
public class ElideEmbedded private constructor () {
  public companion object {
    @JvmStatic public fun main(args: Array<String>) {
      ElideEmbedded().entry(args)
      exitProcess(0)
    }

    /** Static initialization, during entrypoint boot. */
    @JvmStatic private fun initializeStatic() {
      listOf(
        "elide.js.vm.enableStreams" to "true",
        "io.netty.allocator.maxOrder" to "3",
        "io.netty.serviceThreadPrefix" to "elide-svc",
        "io.netty.native.deleteLibAfterLoading" to "true",  // reversed bc of bug (actually does not delete)
        "io.netty.buffer.bytebuf.checkAccessible" to "false",
        org.fusesource.jansi.AnsiConsole.JANSI_MODE to org.fusesource.jansi.AnsiConsole.JANSI_MODE_FORCE,
        org.fusesource.jansi.AnsiConsole.JANSI_GRACEFUL to "false",
      ).forEach {
        System.setProperty(it.first, it.second)
      }

      Security.insertProviderAt(BouncyCastleProvider(), 0)
      SLF4JBridgeHandler.removeHandlersForRootLogger()
      SLF4JBridgeHandler.install()
    }

    /** @return Empty un-initialized instance. */
    @JvmStatic public fun create(): ElideEmbedded = ElideEmbedded()
  }

  // Active API version.
  private val activeApiVersion: AtomicRef<String> = atomic(Constants.API_VERSION)

  // Active protocol mode.
  private val activeProtocolMode: AtomicRef<ProtocolMode> = atomic(ProtocolMode.PROTOBUF)

  // Active instance configuration.
  private val activeInstanceConfig: AtomicRef<InstanceConfiguration?> = atomic(null)

  // Active application context.
  private val activeApplicationContext: AtomicRef<ApplicationContext?> = atomic(null)

  // Active embedded implementation.
  private val activeImpl: AtomicRef<EmbeddedRuntime?> = atomic(null)

  // Active instance configuration.
  private val declaredCapabilities: SortedSet<Capability> = sortedSetOf(Capability.BASELINE)

  /**
   * ## Code Entrypoint
   *
   * This entrypoint immediately takes over for [main], and is used as the entrypoint for JVM-based testing, etc.
   *
   * This method will not exit the VM.
   *
   * @param args Command-line arguments.
   */
  public fun entry(args: Array<String>): Int {
    initialize(ProtocolMode.PROTOBUF)
    capability(Capability.BASELINE)
    configure(Constants.API_VERSION, null)

    try {
      start(args.toList())
    } catch (ixe: InterruptedException) {
      println("Interrupted: ${ixe.message}")
      Thread.interrupted()
      stop()
    } catch (e: Exception) {
      println("Error: ${e.message}")
    } finally {
      teardown()
    }
    return 0
  }

  /**
   * ## Native: Initialization
   *
   * Initialize native integration between a host application and embedded Elide. The provided thread is expected to be
   * an initialized GraalVM isolate thread, which should be held for the lifetime of the outer host application.
   *
   * ### API Versioning
   *
   * At the time of this writing, the expected API version is `v1alpha`; this is the only version of the API.
   *
   * @param protocol Describes the protocol operating mode that will be used to dispatch calls.
   * @return Integer indicating success or error; `0` for success, non-zero for an error. For a guide of available error
   *   codes during initialization, see the [InitializationError] enum.
   */
  public fun initialize(protocol: ProtocolMode): Int {
    initializeStatic()
    activeProtocolMode.value = protocol
    return 0
  }

  /**
   * ## Native: Capabilities
   *
   * Initialize individual native capabilities by declaring support for them in the host application; if Elide likewise
   * supports the capability at this version and in this operating context, the capability will be enabled, and a return
   * code of `0` is provided.
   *
   * Any other return code indicates an [InitializationError] of some kind.
   *
   * @param capability Capability to initialize.
   * @return Integer indicating success or error; `0` for success, non-zero for an error. For a guide of available error
   *   codes during initialization, see the [InitializationError] enum.
   */
  public fun capability(capability: Capability): Int {
    declaredCapabilities.add(capability)
    return 0
  }

  /**
   * ## Native: Configuration
   *
   * TBD.
   *
   * @return Integer indicating success or error; `0` for success, non-zero for an error. For a guide of available error
   *   codes during initialization, see the [InitializationError] enum.
   */
  public fun configure(version: String, config: InstanceConfiguration? = null): Int {
    assert (version == Constants.API_VERSION) { "Unsupported API version: $version" }
    activeApiVersion.value = version
    activeInstanceConfig.value = config ?: InstanceConfiguration.createFrom(HostConfiguration.getDefaultInstance())
    return 0
  }

  /**
   * ## Native: Start
   *
   * TBD.
   *
   * @return Integer indicating success or error; `0` for success, non-zero for an error. For a guide of available error
   *   codes during initialization, see the [InitializationError] enum.
   */
  @Suppress("SpreadOperator")
  public fun start(args: List<String>? = null): Int {
    require(activeApiVersion.value.isNotBlank()) { "API version must be set before starting the instance" }
    val configuration = requireNotNull(activeInstanceConfig.value) { "Configuration must be set before starting" }
    require(activeApplicationContext.value == null && activeImpl.value == null) { "Instance is already running" }

    try {
      val applicationContext = Micronaut.build(*(args?.toTypedArray() ?: emptyArray<String>())).apply {
        banner(false)
        bootstrapEnvironment(true)
        deduceEnvironment(false)
        eagerInitSingletons(true)
        eagerInitAnnotated(Eager::class.java)
        enableDefaultPropertySources(false)
        singletons(configuration)
        environments("embedded", if (ImageInfo.inImageCode()) "native" else "jvm")
      }.build()

      // mount
      applicationContext.start()
      val impl = applicationContext.getBean(EmbeddedRuntime::class.java)
      activeApplicationContext.value = applicationContext
      activeImpl.value = impl
      impl.notify(declaredCapabilities)
    } catch (err: Throwable) {
      println("Error in `start` (${err::class.java.simpleName}): ${err.message}")
      throw err
    }
    return 0
  }

  /**
   * ## Native: Entry Dispatch
   */
  public fun enterDispatch(call: UnaryNativeCall, ticket: InFlightCallInfo): Int = ticket.use {
    requireNotNull(activeImpl.value) { "Instance is already running" }.let {
      runBlocking {
        Dispatchers.Default.invoke {
          it.dispatcher().handle(call)
        }
        0
      }
    }
  }

  /**
   * ## Native: Call Cancellation
   */
  public fun dispatchCancel(handle: InFlightCallInfo): Int {
    return 0
  }

  /**
   * ## Native: Call Polling
   */
  public fun dispatchPoll(handle: InFlightCallInfo): Int {
    return 0
  }

  /**
   * ## Native: Gather Response
   */
  public fun response(call: UnaryNativeCall, ticket: InFlightCallInfo): UnaryInvocationResponse {
    return UnaryInvocationResponse.getDefaultInstance()  // not yet implemented
  }

  /**
   * ## Native: Exit Dispatch
   */
  public fun exitDispatch(call: UnaryNativeCall, ticket: InFlightCallInfo): Int {
    return 0
  }

  /**
   * ## Native: Stop
   *
   * TBD.
   *
   * @return Integer indicating success or error; `0` for success, non-zero for an error. For a guide of available error
   *   codes during initialization, see the [InitializationError] enum.
   */
  public fun stop(): Int {
    // nothing
    return 0
  }

  /**
   * ## Native: Exit Dispatch
   */
  public fun teardown(): Int {
    return 0
  }
}

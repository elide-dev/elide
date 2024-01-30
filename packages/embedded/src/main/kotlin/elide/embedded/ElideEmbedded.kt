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

package elide.embedded

import elide.embedded.api.*
import org.graalvm.nativeimage.IsolateThread
import kotlin.system.exitProcess

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
public object ElideEmbedded {
  /**
   * ## Main Entrypoint
   *
   * When dispatched as an executable, this entrypoint takes over execution; executable use of Elide Embedded is used
   * for testing and development purposes.
   *
   * This method will exit with a descriptive code.
   *
   * @param args Command-line arguments.
   */
  @JvmStatic public fun main(args: Array<String>): Unit = exitProcess(entry(args))

  /**
   * ## Code Entrypoint
   *
   * This entrypoint immediately takes over for [main], and is used as the entrypoint for JVM-based testing, etc.
   *
   * This method will not exit the VM.
   *
   * @param args Command-line arguments.
   */
  @JvmStatic public fun entry(args: Array<String>): Int {
    println("Hello from Elide Embedded")
    return 0
  }

  /**
   * ## Native: Initialization
   *
   * Initialize native integration between a host application and embedded Elide. The provided [thread] is expected to
   * be an initialized GraalVM isolate thread, which should be held for the lifetime of the outer host application.
   *
   * ### API Versioning
   *
   * At the time of this writing, the expected API version is `v1alpha`; this is the only version of the API.
   *
   * @param protocol Describes the protocol operating mode that will be used to dispatch calls.
   * @return Integer indicating success or error; `0` for success, non-zero for an error. For a guide of available error
   *   codes during initialization, see the [InitializationError] enum.
   */
  @JvmStatic public fun initialize(protocol: ProtocolMode): Int {
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
  @JvmStatic public fun capability(capability: Capability): Int {
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
  @JvmStatic public fun configure(version: String, config: InstanceConfiguration?): Int {
    // nothing
    return 0
  }

  /**
   * ## Native: Entry Dispatch
   */
  @JvmStatic public fun enterDispatch(call: NativeCall, ticket: InFlightCallInfo): Int {
    return 0
  }

  /**
   * ## Native: Call Cancellation
   */
  @JvmStatic public fun dispatchCancel(handle: InFlightCallInfo): Int {
    return 0
  }

  /**
   * ## Native: Call Polling
   */
  @JvmStatic public fun dispatchPoll(handle: InFlightCallInfo): Int {
    return 0
  }

  /**
   * ## Native: Exit Dispatch
   */
  @JvmStatic public fun exitDispatch(call: NativeCall, ticket: InFlightCallInfo): Int {
    return 0
  }

  /**
   * ## Native: Exit Dispatch
   */
  @JvmStatic public fun teardown(): Int {
    return 0
  }
}

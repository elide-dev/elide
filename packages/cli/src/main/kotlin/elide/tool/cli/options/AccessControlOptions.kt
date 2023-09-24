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

package elide.tool.cli.options

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine.Option
import kotlinx.serialization.Serializable
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.core.PolyglotEngineConfiguration.HostAccess.*

/**
 * TBD.
 */
@Introspected @ReflectiveAccess class AccessControlOptions : RunnableOptionsMixin {
  private companion object {
    private const val DEFAULT_ALLOW_ALL = false
    private const val DEFAULT_ALLOW_IO = false
    private const val DEFAULT_ALLOW_ENV = false
    private const val DEFAULT_ALLOW_NATIVE = true
    private const val DEFAULT_ALLOW_THREADS = false
    private const val DEFAULT_ALLOW_PROC = false
    private const val DEFAULT_ISOLATES = false
  }

  /** Sandbox and isolation settings. */
  @Serializable @JvmRecord data class SandboxSettings (
    /** Whether to run guest code in isolates, where supported. */
    val isolates: Boolean = DEFAULT_ISOLATES,
  ) {
    companion object {
      /** Defaults for sandbox and isolation settings. */
      val DEFAULTS = SandboxSettings()
    }
  }

  /** Host access settings. */
  @Serializable @JvmRecord data class HostAccessSettings (
    /** Whether to allow all host access. */
    val allowAll: Boolean = DEFAULT_ALLOW_ALL,

    /** Whether to allow host I/O access. */
    val allowIo: Boolean = DEFAULT_ALLOW_IO,

    /** Whether to allow host environment access. */
    val allowEnv: Boolean = DEFAULT_ALLOW_ENV,

    /** Whether to allow native access. */
    val allowNative: Boolean = DEFAULT_ALLOW_NATIVE,

    /** Whether to allow guests to launch threads. */
    val allowThreads: Boolean = DEFAULT_ALLOW_THREADS,

    /** Whether to allow guests to launch processes. */
    val allowProc: Boolean = DEFAULT_ALLOW_PROC,
  ) {
    companion object {
      /** Defaults for host access. */
      val DEFAULTS = HostAccessSettings()
    }
  }

  /** Combined access control settings. */
  @Serializable @JvmRecord data class AccessControlSettings(
    val sandbox: SandboxSettings,
    val hostAccess: HostAccessSettings,
  ) {
    companion object {
      /** Defaults for combined access control settings. */
      val DEFAULTS = AccessControlSettings(
        sandbox = SandboxSettings.DEFAULTS,
        hostAccess = HostAccessSettings.DEFAULTS,
      )
    }
  }

  /** Whether to run guest code in isolates. */
  @Option(
    names = ["--sandbox:isolate"],
    description = ["Whether to run guest code in isolates, where supported."],
    defaultValue = "false",
  )
  internal var enableIsolates: Boolean = DEFAULT_ISOLATES

  /** Whether to allow all host access. */
  @Option(
    names = ["--host:allow-all"],
    description = ["Whether to allow host access. Careful, this can be dangerous!"],
    defaultValue = "false",
  )
  internal var allowAll: Boolean = DEFAULT_ALLOW_ALL

  /** Whether to allow host I/O access. */
  @Option(
    names = ["--host:allow-io"],
    description = ["Allows I/O access to the host from guest VMs (Experimental)"],
    defaultValue = "false",
  )
  internal var allowIo: Boolean = DEFAULT_ALLOW_IO

  /** Whether to allow host environment access. */
  @Option(
    names = ["--host:allow-env"],
    description = ["Allows environment access to the host from guest VMs (Experimental)"],
    defaultValue = "false",
  )
  internal var allowEnv: Boolean = DEFAULT_ALLOW_ENV

  /** Apply these settings to the root engine configuration container. */
  internal fun apply(config: PolyglotEngineConfiguration) {
    config.hostAccess = when {
      allowAll || (allowIo && allowEnv) -> ALLOW_ALL
      allowIo -> ALLOW_IO
      allowEnv -> ALLOW_ENV
      else -> ALLOW_NONE
    }
  }

  /** @return Mixin options as a set of [HostAccessSettings]. */
  fun toSettings(): AccessControlSettings = AccessControlSettings(
    sandbox = SandboxSettings(
      isolates = enableIsolates,
    ),
    hostAccess = HostAccessSettings(
      allowAll = allowAll,
      allowIo = allowIo,
      allowEnv = allowEnv,
    )
  )
}

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
package elide.tool.cli.options

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine.Option
import elide.tooling.project.ElideProject

/**
 * ## Server Options
 *
 * Defines a common suite of options for server-related functionality in the CLI; this includes selection of a server
 * host and port, and configuration of various server features.
 */
@Introspected @ReflectiveAccess class ServerOptions : OptionsMixin<ServerOptions> {
  @JvmRecord data class EffectiveServerOptions(
    val host: String,
    val port: UShort,
    val linkHost: String,
  ) {
    fun hostPair(): Pair<String, UShort> = host to port
  }

  /** Specifies a host to use. */
  @Option(
    names = ["--host"],
    description = ["Hostname to use for a server."],
  )
  var host: String? = null

  /** Specifies a port to use. */
  @Option(
    names = ["--port"],
    description = ["Port to use for a server."],
  )
  var port: Int? = null

  /** Specifies a WSGI app to be served. */
  @Option(
    names = ["--wsgi"],
    description = ["Import spec for a WSGI application to serve."],
  )
  var wsgi: String? = null

  fun effectiveServerOptions(project: ElideProject?): EffectiveServerOptions {
    return EffectiveServerOptions(
      host = host ?: project?.manifest?.dev?.server?.host ?: DEFAULT_SERVER_HOST,
      port = (port ?: project?.manifest?.dev?.server?.port ?:DEFAULT_SERVER_PORT).toUShort(),
      linkHost = when (val explicit = host) {
        null -> DEFAULT_LINK_HOST
        LOCAL_SERVER_HOST, DEFAULT_SERVER_HOST -> DEFAULT_LINK_HOST
        else -> explicit
      },
    )
  }

  companion object {
    const val DEFAULT_SERVER_HOST: String = "0.0.0.0"
    const val LOCAL_SERVER_HOST: String = "127.0.0.1"
    const val DEFAULT_LINK_HOST: String = "localhost"
    const val DEFAULT_SERVER_PORT: Int = 8080
  }
}

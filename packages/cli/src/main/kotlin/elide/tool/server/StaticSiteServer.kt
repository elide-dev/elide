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
package elide.tool.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.routing.*
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import elide.runtime.Logging
import elide.tool.cli.cfg.ElideCLITool.ELIDE_TOOL_VERSION
import elide.tooling.project.manifest.ElidePackageManifest.StaticSite

/**
 * ## Static Site Server
 *
 * Simple server agent which is capable of serving a built Elide static site project; this consists of a directory root
 * where site files and assets are built. URL handling is minimal and based on disk layout.
 */
object StaticSiteServer {
  /**
   * ## Server Agent
   *
   * Control interface for a running static site server.
   */
  interface ServerAgent {
    /**
     * Start the server at the configured host and port.
     *
     * @param wait Whether to block until the server is stopped; defaults to `true`.
     * @return Self.
     */
    fun start(wait: Boolean = true): ServerAgent
  }

  /**
   * ## Static Server Configuration
   *
   * Configuration for running a static site server.
   */
  @JvmRecord data class StaticServerConfig internal constructor (
    val root: Path,
    val site: StaticSite? = null,
    val host: Pair<String, UShort> = "0.0.0.0" to 8080u,
    val devMode: Boolean = false,
    val compression: Boolean = true,
    val prefix: String = site?.prefix ?: "/",
  )

  /**
   * Start the static site server using coroutines.
   *
   * @return Server agent which can be used to control the server.
   */
  fun StaticServerConfig.buildStaticServer(): ServerAgent = host.let { (host, port) ->
    // activate dev mode if requested
    if (devMode) System.setProperty("io.ktor.development", "true")

    embeddedServer(Netty, host = host, port = port.toInt()) {
      // install plugins
      install(Compression)
      install(ContentNegotiation)
      install(ConditionalHeaders)
      install(DefaultHeaders) {
        header(HttpHeaders.Server, "elide/$ELIDE_TOOL_VERSION")
      }
      install(CallLogging) {
        logger = Logging.named("tool:server:static")
      }

      if (!devMode) {
        install(CachingHeaders)
      }

      // setup routing
      routing {
        when {
          // when we are passed a zip file, route to that
          root.isRegularFile() && root.extension == "zip" -> staticZip(prefix, "", root)

          // when passed a directory, route to that
          root.isDirectory() -> staticFiles(prefix, root.toFile()) {
            // enable auto-reload when in dev mode
            if (devMode) modify { path, call ->
              call.response.headers.append(HttpHeaders.ETag, path.name)
            }
          }

          // otherwise we can't use this path
          else -> error("Cannot serve static site from path '$root': must be a directory or zip file")
        }
      }
    }
  }.let { embeddedServer ->
    object: ServerAgent {
      override fun start(wait: Boolean): ServerAgent = apply {
        embeddedServer.start(wait = wait)
      }
    }
  }
}

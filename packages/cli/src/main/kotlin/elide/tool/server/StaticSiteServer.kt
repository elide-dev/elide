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
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.server.websocket.*
import java.io.File
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import elide.runtime.Logging
import elide.tool.cli.cfg.ElideCLITool.ELIDE_TOOL_VERSION
import elide.tool.cli.options.ServerOptions
import elide.tooling.project.ElideProject
import elide.tooling.project.manifest.ElidePackageManifest.StaticSite

// Static server constants.
private const val KTOR_DEV_MODE = "io.ktor.development"
private const val STATIC_SERVER_LOGGER = "tool:server:static"
private const val SERVER_HEADER_TOKEN = "elide/$ELIDE_TOOL_VERSION"
private const val ELIDE_ROUTES_PREFIX = "/_/elide"
private const val STATIC_SERVER_PROTOCOL = "elide-static-server/v1"
private const val CHROME_DEVTOOLS_JSON = "/.well-known/appspecific/com.chrome.devtools.json"

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
    val project: ElideProject? = null,
    val host: Pair<String, UShort> = ServerOptions.DEFAULT_SERVER_HOST to ServerOptions.DEFAULT_SERVER_PORT.toUShort(),
    val devMode: Boolean = false,
    val autoReload: Boolean = devMode,
    val browserIntegration: Boolean = devMode,
    val builtinRoutes: Boolean = true,
    val compression: Boolean = true,
    val prefix: String = site?.prefix ?: "/",
    /** Enable SPA fallback mode - serves index.html for routes that don't match static files. */
    val spaFallback: Boolean = false,
    /** Custom fallback file for SPA mode (default: index.html). */
    val spaFallbackFile: String = "index.html",
  )

  // Build workspace information for a project, or the current server, that can be consumed by Chrome.
  private fun StaticServerConfig.buildChromeDevtoolsJson() = when (project) {
    null -> ChromeDevtoolsWorkspace.buildFor(root)
    else -> ChromeDevtoolsWorkspace.forProject(project)
  }

  /**
   * Start the static site server using coroutines.
   *
   * @return Server agent which can be used to control the server.
   */
  fun StaticServerConfig.buildStaticServer(): ServerAgent = host.let { (host, port) ->
    // activate dev mode if requested
    if (devMode) System.setProperty(KTOR_DEV_MODE, "true")

    embeddedServer(CIO, host = host, port = port.toInt()) {
      // install plugins
      install(SSE)
      install(WebSockets)
      install(Compression)
      install(ContentNegotiation)
      install(ConditionalHeaders)
      install(DefaultHeaders) { header(HttpHeaders.Server, SERVER_HEADER_TOKEN) }
      install(CallLogging) { logger = Logging.named(STATIC_SERVER_LOGGER) }
      if (!devMode) install(CachingHeaders)

      fun StaticContentConfig<*>.configureStaticAutoReload() {
        modify { path, call ->
          val pathName = when (path) {
            is Path -> path.name
            is File -> path.toPath().name
            else -> error("Invalid static content path: $path")
          }
          call.response.headers.append(HttpHeaders.ETag, pathName)
        }
      }

      // setup routing
      routing {
        if (builtinRoutes) {
          get("$ELIDE_ROUTES_PREFIX/health") {
            call.respond(HttpStatusCode.NoContent)
          }
          get("$ELIDE_ROUTES_PREFIX/reload") {
            // not yet implemented
            call.respond(HttpStatusCode.Accepted)
          }
          if (devMode && browserIntegration) {
            sse("$ELIDE_ROUTES_PREFIX/browser-events/sse") {
              // nothing at this time
            }
            webSocket("$ELIDE_ROUTES_PREFIX/browser-events/websocket", STATIC_SERVER_PROTOCOL) {
              // nothing at this time
            }
            get(CHROME_DEVTOOLS_JSON) {
              call.respondText(ContentType.Application.Json, HttpStatusCode.OK) {
                Json.encodeToString(buildChromeDevtoolsJson())
              }
            }
          }
        }
        when {
          // when we are passed a zip file, route to that
          root.isRegularFile() && root.extension == "zip" -> staticZip(prefix, "", root) {
            if (autoReload) configureStaticAutoReload()
          }

          // when passed a directory, route to that
          root.isDirectory() -> staticFiles(prefix, root.toFile()) {
            if (autoReload) configureStaticAutoReload()
          }

          // otherwise we can't use this path
          else -> error("Cannot serve static site from path '$root': must be a directory or zip file")
        }

        // SPA fallback: serve index.html for routes that don't match static files
        // This enables client-side routing for SPAs built with React Router, Vue Router, etc.
        if (spaFallback && root.isDirectory()) {
          get("{...}") {
            val fallbackFile = root.resolve(spaFallbackFile).toFile()
            if (fallbackFile.exists() && fallbackFile.isFile) {
              call.respondFile(fallbackFile)
            } else {
              call.respond(HttpStatusCode.NotFound, "SPA fallback file not found: $spaFallbackFile")
            }
          }
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

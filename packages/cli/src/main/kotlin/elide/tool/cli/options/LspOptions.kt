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
import elide.runtime.core.PolyglotContextBuilder
import elide.tooling.project.ElideConfiguredProject

/**
 * ## LSP Options
 *
 * Specifies options which relate to LSP (Language Server Protocol) support in the Elide CLI.
 */
@Introspected @ReflectiveAccess class LspOptions : OptionsMixin<LspOptions> {
  private companion object {
    private const val DEFAULT_LSP_HOST = "localhost"
    private const val DEFAULT_LSP_PORT = 8124
  }
  /** Debug mode for LSP usage. */
  @Option(
    names = ["--lsp:debug"],
    description = ["Activate debugging for LSP services."],
  )
  private var lspDebug: Boolean = false

  /** Specifies the host to bind to for LSP services. */
  @Option(
    names = ["--lsp:host"],
    description = ["Host to use for the LSP server."],
    paramLabel = "<host>",
  )
  private var lspHost: String? = null

  /** Specifies the port to bind to for LSP services. */
  @Option(
    names = ["--lsp:port"],
    description = ["Port to use for the LSP server."],
    paramLabel = "<port>",
  )
  private var lspPort: Int? = null

  /** Enable internal sources via LSP. */
  @Option(
    names = ["--lsp:internal"],
    description = ["Activate tools for LSP debugging and internals."],
  )
  private var lspInternals: Boolean = false

  /** Enable internal sources via LSP. */
  @Option(
    names = ["--lsp:delegate"],
    description = ["Add a delegate second-order LSP server."],
    paramLabel = "[languageId@][[host:]port]",
    arity = "0..*",
  )
  private var lspDelegates: List<String> = emptyList()

  @JvmRecord private data class LspDelegate(
    val langId: String?,
    val host: String?,
    val port: Int?,
  ) {
    fun asString(): String = buildString {
      if (langId != null) append("$langId@")
      if (host != null) append(host)
      if (port != null) append(":$port")
    }
  }

  internal fun hostAndPort(): Pair<String, Int> {
    return (lspHost ?: DEFAULT_LSP_HOST) to (lspPort ?: DEFAULT_LSP_PORT)
  }

  internal fun lspHostString(): String = hostAndPort().let {
    "${it.first}:${it.second}"
  }

  internal fun apply(builder: PolyglotContextBuilder) {
    if (lspInternals) {
      builder.option("lsp.Internal", "true")
      if (lspDebug)  {
        builder.option("lsp.DeveloperMode", "true")
      }
    }
  }

  private fun resolveAllLanguageDelegates(project: ElideConfiguredProject?): Sequence<LspDelegate> = sequence {
    val allDelegates = buildList {
      addAll(lspDelegates)
      project?.manifest?.dev?.lsp?.delegates?.let { addAll(it) }
    }
    for (delegate in allDelegates) {
      val parts = delegate.split("@", limit = 2)
      val langId = parts.getOrNull(0)
      val hostAndPort = parts.getOrNull(1)?.split(":") ?: emptyList()
      val host = hostAndPort.getOrNull(0)
      val port = hostAndPort.getOrNull(1)?.toIntOrNull()

      yield(LspDelegate(langId, host, port))
    }
  }

  internal fun allLanguageServerDelegates(project: ElideConfiguredProject?): List<Pair<String?, String>> {
    return resolveAllLanguageDelegates(project).map {
      val langId = it.langId
      Pair(langId, it.asString())
    }.toList()
  }
}

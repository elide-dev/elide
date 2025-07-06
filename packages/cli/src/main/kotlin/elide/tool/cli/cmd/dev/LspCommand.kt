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
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import jakarta.inject.Inject
import jakarta.inject.Provider
import kotlinx.coroutines.runBlocking
import kotlin.io.path.name
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotContextBuilder
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState
import elide.tool.cli.options.LspOptions
import elide.tool.project.ProjectManager
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.load

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
internal class LspCommand @Inject constructor(
  private val projectManagerProvider: Provider<ProjectManager>,
) : ProjectAwareSubcommand<ToolState, CommandContext>() {
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

  // Configure a polyglot context for LSP services.
  private fun PolyglotContextBuilder.configureLSP(project: ElideConfiguredProject?) {
    // configure the host string for the lsp server
    option("lsp", lspOptions.lspHostString())

    // apply general options
    lspOptions.apply(this)

    // configure delegated second-order lsp servers
    lspOptions.allLanguageServerDelegates(project).ifEmpty { null }?.let { delegates ->
      option("lsp.Delegates", delegates.joinToString(",") { delegate ->
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
    }
  }

  @Suppress("UNUSED_PARAMETER")
  private suspend fun CommandContext.lspEntry(project: ElideConfiguredProject?, ctxAccessor: () -> PolyglotContext) {
    output {
      if (project != null) {
        val name = project.manifest.name ?: project.root.name
        append("Running LSP for project '$name'")
        ctxAccessor()
      }
    }
  }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val project = projectManagerProvider.get().resolveProject(projectOptions().projectPath())
    if (project == null) {
      if (runnable == null) {
        // nothing to start
        return err("No LSP start entrypoint (via project or files).")
      }
    }
    val configured = project?.load()

    withDeferredContext(emptySet(), shared = false, cfg = { configureLSP(configured) }) { accessor ->
      runBlocking {
        lspEntry(configured, accessor)
      }
    }
    return success()
  }
}

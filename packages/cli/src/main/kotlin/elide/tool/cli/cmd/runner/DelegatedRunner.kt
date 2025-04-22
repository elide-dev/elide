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

package elide.tool.cli.cmd.runner

import java.nio.file.Path
import elide.tool.Argument
import elide.tool.Arguments
import elide.tool.Environment
import elide.tool.MutableArguments
import elide.tool.cli.CommandContext
import elide.tool.exec.which
import elide.tooling.project.ProjectEcosystem

/**
 * # Delegated Runner
 *
 * Describes adapters to other runtimes and calling conventions for scripts or other runnable material, typically from
 * an Elide project.
 */
sealed interface DelegatedRunner {
  /**
   * Name of this delegated script runner.
   */
  val name: String

  /**
   * Calling style for this delegated script runner.
   */
  val style: CallingStyle

  /**
   * Resolved path to the runner.
   */
  val path: Path

  /**
   * Transform arguments for this delegated runner.
   *
   * @param args Arguments to transform.
   * @return Arguments to pass to the runner.
   */
  fun transform(args: Arguments.Suite): MutableArguments = style.transform(args).toMutable()

  /**
   * Transform environment for this delegated runner.
   *
   * @param env Environment to transform.
   * @return Environment to apply for the runner.
   */
  fun transform(env: Environment): Environment = style.transform(env)

  /**
   * Describes how arguments should be passed to the delegated runner.
   */
  sealed interface CallingStyle {
    /**
     * Prefix to apply before the script name or file.
     */
    val runPrefix: String? get() = null

    /**
     * Binary or entrypoint name.
     */
    val binName: String

    /**
     * Transform arguments for this delegated runner.
     *
     * @param args Arguments to transform.
     * @return Arguments to pass to the runner.
     */
    fun transform(args: Arguments.Suite): Arguments.Suite = runPrefix?.let {
      Arguments.empty().toMutable().apply {
        add(Argument.of(it))
        addAll(args.asArgumentSequence().toList())
      }
    } ?: args

    /**
     * Transform environment for this delegated runner.
     *
     * @param env Environment to transform.
     * @return Environment to apply for the runner.
     */
    fun transform(env: Environment): Environment = env

    /**
     * Default: Arguments are passed literally.
     */
    data object Default: CallingStyle {
      override val binName: String get() = error("No bin name for default calling style")
    }

    /**
     * Node: Arguments are transformed as needed, and the script is provided directly.
     */
    data object Node: CallingStyle {
      override val binName: String get() = "npm"
    }

    /**
     * Deno: Arguments are transformed as needed, and the script is prefixed with `run`.
     */
    data object Deno: CallingStyle {
      override val binName: String get() = "deno"
      override val runPrefix: String? get() = "run"
    }

    /**
     * Bun: Arguments are transformed as needed, and the script is provided directly.
     */
    data object Bun: CallingStyle {
      override val binName: String get() = "bun"
    }

    /**
     * Pnpm: Arguments are transformed as needed, and the script is prefixed with `run`.
     */
    data object Pnpm: CallingStyle {
      override val binName: String get() = "pnpm"
      override val runPrefix: String? get() = "run"
    }

    /**
     * Yarn: Arguments are transformed as needed, and the script is prefixed with `run`.
     */
    data object Yarn: CallingStyle {
      override val binName: String get() = "yarn"
      override val runPrefix: String? get() = "run"
    }
  }

  /**
   * Resolved delegated runner.
   *
   * Holds info about a delegated runner, as resolved from the user's PATH.
   *
   * @property name Simple name for the runner
   * @property style Calling style to use when invoking the runner
   * @property path Resolved path to the runner
   */
  @JvmRecord data class ResolvedDelegatedRunner(
    override val name: String,
    override val style: CallingStyle,
    override val path: Path,
  ): DelegatedRunner

  /** Resolvers and factories for delegated runners. */
  companion object {
    /**
     * Resolve a delegated runner adapter ([DelegatedRunner]) for a given [ecosystem]; optionally, a specific [cmd] may
     * be specified as context.
     *
     * @param ecosystem The ecosystem to resolve a runner for.
     * @param cmd An optional command to resolve a runner for.
     * @return Resolved delegated runner, or `null` if no runner could be resolved.
     */
    @Suppress("unused", "UNUSED_PARAMETER")
    @JvmStatic suspend fun CommandContext.delegatedRunner(
      ecosystem: ProjectEcosystem,
      cmd: String? = null,
    ): DelegatedRunner? {
      return when (ecosystem) {
        // node projects have many runner types
        ProjectEcosystem.Node -> listOf(
          CallingStyle.Node,
          CallingStyle.Bun,
          CallingStyle.Deno,
          CallingStyle.Pnpm,
          CallingStyle.Yarn,
        ).map { style ->
          which(Path.of(style.binName))?.let { path ->
            ResolvedDelegatedRunner(name = style.binName, style = style, path = path)
          }
        }.first()

        // no delegates for project ecosystem
        else -> null
      }
    }
  }
}

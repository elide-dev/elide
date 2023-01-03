package elide.tool.cli

import elide.tool.cli.cmd.AbstractSubcommand.OutputController
import elide.tool.cli.cmd.AbstractSubcommand.ToolContext
import org.graalvm.polyglot.Context as VMContext

/**
 * Type structure of a tool output callable, which runs within the sum context of [OutputController] and
 * [ToolContext].
 */
internal typealias OutputCallable = context(ToolState, OutputController) () -> Unit

/**
 * Type structure of a guest VM callable function, which runs in the sum context of the [ToolContext] and the
 * low-level [VMContext].
 */
internal typealias VMCallable<State> = context(ToolContext<State>) (VMContext) -> Unit

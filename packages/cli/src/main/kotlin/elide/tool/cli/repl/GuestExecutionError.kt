package elide.tool.cli.repl

import elide.tool.cli.err.AbstractToolError

/** Thrown when a guest VM script, expression, or statement, errors during execution under the `run` command. */
internal class GuestExecutionError : AbstractToolError() {

}

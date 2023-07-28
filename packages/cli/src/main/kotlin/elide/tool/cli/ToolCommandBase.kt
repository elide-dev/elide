package elide.tool.cli

/**
 * Default tool command implementation; provides a base for all tool commands.
 */
sealed class ToolCommandBase<T> : AbstractToolCommand<T>() where T: CommandContext

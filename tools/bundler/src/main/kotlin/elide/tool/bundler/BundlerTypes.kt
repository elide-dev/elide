package elide.tool.bundler

/**
 * Interface expected for a bundler command, returned from the `invoke` function on a sub-command implementation.
 */
internal typealias BundlerOperation = (suspend AbstractBundlerSubcommand.CommandContext.() -> Unit)

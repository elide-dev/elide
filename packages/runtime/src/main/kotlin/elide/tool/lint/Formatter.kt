package elide.tool.lint

import elide.tool.Tool

/**
 * # Formatter Agent
 *
 * Describes the API provided by a tool implementation which can format source code; linters are often also formatters,
 * so implementations may elect to also implement [Linter].
 */
interface Formatter : Tool {
}

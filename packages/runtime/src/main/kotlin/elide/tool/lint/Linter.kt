package elide.tool.lint

import elide.tool.Tool

/**
 * # Linter Agent
 *
 * Describes the API provided by a tool implementation which can lint source code, and potentially format source code,
 * if the implementation also extends [Formatter].
 */
interface Linter : Tool {

}

package elide.tools.auximage

import elide.core.api.Symbolic

/**
 * # Auxiliary Image Action
 *
 * Enumerates "actions" supported by the `auximage` tool; a sub-command action is a required parameter for running the
 * tool.
 */
enum class AuxImageAction (override val symbol: String, internal val description: String): Symbolic<String> {
  /**
   * ## Build
   *
   * Build an auxiliary image from a suite of source files.
   */
  BUILD("build", "Generate an auxiliary image for a suite of source files"),

  /**
   * ## Check
   *
   * Check the validity of an auxiliary image.
   */
  CHECK("check", "Load an existing image into memory and check its validity");

  companion object: Symbolic.SealedResolver<String, AuxImageAction> {
    override fun resolve(symbol: String): AuxImageAction = when (symbol) {
      "build" -> BUILD
      "check" -> CHECK
      else -> throw IllegalArgumentException("Unknown aux-image action: $symbol")
    }
  }
}

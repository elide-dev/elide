@file:Suppress("WildcardImport")

package elide.docs

import elide.server.Application

/** Application which powers the Elide site and docs. */
object App : Application {
  /** Main entrypoint for the application. */
  @JvmStatic fun main(args: Array<String>) {
    boot(args)
  }
}

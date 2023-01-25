package elide.site.pages.defaults.tooling

import elide.site.abstract.SitePage

/** Describes the tooling page for Elide's Gradle support. */
object Gradle : SitePage(
  name = "gradle",
  label = "Gradle",
  path = "/tools/gradle",
  title = "Gradle Tooling",
  badge = "info" to "beta",
)

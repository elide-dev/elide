package elide.site.pages

import elide.site.abstract.SitePage
import elide.site.pages.tooling.*

/** Describes the Elide tooling page. */
object Tooling : SitePage(
  name = "tooling",
  label = "Tooling",
  path = "/tools",
  title = "Development Tooling",
  children = listOf(
    Gradle,
    Bazel,
  ),
)

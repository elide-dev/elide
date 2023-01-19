package elide.site.pages.tooling

import elide.site.abstract.SitePage
import elide.site.pages.defaults.Tooling
import elide.site.pages.defaults.tooling.Bazel as Defaults

/** Describes the Bazel tooling page on the Elide site. */
object Bazel : SitePage(
  name = Defaults.name,
  label = Defaults.label,
  path = Defaults.path,
  title = Defaults.title,
  parent = Tooling,
)

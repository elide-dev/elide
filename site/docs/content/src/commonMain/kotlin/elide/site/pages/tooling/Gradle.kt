package elide.site.pages.tooling

import elide.site.abstract.SitePage
import elide.site.pages.defaults.Tooling
import elide.site.pages.defaults.tooling.Gradle as Defaults

/** Describes the Gradle tooling page on the Elide site. */
object Gradle : SitePage(
  name = Defaults.name,
  label = Defaults.label,
  path = Defaults.path,
  title = Defaults.title,
  badge = Defaults.badge,
  parent = Tooling,
)

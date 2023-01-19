package elide.site.pages

import elide.site.abstract.SitePage
import elide.site.pages.defaults.Runtime as Defaults

/** Describes the Elide runtime intro top-level page. */
actual object Runtime : SitePage(
  name = Defaults.name,
  label = Defaults.label,
  path = Defaults.path,
  title = Defaults.title,
)

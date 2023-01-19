package elide.site.pages

import elide.site.abstract.SitePage
import elide.site.pages.defaults.GettingStarted as Defaults

/** Describes the Elide Getting Started page. */
actual object GettingStarted : SitePage(
  name = Defaults.name,
  label = Defaults.label,
  path = Defaults.path,
  title = Defaults.title,
)

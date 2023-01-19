package elide.site.pages

import elide.site.abstract.SitePage
import elide.site.pages.library.Packages
import elide.site.pages.defaults.Library as Defaults

/** Describes the Elide library intro top-level page. */
actual object Library : SitePage(
  name = Defaults.name,
  label = Defaults.label,
  path = Defaults.path,
  title = Defaults.title,
  children = listOf(
    Packages,
  ),
)

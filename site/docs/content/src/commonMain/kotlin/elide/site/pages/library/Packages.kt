package elide.site.pages.library

import elide.site.abstract.SitePage
import elide.site.pages.defaults.Library
import elide.site.pages.defaults.library.Packages as Defaults

/** Describes the Elide library packages page. */
object Packages : SitePage(
  name = Defaults.name,
  label = Defaults.label,
  path = Defaults.path,
  title = Defaults.title,
  parent = Library,
)

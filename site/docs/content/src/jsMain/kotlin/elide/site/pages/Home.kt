package elide.site.pages

import elide.site.abstract.SitePage
import elide.site.pages.defaults.Home as DefaultHome

/** Describes the Elide site home page. */
actual object Home : SitePage(
  name = DefaultHome.name,
  label = DefaultHome.label,
  path = DefaultHome.path,
  title = DefaultHome.title,
)
